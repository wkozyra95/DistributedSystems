defmodule Hospital.Doctor do
  use GenServer
  use AMQP

  def start_link(id) do
    GenServer.start_link(__MODULE__, id, [name: :doctor_consumer])
  end

  @exchange    "gen_server_test_exchange"
  @chan_admin   "admin_channel"

  def init(id) do
    chan_name = "doctor." <> id
    IO.puts "SETUP DOCTOR - " <> chan_name
    {:ok, conn} = Connection.open("amqp://guest:guest@localhost")
    {:ok, chan} = Channel.open(conn)
    Basic.qos chan, prefetch_size: 0

    Exchange.topic chan, @exchange, durable: true

    Queue.declare chan, chan_name, auto_delete: true
    Queue.bind chan, chan_name, @exchange, routing_key: chan_name
    Queue.bind chan, chan_name, @exchange, routing_key: @chan_admin
    {:ok, _consumer_tag} = Basic.consume(chan, chan_name)

    {:ok, chan}
  end

  # Confirmation sent by the broker after registering this process as a consumer
  def handle_info({:basic_consume_ok, %{consumer_tag: consumer_tag}}, chan) do
    IO.puts "DOCTOR_REGISTERED - " <> consumer_tag 
    {:noreply, chan}
  end

  # Sent by the broker when the consumer is unexpectedly cancelled (such as after a queue deletion)
  def handle_info({:basic_cancel, %{consumer_tag: consumer_tag}}, chan) do
    IO.puts :basic_cancel
    {:stop, :normal, chan}
  end

  # Confirmation sent by the broker to the consumer process after a Basic.cancel
  def handle_info({:basic_cancel_ok, %{consumer_tag: consumer_tag}}, chan) do
    IO.puts :basic_cancel_ok
    {:noreply, chan}
  end

  def handle_info({:basic_deliver, payload, %{delivery_tag: tag, redelivered: redelivered}}, chan) do
    spawn fn -> consume(chan, tag, redelivered, payload) end
    {:noreply, chan}
  end

  def handle_info({:medical_request, type, message, replay_address}, chan) do
    AMQP.Basic.publish chan, @exchange, type, message, reply_to: replay_address
    {:noreply, chan}
  end

  defp consume(channel, tag, redelivered, payload) do
    IO.puts "received results - " <> payload 
    AMQP.Basic.ack channel, tag
  end
end

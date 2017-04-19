defmodule Hospital.Admin do
  use GenServer
  use AMQP

  def start_link() do
    GenServer.start_link(__MODULE__, [], [name: :admin_consumer])
  end

  @exchange    "gen_server_test_exchange"
  @chan_name   "admin_channel"

  def init _ do
    IO.puts "SETUP ADMIN"
    {:ok, conn} = Connection.open("amqp://guest:guest@localhost")
    {:ok, chan} = Channel.open(conn)
    Basic.qos chan, prefetch_size: 0

    Exchange.topic chan, @exchange, durable: true

    Queue.declare chan, @chan_name, auto_delete: true
    Queue.bind chan, @chan_name, @exchange, routing_key: "technican.#"
    Queue.bind chan, @chan_name, @exchange, routing_key: "doctor.#"
    {:ok, _consumer_tag} = Basic.consume(chan, @chan_name)

    {:ok, chan}
  end

  # Confirmation sent by the broker after registering this process as a consumer
  def handle_info({:basic_consume_ok, %{consumer_tag: consumer_tag}}, chan) do
    IO.puts "ADMIN_REGISTERED - " <> consumer_tag 
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

  def handle_info({:basic_deliver, payload, %{delivery_tag: tag }}, chan) do
    spawn fn -> consume(chan, tag, payload) end
    {:noreply, chan}
  end

  def handle_info({:broadcast, message}, chan) do
    AMQP.Basic.publish chan, @exchange, @chan_name, message
    {:noreply, chan}
  end

  defp consume(channel, tag, payload) do
    IO.puts "logged - " <> payload 
    AMQP.Basic.ack channel, tag
  end
end

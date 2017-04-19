defmodule Hospital.Technican do
  use GenServer
  use AMQP

  def start_link(skill1, skill2, id) do
    GenServer.start_link(__MODULE__, [skill1, skill2, id], [name: :technican_consumer])
  end

  @exchange    "gen_server_test_exchange"

  def init([skill1, skill2, id]) do
    IO.puts "SETUP TECHNICAN." <> id <> " skill(" <> skill1 <> ", " <> skill2 <> ")"
    {:ok, conn} = Connection.open("amqp://guest:guest@localhost")
    {:ok, chan} = Channel.open(conn)
    Basic.qos chan, prefetch_size: 0
    
    Exchange.topic chan, @exchange, durable: true
    
    Queue.declare chan, skill1, auto_delete: true
    Queue.bind chan, skill1, @exchange, routing_key: "technican." <> skill1
    {:ok, _consumer_tag} = Basic.consume(chan, skill1)

    Queue.declare chan, skill2, auto_delete: true
    Queue.bind chan, skill2, @exchange, routing_key: "technican." <> skill2
    {:ok, _consumer_tag} = Basic.consume(chan, skill2)

    tech_chan = "tech." <> id
    Queue.declare chan, tech_chan, auto_delete: true
    Queue.bind chan, tech_chan, @exchange, routing_key: "admin_channel"
    {:ok, _consumer_tag} = Basic.consume(chan, tech_chan)
    
    {:ok, chan}
  end

  # Confirmation sent by the broker after registering this process as a consumer
  def handle_info({:basic_consume_ok, %{consumer_tag: consumer_tag}}, chan) do
    IO.puts "TECHNICAN_SKILL_REGISTERED " <> consumer_tag
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

  def handle_info({:basic_deliver, payload, options}, chan) do
    spawn fn -> consume(chan, payload, options) end
    {:noreply, chan}
  end

  defp consume(channel, payload, %{delivery_tag: tag, reply_to: :undefined, routing_key: routing_key }) do
    IO.inspect "Admin: " <> payload
  end

  defp consume(channel, payload, %{delivery_tag: tag, reply_to: reply_to, routing_key: routing_key }) do
    IO.inspect reply_to
    response = "Message: " <> payload <> " Skill: " <> routing_key
    :timer.sleep(5000)
    AMQP.Basic.publish channel, @exchange, reply_to, response
    Basic.ack channel, tag
  end
end

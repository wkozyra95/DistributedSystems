defmodule Hospital.DoctorUI do
  def start_link(id) do
    {:ok, channel} = GRPC.Stub.connect("localhost:50051")
    Agent.start_link(fn -> %{ channel: channel, id: id } end, name: __MODULE__)
    Task.start_link __MODULE__, :ui_loop, [id]
  end

  def ui_loop(id) do
    IO.gets("#")
    |> String.trim
    |> String.split(" ")
    |> send_request(id)

    ui_loop id
  end

  defp send_request(cmd, id) do
    case cmd do
      ["list", "doctor"] -> spawn fn -> list_doctors() end
      ["list", "technican"] -> spawn fn -> list_technicans() end
      _ -> IO.puts "Invalid cmd"
    end
  end

  def list_doctors() do
    {channel, id} = Agent.get(__MODULE__, fn(x) -> {x.channel, x.id} end)
    {numeric_id, ""} = Integer.parse(id)
    request = Hospital.BasicRequest.new(id: numeric_id)
    results = channel |> Hospital.PublicService.Stub.list_doctors(request)
    IO.inspect results 
  end
  
  def list_technicans() do
    {channel, id} = Agent.get(__MODULE__, fn(x) -> {x.channel, x.id} end)
    {numeric_id, ""} = Integer.parse(id)
    request = Hospital.BasicRequest.new(id: numeric_id)
    results = channel |> Hospital.PublicService.Stub.list_technicans(request)
    IO.inspect results 
  end
end


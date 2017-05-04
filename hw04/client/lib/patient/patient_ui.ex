defmodule Hospital.PatientUI do
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
      ["get", "results", "all"] -> spawn fn -> get_all_results() end
      ["get", "patient"] -> spawn fn -> get_patient_data() end
      ["list", "doctor"] -> spawn fn -> list_doctors() end
      ["list", "technican"] -> spawn fn -> list_technicans() end
      _ -> IO.puts "Invalid cmd"
    end
  end

  def get_all_results() do
    {channel, id} = Agent.get(__MODULE__, fn(x) -> {x.channel, x.id} end)
    {numeric_id, ""} = Integer.parse(id)
    request = Hospital.BasicRequest.new(id: numeric_id)
    stream = channel |> Hospital.PatientService.Stub.request_all_results(request)
    Enum.each(stream, fn(x) -> IO.inspect x end)
  end

  def get_patient_data() do
    {channel, id} = Agent.get(__MODULE__, fn(x) -> {x.channel, x.id} end)
    {numeric_id, ""} = Integer.parse(id)
    request = Hospital.BasicRequest.new(id: numeric_id)
    results = channel |> Hospital.PatientService.Stub.request_personal_data(request)
    IO.inspect results 
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


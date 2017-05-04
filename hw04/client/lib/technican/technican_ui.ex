defmodule Hospital.TechnicanUI do
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
      ["add", "results" | tail] -> spawn fn -> add_results(tail) end
      ["list", "doctor"] -> spawn fn -> list_doctors() end
      ["list", "technican"] -> spawn fn -> list_technicans() end
      _ -> IO.puts "Invalid cmd"
    end
  end

  def add_results([patient, doctor | key_values]) do
    {channel, id} = Agent.get(__MODULE__, fn(x) -> {x.channel, x.id} end)
    {numeric_id, ""} = Integer.parse(id)
    {patient_id, ""} = Integer.parse(patient)
    {doctor_id, ""} = Integer.parse(doctor)
    {results, key} = Enum.reduce(key_values, {[], nil}, fn(x, acc) ->
      IO.inspect acc
      if elem(acc, 1) == nil do
        {elem(acc, 0), x}
      else
        {float, ""} = Float.parse(x)
        {elem(acc, 0) ++ [{elem(acc, 1), float}], nil}
      end
    end)
    
    IO.inspect results
    request = Hospital.AddTestRequest.new([doctor: doctor_id, patient: patient_id, technican: numeric_id, results: results])
    results = channel |> Hospital.TechnicanService.Stub.add_results(request)
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


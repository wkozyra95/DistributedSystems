defmodule Hospital.DoctorUi do

  def start_link(id) do
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
      ["send", type, message] -> send :doctor_consumer, { :medical_request, "technican." <> type, message, "doctor." <> id } 
      _ -> IO.puts "Invalid cmd"
    end
  end
end


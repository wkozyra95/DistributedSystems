defmodule Hospital.AdminUi do

  def start_link() do
    Task.start_link __MODULE__, :ui_loop, []
  end

  def ui_loop() do
    IO.gets("#")
    |> String.trim
    |> send_request

    ui_loop
  end

  defp send_request(cmd) do
    case cmd do
      message -> send :admin_consumer, { :broadcast, message  } 
      _ -> IO.puts "Invalid cmd"
    end
  end
end


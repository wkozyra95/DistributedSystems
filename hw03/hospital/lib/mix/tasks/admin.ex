defmodule Mix.Tasks.Admin do
  use Mix.Task

  def run(args) do
    Hospital.AdminApp.start nil, args
  end
end


defmodule Mix.Tasks.Patient do
  use Mix.Task

  def run(args) do
    Hospital.PatientApp.start nil, args
  end
end



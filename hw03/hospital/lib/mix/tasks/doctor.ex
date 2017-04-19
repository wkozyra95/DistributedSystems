defmodule Mix.Tasks.Doctor do
  use Mix.Task

  def run(args) do
    Hospital.DoctorApp.start :none, args
  end
end

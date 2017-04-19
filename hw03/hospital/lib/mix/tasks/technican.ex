defmodule Mix.Tasks.Technican do
  use Mix.Task

  def run(args) do
    Hospital.TechnicanApp.start :none, args
  end
end


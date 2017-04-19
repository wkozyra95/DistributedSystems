defmodule Hospital.AdminApp do
  # See http://elixir-lang.org/docs/stable/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  def start(_type, _args) do
    import Supervisor.Spec, warn: false

    # Define workers and child supervisors to be supervised
    children = [
      # Starts a worker by calling: Hospital.Worker.start_link(arg1, arg2, arg3)
      # worker(Hospital.Worker, [arg1, arg2, arg3]),
      worker(Hospital.Admin, []),
      worker(Hospital.AdminUi, []),
    ]

    # See http://elixir-lang.org/docs/stable/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Hospital.Supervisor]
    Supervisor.start_link(children, opts)
    :timer.sleep(:infinity)
  end
end

# Generated by `mix grpc.gen.server`.
# Please implement all functions.
defmodule Hospital.PatientService.Server do
  use GRPC.Server, service: Hospital.PatientService.Service

  def request_all_results(basic_request, stream) do
  end

  def request_personal_data(basic_request, _stream) do
  end

end
defmodule Hospital.DoctorService.Server do
  use GRPC.Server, service: Hospital.DoctorService.Service

  def request_all_patients(basic_request, _stream) do
  end

  def request_patients_for_doctor(basic_request, _stream) do
  end

  def request_results_with_id(filter_by_id_request, _stream) do
  end

end
defmodule Hospital.TechnicanService.Server do
  use GRPC.Server, service: Hospital.TechnicanService.Service

  def add_results(add_test_request, _stream) do
  end

  def request_all_results_for_technican(basic_request, _stream) do
  end

end
defmodule Hospital.PublicService.Server do
  use GRPC.Server, service: Hospital.PublicService.Service

  def list_doctors(basic_request, _stream) do
  end

  def list_technicans(basic_request, _stream) do
  end

end

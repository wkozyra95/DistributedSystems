package main

import (
	pb "./hospital"
	"errors"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
	"log"
	"net"
)

const port = ":50051"

// DataStore type
type DataStore struct {
	Patient         map[int64]*pb.Patient
	Doctor          map[int64]*pb.Doctor
	Technican       map[int64]*pb.Technican
	Tests           map[int64]*pb.MedicalTest
	TestCounter     int64
	IdentityCounter int64
}

// NewDataStore constructor
func NewDataStore() *DataStore {
	store := &DataStore{
		Patient:         make(map[int64]*pb.Patient),
		Doctor:          make(map[int64]*pb.Doctor),
		Technican:       make(map[int64]*pb.Technican),
		Tests:           make(map[int64]*pb.MedicalTest),
		TestCounter:     2,
		IdentityCounter: 10,
	}
	patient1 := &pb.Patient{Identity: &pb.Person{Id: 1, FirstName: "first_name1", LastName: "last_name1"}}
	patient2 := &pb.Patient{Identity: &pb.Person{Id: 2, FirstName: "first_name2", LastName: "last_name2"}}
	patient3 := &pb.Patient{Identity: &pb.Person{Id: 3, FirstName: "first_name3", LastName: "last_name3"}}

	doctor1 := &pb.Doctor{Identity: &pb.Person{Id: 4, FirstName: "first_name4", LastName: "last_name4"}}
	doctor2 := &pb.Doctor{Identity: &pb.Person{Id: 5, FirstName: "first_name5", LastName: "last_name5"}}
	doctor3 := &pb.Doctor{Identity: &pb.Person{Id: 6, FirstName: "first_name6", LastName: "last_name6"}}

	technican1 := &pb.Technican{Identity: &pb.Person{Id: 7, FirstName: "first_name7", LastName: "last_name7"}}
	technican2 := &pb.Technican{Identity: &pb.Person{Id: 8, FirstName: "first_name8", LastName: "last_name8"}}
	technican3 := &pb.Technican{Identity: &pb.Person{Id: 9, FirstName: "first_name9", LastName: "last_name9"}}

	test := &pb.MedicalTest{Id: 1, Patient: patient1, Doctor: doctor1, Technican: technican1, Results: map[string]float64{
		"key1": 1.23,
		"key2": 1000,
		"key3": -10,
	}}
	store.Patient[1] = patient1
	store.Patient[2] = patient2
	store.Patient[3] = patient3

	store.Doctor[4] = doctor1
	store.Doctor[5] = doctor2
	store.Doctor[6] = doctor3

	store.Technican[7] = technican1
	store.Technican[8] = technican2
	store.Technican[9] = technican3

	store.Tests[1] = test
	return store
}

func main() {
	listener, err := net.Listen("tcp", port)
	if err != nil {
		log.Print(err.Error())
	}
	server := grpc.NewServer()
	dataStore := NewDataStore()

	pb.RegisterPublicServiceServer(server, &publicServer{dataStore})
	pb.RegisterPatientServiceServer(server, &patientServer{dataStore})
	pb.RegisterDoctorServiceServer(server, &doctorService{dataStore})
	pb.RegisterTechnicanServiceServer(server, &technicanService{dataStore})

	reflection.Register(server)
	server.Serve(listener)
}

type publicServer struct {
	dataStore *DataStore
}

func (s *publicServer) ListDoctors(context context.Context, auth *pb.BasicRequest) (*pb.Doctors, error) {
	list := make([]*pb.Doctor, len(s.dataStore.Doctor))
	i := 0
	for _, val := range s.dataStore.Doctor {
		list[i] = val
		i++
	}
	return &pb.Doctors{Doctors: list}, nil
}

func (s *publicServer) ListTechnicans(context context.Context, auth *pb.BasicRequest) (*pb.Technicans, error) {
	list := make([]*pb.Technican, len(s.dataStore.Technican))
	i := 0
	for _, val := range s.dataStore.Technican {
		list[i] = val
		i++
	}
	return &pb.Technicans{Technicans: list}, nil
}

type patientServer struct {
	dataStore *DataStore
}

func (s *patientServer) RequestAllResults(auth *pb.BasicRequest, stream pb.PatientService_RequestAllResultsServer) error {
	for _, e := range s.dataStore.Tests {
		if e.Patient.Identity.Id == auth.Id {
			err := stream.Send(&pb.MedicalTests{Tests: []*pb.MedicalTest{e}})
			if err != nil {
				log.Print(err.Error())
				return err
			}
		}
	}
	return nil
}

func (s *patientServer) RequestPersonalData(context context.Context, auth *pb.BasicRequest) (*pb.Patient, error) {
	log.Print("RequestPersonalData")
	patientData := s.dataStore.Patient[auth.Id]
	if patientData == nil {
		return nil, errors.New("Unknown patient")
	}
	return patientData, nil
}

type doctorService struct {
	dataStore *DataStore
}

func (s *doctorService) RequestAllPatients(context context.Context, auth *pb.BasicRequest) (*pb.Patients, error) {
	log.Print("RequestPatientsForDoctor")
	list := make([]*pb.Patient, len(s.dataStore.Patient))
	i := 0
	for _, val := range s.dataStore.Patient {
		list[i] = val
		i++
	}
	return &pb.Patients{Patients: list}, nil
}

func (s *doctorService) RequestPatientsForDoctor(context context.Context, auth *pb.BasicRequest) (*pb.Patients, error) {
	log.Print("RequestPatientsForDoctor")
	list := make([]*pb.Patient, 0)
	for _, val := range s.dataStore.Tests {
		if val.Doctor.Identity.Id == auth.Id {
			list = append(list, val.Patient)
		}
	}
	return &pb.Patients{Patients: list}, nil
}

func (s *doctorService) RequestResultsWithId(context context.Context, filterRequest *pb.FilterByIdRequest) (*pb.MedicalTest, error) {
	log.Print("RequestResultsWithId")
	test := s.dataStore.Tests[filterRequest.FilterId]
	if test == nil {
		return nil, errors.New("Unknown test")
	}
	return test, nil
}

type technicanService struct {
	dataStore *DataStore
}

func (s *technicanService) AddResults(context context.Context, newTestResults *pb.AddTestRequest) (*pb.BasicResponse, error) {
	log.Print("AddResults")
	doctor := s.dataStore.Doctor[newTestResults.Doctor]
	technican := s.dataStore.Technican[newTestResults.Technican]
	patient := s.dataStore.Patient[newTestResults.Patient]
	if doctor == nil || technican == nil || patient == nil {
		log.Print("invalid person", doctor, technican, patient)
		return nil, errors.New("invalid person")
	}

	s.dataStore.Tests[s.dataStore.TestCounter] = &pb.MedicalTest{Id: s.dataStore.TestCounter, Doctor: doctor, Patient: patient, Technican: technican, Results: newTestResults.Results}
	log.Print(s.dataStore.Tests[s.dataStore.TestCounter])
	s.dataStore.TestCounter++
	return &pb.BasicResponse{Status: "ok"}, nil
}

func (s *technicanService) RequestAllResultsForTechnican(context context.Context, auth *pb.BasicRequest) (*pb.MedicalTests, error) {
	list := make([]*pb.MedicalTest, 0)
	for _, val := range s.dataStore.Tests {
		if val.Technican.Identity.Id == auth.Id {
			list = append(list, val)
		}
	}
	return &pb.MedicalTests{Tests: list}, nil
}

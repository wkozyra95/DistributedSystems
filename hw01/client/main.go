package main

import (
	"bufio"
	"encoding/gob"
	"log"
	"net"
	"os"
	"syscall"
)

var ASCII = `
   ______                           _                  _____  _____ _____ _____                 _   
  |  ____|                         | |          /\    / ____|/ ____|_   _|_   _|     /\        | |  
  | |__  __  ____ _ _ __ ___  _ __ | | ___     /  \  | (___ | |      | |   | |      /  \   _ __| |_ 
  |  __| \ \/ / _  | '_   _ \| '_ \| |/ _ \   / /\ \  \___ \| |      | |   | |     / /\ \ | '__| __|
  | |____ >  < (_| | | | | | | |_) | |  __/  / ____ \ ____) | |____ _| |_ _| |_   / ____ \| |  | |_ 
  |______/_/\_\__,_|_| |_| |_| .__/|_|\___| /_/    \_\_____/ \_____|_____|_____| /_/    \_\_|   \__|
	                               | |                                                                    
	                               |_|                                                                    
`

type UserMessage struct {
	ID  int
	Msg string
}

func main() {
	connection, dialErr := net.Dial("tcp4", "localhost:12345")
	if dialErr != nil {
		log.Fatal(dialErr.Error())
	}
	defer connection.Close()

	udpAddress, _ := net.ResolveUDPAddr("udp4", connection.LocalAddr().String())
	udpConnection, connError := net.ListenUDP("udp4", udpAddress)
	if connError != nil {
		log.Fatal(connError.Error())
	}
	defer udpConnection.Close()

	udpMulticastAddr, _ := net.ResolveUDPAddr("udp4", "224.0.0.1:7381")
	udpMulticastConn, udpMulticastErr := net.ListenMulticastUDP("udp4", nil, udpMulticastAddr)
	if udpMulticastErr != nil {
		log.Fatal(udpMulticastErr.Error())
	}

	multicastDescriptor, _ := udpMulticastConn.File()
	syscall.SetsockoptInt(int(multicastDescriptor.Fd()), syscall.IPPROTO_IP, syscall.IP_MULTICAST_LOOP, 1)

	listenChannel := make(chan bool)
	go listenForMessage(connection, listenChannel)
	listenUDPChannel := make(chan bool)
	go listenForUDPData(udpConnection, listenUDPChannel)
	listenMulticast := make(chan bool)
	go listenForUDPMulticast(udpMulticastConn, listenMulticast)

	reader := bufio.NewReader(os.Stdin)

	for {
		text, readErr := reader.ReadString('\n')
		if readErr != nil {
			log.Fatal(readErr.Error())
		}
		if text == "M\n" {
			sendUdpUnicast(udpConnection)
			continue
		}
		if text == "N\n" {
			sendMulticast(udpMulticastConn)
			continue
		}

		connection.Write([]byte(text))
		select {
		case <-listenChannel:
			return
		case <-listenUDPChannel:
			return
		default:
		}
	}
}

func listenForMessage(connection net.Conn, channel chan bool) {
	defer func() { channel <- true }()
	userMsg := UserMessage{}
	for {
		decoder := gob.NewDecoder(connection)
		userMsg = UserMessage{}
		err := decoder.Decode(&userMsg)
		if err != nil {
			log.Print(err.Error())
			return
		}
		log.Print(userMsg.ID, " - ", userMsg.Msg)
	}
}

func sendUdpUnicast(connection *net.UDPConn) {
	addr, _ := net.ResolveUDPAddr("udp4", "127.0.0.1:12345")
	_, writeErr := connection.WriteToUDP([]byte(ASCII), addr)
	if writeErr != nil {
		log.Print(writeErr.Error())
	}
}

func listenForUDPData(connection *net.UDPConn, channel chan bool) {
	defer func() { channel <- true }()
	buffer := make([]byte, 1024)
	for {
		size, _, readErr := connection.ReadFromUDP(buffer)
		if readErr != nil {
			log.Print(readErr.Error())
			return
		}
		log.Print(string(buffer[:size]))
	}
}

func listenForUDPMulticast(connection *net.UDPConn, channel chan bool) {
	defer func() { channel <- true }()

	buffer := make([]byte, 1024)
	for {
		size, readErr := connection.Read(buffer)
		if readErr != nil {
			log.Print(readErr.Error())
			return
		}
		log.Print(string(buffer[:size]))
	}
}

func sendMulticast(connection *net.UDPConn) {
	udpMulticastAddr, _ := net.ResolveUDPAddr("udp4", "224.0.0.1:7381")
	_, err := connection.WriteToUDP([]byte(ASCII), udpMulticastAddr)
	if err != nil {
		log.Print(err.Error())
	}
	log.Print("Sent multicast")
}

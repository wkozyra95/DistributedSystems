package main

import (
	"encoding/gob"
	"log"
	"net"
	"sync"
	"time"
)

type UserMessage struct {
	ID  int
	Msg string
}

type ConnectionPool struct {
	Pool  map[net.Conn]int
	Mutex sync.Mutex
}

func (c *ConnectionPool) add(connection net.Conn, id int) {
	c.Mutex.Lock()
	c.Pool[connection] = id
	c.Mutex.Unlock()
}

func (c *ConnectionPool) remove(connection net.Conn) {
	c.Mutex.Lock()
	delete(c.Pool, connection)
	c.Mutex.Unlock()
}

func (c *ConnectionPool) getAll() []net.Conn {
	c.Mutex.Lock()
	keys := make([]net.Conn, len(c.Pool))
	i := 0
	for key := range c.Pool {
		keys[i] = key
		i = i + 1
	}
	c.Mutex.Unlock()
	return keys
}

func main() {
	listener, listenErr := net.ListenTCP("tcp4", &net.TCPAddr{IP: net.ParseIP("127.0.0.1"), Port: 12345})
	if listenErr != nil {
		log.Fatal(listenErr.Error())
	}
	defer listener.Close()

	connectionPool := &ConnectionPool{
		Pool:  make(map[net.Conn]int),
		Mutex: sync.Mutex{},
	}

	listenTCPChannel := make(chan int)
	go listenForConnection(listenTCPChannel, listener, connectionPool)
	listenUDPChannel := make(chan int)
	go listenForUDPData(listenUDPChannel, connectionPool)
	for {
		time.Sleep(1 * time.Second)
		select {
		case <-listenTCPChannel:
			return
		case <-listenUDPChannel:
			return
		default:
		}
	}
}

func listenForConnection(listenChannel chan int, listener *net.TCPListener, connectionPool *ConnectionPool) {
	idCounter := 1
	for {
		connection, acceptErr := listener.Accept()
		if acceptErr != nil {
			log.Println(acceptErr.Error())
			continue
		}
		connectionPool.add(connection, idCounter)
		go tcpConnected(connection, connectionPool, idCounter)
		idCounter++
	}
}

func tcpConnected(connection net.Conn, pool *ConnectionPool, userID int) {
	defer connection.Close()
	defer pool.remove(connection)

	request := make([]byte, 1024)
	for {
		requestSize, readErr := connection.Read(request)
		if readErr != nil {
			log.Print(readErr.Error())
			return
		}
		log.Print("server log - ", string(request[:requestSize]))
		allConections := pool.getAll()

		response := UserMessage{
			ID:  userID,
			Msg: string(request[:requestSize]),
		}
		for _, c := range allConections {
			if c != connection {
				encoder := gob.NewEncoder(c)
				err := encoder.Encode(response)
				if err != nil {
					log.Print(err.Error())
				}
			}
		}
	}
}

func listenForUDPData(channel chan int, connectionPool *ConnectionPool) {
	defer func() { channel <- 0 }()
	localAddress := &net.UDPAddr{IP: net.ParseIP("127.0.0.1"), Port: 12345}
	connection, connError := net.ListenUDP("udp4", localAddress)
	if connError != nil {
		log.Print(connError.Error())
		return
	}
	defer connection.Close()
	buffer := make([]byte, 1024)
	for {
		size, address, readErr := connection.ReadFromUDP(buffer)
		if readErr != nil {
			log.Print(readErr.Error())
			continue
		}
		log.Print("ASCII Art received")
		for _, c := range connectionPool.getAll() {
			cAddress := c.RemoteAddr()
			if cAddress.String() != address.String() {
				cUDPAdrr, _ := net.ResolveUDPAddr("udp4", cAddress.String())
				_, udpConnErr := connection.WriteToUDP(buffer[:size], cUDPAdrr)
				if udpConnErr != nil {
					log.Print(udpConnErr.Error())
					continue
				}
			}
		}
	}
}

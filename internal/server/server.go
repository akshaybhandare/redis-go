package server

import (
	"fmt"
	"log"
	"net"
	"redis-go/internal/protocol"
	"redis-go/internal/store"
	"strconv"
	"strings"
	"time"
)

type Server struct {
	port  int
	store *store.Store
}

func NewServer(port int, st *store.Store) *Server {
	return &Server{
		port:  port,
		store: st,
	}
}

func (s *Server) Start() error {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", s.port))
	if err != nil {
		return fmt.Errorf("failed to start server: %w", err)
	}
	defer listener.Close()

	log.Printf("Redis server listening on port %d", s.port)

	for {
		conn, err := listener.Accept()
		if err != nil {
			log.Printf("Error accepting connection: %v", err)
			continue
		}

		go s.handleConnection(conn)
	}
}

func (s *Server) handleConnection(conn net.Conn) {
	defer conn.Close()

	reader := protocol.NewRESPReader(conn)

	for {
		// Read command array
		args, err := reader.ReadArray()
		if err != nil {
			// Connection closed or error
			return
		}

		if len(args) == 0 {
			protocol.WriteError(conn, "ERR empty command")
			continue
		}

		// Process command
		command := strings.ToUpper(args[0])

		switch command {
		case "PING":
			s.handlePing(conn)
		case "SET":
			s.handleSet(conn, args)
		case "JSON.SET":
			s.handleJsonSet(conn, args)
		case "GET":
			s.handleGet(conn, args)
		case "DEL":
			s.handleDel(conn, args)
		default:
			protocol.WriteError(conn, fmt.Sprintf("ERR unknown command '%s'", command))
		}
	}
}

func (s *Server) handleJsonSet(conn net.Conn, args []string) {
	if len(args) < 3 {
		protocol.WriteError(conn, "ERR wrong number of arguments for 'set' command")
		return
	}

	key := args[1]
	value := args[2]

	// Default TTL is 0 (no expiration, but we'll use a large value)
	ttl := time.Duration(0)

	if len(args) >= 4 {
		ttlSeconds, err := strconv.Atoi(args[3])
		if err != nil {
			protocol.WriteError(conn, "ERR invalid TTL value")
			return
		}
		ttl = time.Duration(ttlSeconds) * time.Second
	} else {
		// If no TTL specified, use a very large value (100 years)
		ttl = time.Duration(120) * time.Second
	}

	s.store.Set(key, value, store.TypeJSON, ttl)
	protocol.WriteSimpleString(conn, "OK")
}

func (s *Server) handlePing(conn net.Conn) {
	protocol.WriteSimpleString(conn, "PONG")
}

func (s *Server) handleSet(conn net.Conn, args []string) {
	if len(args) < 3 {
		protocol.WriteError(conn, "ERR wrong number of arguments for 'set' command")
		return
	}

	key := args[1]
	value := args[2]

	// Default TTL is 0 (no expiration, but we'll use a large value)
	ttl := time.Duration(0)

	if len(args) >= 4 {
		ttlSeconds, err := strconv.Atoi(args[3])
		if err != nil {
			protocol.WriteError(conn, "ERR invalid TTL value")
			return
		}
		ttl = time.Duration(ttlSeconds) * time.Second
	} else {
		// If no TTL specified, use a very large value (100 years)
		ttl = time.Duration(120) * time.Second
	}

	s.store.Set(key, value, store.TypeString, ttl)
	protocol.WriteSimpleString(conn, "OK")
}

func (s *Server) handleGet(conn net.Conn, args []string) {
	if len(args) != 2 {
		protocol.WriteError(conn, "ERR wrong number of arguments for 'get' command")
		return
	}

	key := args[1]
	value, ok := s.store.Get(key)

	if !ok {
		protocol.WriteNull(conn)
		return
	}

	protocol.WriteBulkString(conn, value)
}

func (s *Server) handleDel(conn net.Conn, args []string) {
	if len(args) != 2 {
		protocol.WriteError(conn, "ERR wrong number of arguments for 'del' command")
		return
	}

	key := args[1]
	s.store.Delete(key)
	protocol.WriteSimpleString(conn, "OK")
}

package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"os/exec"
	"redis-go/internal/persistence"
	"redis-go/internal/server"
	"redis-go/internal/store"
	"redis-go/internal/ttl"
	"strings"
	"time"
)

func main() {
	// Define flags
	isServer := flag.Bool("server", false, "Run as server")
	host := flag.String("host", "localhost", "Server host (client mode)")
	port := flag.Int("port", 1122, "Port to use")
	flag.Parse()

	if *isServer {
		runServer(*port)
	} else {
		runClient(*host, *port)
	}
}

func startServerInBackground(port int) error {
	// Get the current executable path
	executable, err := os.Executable()
	if err != nil {
		return fmt.Errorf("failed to get executable path: %w", err)
	}

	// Start server as background process using exec.Command
	cmd := exec.Command(executable, "--server", "--port", fmt.Sprintf("%d", port))

	// Detach from parent process
	cmd.Stdout = nil
	cmd.Stderr = nil
	cmd.Stdin = nil

	if err := cmd.Start(); err != nil {
		return fmt.Errorf("failed to start server: %w", err)
	}

	// Don't wait for the process to finish
	go cmd.Wait()

	return nil
}

func runServer(port int) {
	fmt.Println("Starting Redis Server...")

	// Initialize AOF persistence
	aof, err := persistence.NewAOF("./aof.log")
	if err != nil {
		log.Fatalf("Error creating AOF file: %v", err)
	}
	defer aof.CloseFile()

	// Initialize store
	kv := store.NewStore()
	kv.WithPersistence(aof)

	// Start TTL cleaner
	cleaner := ttl.NewCleaner(5*time.Second, kv)
	cleaner.Start()

	// Create and start TCP server
	srv := server.NewServer(port, kv)
	if err := srv.Start(); err != nil {
		log.Fatalf("Server error: %v", err)
	}
}

func runClient(host string, port int) {
	// Try to connect to server
	conn, err := net.Dial("tcp", fmt.Sprintf("%s:%d", host, port))
	if err != nil {
		// Server not running, start it in background
		fmt.Printf("No server running on %s:%d, starting server in background...\n", host, port)

		if err := startServerInBackground(port); err != nil {
			fmt.Printf("Error starting server: %v\n", err)
			os.Exit(1)
		}

		// Wait a moment for server to start
		time.Sleep(500 * time.Millisecond)

		// Retry connection
		conn, err = net.Dial("tcp", fmt.Sprintf("%s:%d", host, port))
		if err != nil {
			fmt.Printf("Error connecting to server: %v\n", err)
			os.Exit(1)
		}
	}
	defer conn.Close()

	fmt.Printf("Connected to %s:%d\n", host, port)
	fmt.Println("Type 'exit' or 'quit' to close the connection")
	fmt.Println()

	reader := bufio.NewReader(os.Stdin)
	respReader := bufio.NewReader(conn)

	for {
		// Print prompt
		fmt.Printf("%s:%d> ", host, port)

		// Read user input
		input, err := reader.ReadString('\n')
		if err != nil {
			fmt.Printf("Error reading input: %v\n", err)
			break
		}

		// Trim whitespace
		input = strings.TrimSpace(input)

		// Check for exit commands
		if input == "exit" || input == "quit" {
			fmt.Println("Goodbye!")
			break
		}

		// Skip empty input
		if input == "" {
			continue
		}

		// Parse command into arguments
		args := strings.Fields(input)

		// Send RESP command
		if err := sendCommand(conn, args); err != nil {
			fmt.Printf("Error sending command: %v\n", err)
			continue
		}

		// Read and display response
		if err := readResponse(respReader); err != nil {
			fmt.Printf("Error reading response: %v\n", err)
			continue
		}
	}
}

func sendCommand(conn net.Conn, args []string) error {
	// Write array header
	if _, err := fmt.Fprintf(conn, "*%d\r\n", len(args)); err != nil {
		return err
	}

	// Write each argument as bulk string
	for _, arg := range args {
		if _, err := fmt.Fprintf(conn, "$%d\r\n%s\r\n", len(arg), arg); err != nil {
			return err
		}
	}

	return nil
}

func readResponse(reader *bufio.Reader) error {
	response, err := reader.ReadString('\n')
	if err != nil {
		return err
	}

	if len(response) == 0 {
		fmt.Println("(empty response)")
		return nil
	}

	switch response[0] {
	case '+': // Simple string
		fmt.Printf("%s", response[1:])
	case '-': // Error
		fmt.Printf("(error) %s", response[1:])
	case ':': // Integer
		fmt.Printf("(integer) %s", response[1:])
	case '$': // Bulk string
		var length int
		fmt.Sscanf(response[1:], "%d", &length)

		if length == -1 {
			fmt.Println("(nil)")
		} else {
			// Read the actual data
			data := make([]byte, length+2) // +2 for \r\n
			_, err := reader.Read(data)
			if err != nil {
				return fmt.Errorf("error reading bulk string: %w", err)
			}
			fmt.Printf("\"%s\"\n", data[:length])
		}
	case '*': // Array
		var count int
		fmt.Sscanf(response[1:], "%d", &count)
		fmt.Printf("(array of %d elements)\n", count)
	default:
		fmt.Printf("(unknown type: %c) %s", response[0], response[1:])
	}

	return nil
}

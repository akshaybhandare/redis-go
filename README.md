# Redis-Go

A lightweight, feature-rich Redis implementation written in Go with a unified binary architecture. This project provides both a Redis-compatible server and an interactive CLI client in a single executable.

## Features

### Core Functionality

- **Unified Binary**: Single executable that functions as both server and client
- **RESP Protocol**: Full Redis Serialization Protocol (RESP) support
- **Key-Value Store**: Thread-safe in-memory data store with mutex-based concurrency control
- **TTL Support**: Automatic key expiration with configurable time-to-live
- **AOF Persistence**: Append-Only File logging for data durability
- **Auto-Start Server**: Client automatically starts server in background if not running

### Supported Commands

- `PING` - Test server connectivity
- `SET key value [ttl]` - Set a key-value pair with optional TTL (in seconds)
- `GET key` - Retrieve value by key
- `DEL key` - Delete a key

## Installation

### Prerequisites

- Go 1.22.5 or higher

### Build from Source

```bash
# Clone the repository
git clone <repository-url>
cd redis-go

# Build the binary
go build -o redis-go

# Optional: Install globally
go install
```

## Usage

### Running as Server

Start the Redis server on the default port (1122):

```bash
./redis-go CGO_ENABLED=0 --server
```

Start the server on a custom port:

```bash
./redis-go --server --port 8080
```

### Running as Client

Connect to a server (starts server automatically if not running):

```bash
./redis-go
```

Connect to a specific host and port:

```bash
./redis-go --host localhost --port 8080
```


### Interactive CLI

Once connected, you can use Redis commands interactively:

```
localhost:1122> PING
PONG
localhost:1122> SET mykey "Hello, Redis!" 60
OK
localhost:1122> GET mykey
"Hello, Redis!"
localhost:1122> DEL mykey
OK
localhost:1122> exit
Goodbye!
```

## Architecture

### Project Structure

```
redis-go/
├── main.go                    # Entry point with unified binary logic
├── internal/
│   ├── server/               # TCP server and command handlers
│   │   └── server.go
│   ├── store/                # In-memory key-value store
│   │   └── store.go
│   ├── protocol/             # RESP protocol implementation
│   │   └── resp.go
│   ├── persistence/          # AOF persistence layer
│   │   └── aof.go
│   └── ttl/                  # TTL cleanup mechanism
│       └── cleaner.go
└── java-client/              # Java client example
```

### Components

#### Server (`internal/server`)

- TCP server listening on configurable port
- Connection handling with goroutines for concurrency
- Command parsing and routing
- RESP protocol response formatting

#### Store (`internal/store`)

- Thread-safe map-based key-value storage
- TTL-aware get operations
- Automatic expiration tracking
- Integration with AOF persistence

#### Protocol (`internal/protocol`)

- RESP protocol reader and writer
- Support for all RESP data types (simple strings, errors, integers, bulk strings, arrays)
- Binary-safe string handling

#### Persistence (`internal/persistence`)

- Append-Only File (AOF) logging
- Thread-safe write operations
- Command replay capability for data recovery

#### TTL Cleaner (`internal/ttl`)

- Background goroutine for expired key cleanup
- Configurable cleanup interval (default: 5 seconds)
- Prevents memory leaks from expired keys

## Configuration

### Command-Line Flags

| Flag       | Type   | Default   | Description               |
| ---------- | ------ | --------- | ------------------------- |
| `--server` | bool   | false     | Run as server mode        |
| `--host`   | string | localhost | Server host (client mode) |
| `--port`   | int    | 1122      | Port to use               |

### Persistence

- **AOF File**: `./aof.log` (created in current directory)
- **Format**: Plain text with command replay format
- **Durability**: Synchronous writes for data safety

### TTL Defaults

- **Default TTL**: 120 seconds (when not specified)
- **Cleanup Interval**: 5 seconds
- **Expiration Check**: On every GET operation

## Development

### Running Tests

```bash
go test ./...
```

### Building for Production

```bash
# Build optimized binary
go build -ldflags="-s -w" -o redis-go

# Cross-compile for different platforms
GOOS=linux GOARCH=amd64 go build -o redis-go-linux
GOOS=darwin GOARCH=arm64 go build -o redis-go-macos
GOOS=windows GOARCH=amd64 go build -o redis-go.exe
```

### Code Organization

The project follows Go best practices:

- **Internal packages**: Prevents external imports
- **Separation of concerns**: Each component has a single responsibility
- **Concurrency safety**: Mutex-based synchronization where needed
- **Clean interfaces**: Minimal coupling between components

## Examples

### Basic Operations

```bash
# Start server in background
./redis-go --server &

# Connect with client
./redis-go

# Set a key with 30-second TTL
localhost:1122> SET session:user123 "active" 30

# Get the value
localhost:1122> GET session:user123
"active"

# Wait 30+ seconds and try again
localhost:1122> GET session:user123
(nil)
```

### Programmatic Access

See the `java-client/` directory for an example Java client implementation that connects to the Redis-Go server.

## Limitations

### Current Limitations

- **In-memory only**: No disk-based storage (except AOF log)
- **Single-threaded writes**: AOF writes are serialized
- **Limited commands**: Only basic GET/SET/DEL/PING operations
- **No clustering**: Single-node deployment only
- **No authentication**: No password protection
- **Fixed AOF location**: AOF file path is hardcoded

### Future Enhancements

- [ ] More Redis commands (INCR, DECR, LPUSH, RPUSH, etc.)
- [ ] Pub/Sub support
- [ ] Snapshot (RDB) persistence
- [ ] Authentication and ACLs
- [ ] Configurable AOF path
- [ ] Master-replica replication
- [ ] Cluster mode
- [ ] Lua scripting support

## Performance

### Benchmarks

_(Add your benchmark results here)_

### Optimization Tips

- Use appropriate TTL values to prevent memory bloat
- Monitor AOF file size and implement rotation if needed
- Adjust cleanup interval based on workload

## Troubleshooting

### Server won't start

- Check if port is already in use: `lsof -i :1122`
- Ensure you have write permissions for AOF file

### Client can't connect

- Verify server is running: `ps aux | grep redis-go`
- Check firewall settings
- Confirm correct host and port

### Data loss after restart

- Ensure AOF file exists and is readable
- Check for AOF write errors in server logs
- Verify disk space availability

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

_(Add your license information here)_

## Acknowledgments

Built with inspiration from the Redis project and Go's excellent standard library.

CGO_ENABLED=0 go build -o redis-go
./redis-go --server --port 8899

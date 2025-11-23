package persistence

import (
	"fmt"
	"os"
	"sync"
	"time"
)

type AOF struct {
	file *os.File
	path string
	mu   sync.Mutex
}

func NewAOF(path string) (*AOF, error) {
	f, er := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)

	if er != nil {
		return nil, er
	}

	return &AOF{
		file: f,
		path: path,
	}, nil
}

func (a *AOF) Write(command, key string, val any, datatype string, ttl time.Duration) error {
	a.mu.Lock()
	defer a.mu.Unlock()

	// Check if file still exists, if not recreate it
	if _, err := os.Stat(a.path); os.IsNotExist(err) {
		// File was deleted, close old handle if still open
		if a.file != nil {
			a.file.Close()
		}

		// Recreate the file
		f, er := os.OpenFile(a.path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
		if er != nil {
			return fmt.Errorf("failed to recreate AOF file: %w", er)
		}
		a.file = f
	}

	_, err := fmt.Fprintf(a.file, "%s %s %s %s %d\n", command, key, val, datatype, ttl)

	return err
}

func (a *AOF) CloseFile() error {
	return a.file.Close()
}

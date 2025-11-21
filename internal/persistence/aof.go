package persistence

import (
	"fmt"
	"os"
	"sync"
	"time"
)

type AOF struct {
	file *os.File
	mu   sync.Mutex
}

func NewAOF(path string) (*AOF, error) {
	f, er := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)

	if er != nil {
		return nil, er
	}

	return &AOF{
		file: f,
	}, nil
}

func (a *AOF) Write(command, key, val string, ttl time.Duration) error {
	a.mu.Lock()
	defer a.mu.Unlock()

	_, err := fmt.Fprintf(a.file, "%s %s %s %d\n", command, key, val, ttl)

	return err
}

func (a *AOF) CloseFile() error {
	return a.file.Close()
}

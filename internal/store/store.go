package store

import (
	"redis-go/internal/persistence"
	"sync"
	"time"
)

const (
	TypeString = "string"
	TypeJSON   = "json"
)

type Item struct {
	Value    any
	ExpireAt int64
	Type     string
}

type Store struct {
	data map[string]Item
	mu   sync.RWMutex
	aof  *persistence.AOF
}

func (s *Store) WithPersistence(aof *persistence.AOF) {
	s.aof = aof
}

func NewStore() *Store {
	return &Store{
		data: make(map[string]Item),
	}
}

func (s *Store) Set(key string, value any, datatype string, ttl time.Duration) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.aof != nil {
		s.aof.Write("SET", key, value, datatype, ttl)
	}

	expires := time.Now().Add(ttl).Unix()

	s.data[key] = Item{
		Value:    value,
		ExpireAt: expires,
		Type:     datatype,
	}
}

func (s *Store) Get(key string) (string, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	item, exists := s.data[key]

	if !exists {
		return "", false
	}

	if time.Now().Unix() > item.ExpireAt {
		return "", false
	}

	if item.Type == TypeJSON {
		return item.Value.(string), true
	}

	return item.Value.(string), true
}

func (s *Store) Delete(key string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.aof != nil {
		s.aof.Write("DEL", key, "","", 0)
	}

	delete(s.data, key)

	return true
}

func (s *Store) DeleteExpired() {
	s.mu.Lock()
	defer s.mu.Unlock()

	now := time.Now().Unix()

	for key, item := range s.data {
		if now > item.ExpireAt {
			delete(s.data, key)
		}
	}
}

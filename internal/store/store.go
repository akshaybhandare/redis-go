package store

import (
	"redis-go/internal/persistence"
	"sync"
	"time"
)

type Item struct {
	Value    string
	ExpireAt int64
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

func (s *Store) Set(key, value string, ttl time.Duration) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.aof != nil {
		s.aof.Write("SET", key, value, ttl)
	}

	expires := time.Now().Add(ttl).Unix()

	s.data[key] = Item{
		Value:    value,
		ExpireAt: expires,
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

	return item.Value, true
}

func (s *Store) Delete(key string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if s.aof != nil {
		s.aof.Write("DEL", key, "", 0)
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

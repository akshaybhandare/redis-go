package store

import (
	"encoding/json"
	"time"
)

func (s *Store) JsonSet(key string, value string, ttl time.Duration) error {
	var data interface{}

	d := json.Unmarshal([]byte(value),&data)

	if d != nil {
		return d
	}

	s.Set(key, data, TypeJSON, ttl)

	return nil	
}
package ttl

import (
	"time"
)

type Expirable interface {
	DeleteExpired()
}

type Cleaner struct {
	interval time.Duration
	target   Expirable
}

func NewCleaner(interval time.Duration, tar Expirable) *Cleaner {
	return &Cleaner{
		interval: interval,
		target:   tar,
	}
}

func (c *Cleaner) Start() {
	ticker := time.NewTicker(c.interval)

	go func() {
		for range ticker.C {
			c.target.DeleteExpired()
		}
	}()

}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Alex Edwards
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* Copy of https://github.com/alexedwards/scs/tree/master/memstore with the additional of Listener */

package session

import (
	"errors"
	"sync"
	"time"
)

var errTypeAssertionFailed = errors.New("type assertion failed: could not convert interface{} to []byte")

type item struct {
	object     interface{}
	expiration int64
}

// MemStore represents the session store.
type MemStore struct {
	items       map[string]item
	mu          sync.RWMutex
	stopCleanup chan bool
	listener    Listener
}

// New returns a new MemStore instance, with a background cleanup goroutine that
// runs every minute to remove expired session data.
func New() *MemStore {
	return NewWithCleanupInterval(time.Minute)
}

// NewWithCleanupInterval returns a new MemStore instance. The cleanupInterval
// parameter controls how frequently expired session data is removed by the
// background cleanup goroutine. Setting it to 0 prevents the cleanup goroutine
// from running (i.e. expired sessions will not be removed).
func NewWithCleanupInterval(cleanupInterval time.Duration) *MemStore {
	m := &MemStore{
		items:    make(map[string]item),
		listener: &noopListener{},
	}

	if cleanupInterval > 0 {
		go m.startCleanup(cleanupInterval)
	}

	return m
}

// Find returns the data for a given session token from the MemStore instance.
// If the session token is not found or is expired, the returned exists flag will
// be set to false.
func (m *MemStore) Find(token string) ([]byte, bool, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	item, found := m.items[token]
	if !found {
		return nil, false, nil
	}
	if time.Now().UnixNano() > item.expiration {
		return nil, false, nil
	}

	b, ok := item.object.([]byte)
	if !ok {
		return nil, true, errTypeAssertionFailed
	}

	return b, true, nil
}

// Commit adds a session token and data to the MemStore instance with the given
// expiry time. If the session token already exists, then the data and expiry
// time are updated.
func (m *MemStore) Commit(token string, b []byte, expiry time.Time) error {
	m.mu.Lock()
	_, present := m.items[token]
	m.items[token] = item{
		object:     b,
		expiration: expiry.UnixNano(),
	}
	if !present {
		m.listener.Added(token)
	}
	m.mu.Unlock()

	return nil
}

// Delete removes a session token and corresponding data from the MemStore
// instance.
func (m *MemStore) Delete(token string) error {
	m.mu.Lock()
	_, present := m.items[token]
	delete(m.items, token)
	if present {
		m.listener.Removed(token)
	}
	m.mu.Unlock()

	return nil
}

func (m *MemStore) startCleanup(interval time.Duration) {
	m.stopCleanup = make(chan bool)
	ticker := time.NewTicker(interval)
	for {
		select {
		case <-ticker.C:
			m.deleteExpired()
		case <-m.stopCleanup:
			ticker.Stop()
			return
		}
	}
}

// StopCleanup terminates the background cleanup goroutine for the MemStore
// instance. It's rare to terminate this; generally MemStore instances and
// their cleanup goroutines are intended to be long-lived and run for the lifetime
// of your application.
//
// There may be occasions though when your use of the MemStore is transient.
// An example is creating a new MemStore instance in a test function. In this
// scenario, the cleanup goroutine (which will run forever) will prevent the
// MemStore object from being garbage collected even after the test function
// has finished. You can prevent this by manually calling StopCleanup.
func (m *MemStore) StopCleanup() {
	if m.stopCleanup != nil {
		m.stopCleanup <- true
	}
}

func (m *MemStore) deleteExpired() {
	now := time.Now().UnixNano()
	m.mu.Lock()
	for token, item := range m.items {
		if now > item.expiration {
			delete(m.items, token)
			m.listener.Removed(token)
		}
	}
	m.mu.Unlock()
}

func (m *MemStore) RegisterListener(listener Listener) {
	if listener == nil {
		m.listener = &noopListener{}
	} else {
		m.listener = listener
	}
}

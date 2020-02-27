/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"sync"
	"time"
)

func Schedule(f func(), delay time.Duration) (doCancel func()) {
	cancel := make(chan bool)
	closeOnce := sync.Once{}
	doCancel = func() {
		closeOnce.Do(func() {
			close(cancel)
		})
	}

	go func() {
		defer doCancel()
		for {
			f()
			select {
			case <-time.After(delay):
			case <-cancel:
				return
			}
		}
	}()
	return
}

func RunAfter(delay time.Duration, f func()) (doCancel func()) {
	cancel := make(chan bool)
	closeOnce := sync.Once{}
	doCancel = func() {
		closeOnce.Do(func() {
			close(cancel)
		})
	}

	go func() {
		defer doCancel()
		select {
		case <-time.After(delay):
			f()
		case <-cancel:
			return
		}
	}()
	return
}

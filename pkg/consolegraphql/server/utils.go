/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import "time"

func Schedule(f func(), delay time.Duration) chan<- bool {
	cancel := make(chan bool)
	defer close(cancel)

	go func() {
		for {
			f()
			select {
			case <-time.After(delay):
			case <-cancel:
				return
			}
		}
	}()

	return cancel
}

func RunAfter(delay time.Duration, f func()) chan<- bool {
	cancel := make(chan bool)
	defer close(cancel)

	go func() {
		select {
		case <-time.After(delay):
			f()
		case <-cancel:
			return
		}
	}()

	return cancel
}

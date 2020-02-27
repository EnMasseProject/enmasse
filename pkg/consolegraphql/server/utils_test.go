/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"github.com/stretchr/testify/assert"
	"sync/atomic"
	"testing"
	"time"
)

func TestRunOnce(t *testing.T) {
	var count int32

	cancel := RunAfter(time.Millisecond, func() {
		atomic.AddInt32(&count, 1)
	})

	assert.Eventually(t, func() bool { return atomic.LoadInt32(&count) == 1 }, time.Second*10, time.Millisecond*100)
	cancel()
}

func TestRunOnceCancelled(t *testing.T) {
	var count int32
	cancel := RunAfter(time.Hour, func() {
		atomic.AddInt32(&count, 1)
	})
	cancel()
	assert.Equal(t, int32(0), atomic.LoadInt32(&count))
}

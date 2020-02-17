/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"math/rand"
	"time"
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

var (
	brokerNameLetters = []rune("abcdefghijklmnopqrstuvwxyz0123456789")
	brokerNameLength  = 8
)

func RandomBrokerName() string {
	b := make([]rune, brokerNameLength)
	for i := range b {
		b[i] = brokerNameLetters[rand.Intn(len(brokerNameLetters))]
	}
	return string(b)
}

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"github.com/alexedwards/scs/v2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server/session"
	"github.com/prometheus/client_golang/prometheus"
	"sync/atomic"
	"time"
)

type sessionCountingListener struct {
	sessionCount      int32
	sessionPresent    func()
	noSessionsPresent func()
	metric            prometheus.Gauge
}

func (l *sessionCountingListener) Added(string) {
	curr := atomic.AddInt32(&l.sessionCount, 1)
	l.sessionPresent()
	l.metric.Set(float64(curr))
}

func (l *sessionCountingListener) Removed(string) {
	curr := atomic.AddInt32(&l.sessionCount, -1)
	if curr == 0 {
		l.noSessionsPresent()
	}
	l.metric.Set(float64(curr))
}

func CreateSessionManager(sessionLifetime time.Duration, sessionIdleTimeout time.Duration, sessionPresentCallback func(), noSessionsPresentCallback func(), metric prometheus.Gauge) *scs.SessionManager {
	store := session.New()
	store.RegisterListener(&sessionCountingListener{
		sessionPresent:    sessionPresentCallback,
		noSessionsPresent: noSessionsPresentCallback,
		metric:            metric,
	})

	sessionManager := scs.New()

	sessionManager.Lifetime = sessionLifetime
	sessionManager.IdleTimeout = sessionIdleTimeout
	sessionManager.Cookie.HttpOnly = true
	sessionManager.Cookie.Persist = false
	sessionManager.Cookie.Secure = true
	sessionManager.Store = store
	return sessionManager
}

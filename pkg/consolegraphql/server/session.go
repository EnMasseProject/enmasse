/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"github.com/alexedwards/scs/v2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server/session"
	"sync/atomic"
	"time"
)

type sessionCountingListener struct {
	sessionCount      int32
	sessionPresent    func()
	noSessionsPresent func()
}

func (l *sessionCountingListener) Added(string) {
	if atomic.AddInt32(&l.sessionCount, 1) == 1 {
		l.sessionPresent()
	}
}

func (l *sessionCountingListener) Removed(string) {
	if atomic.AddInt32(&l.sessionCount, -1) == 0 {
		l.noSessionsPresent()
	}
}

func CreateSessionManager(sessionLifetime time.Duration, sessionIdleTimeout time.Duration, sessionPresentCallback func(), noSessionsPresentCallback func()) *scs.SessionManager {
	store := session.New()
	store.RegisterListener(&sessionCountingListener{
		sessionPresent:    sessionPresentCallback,
		noSessionsPresent: noSessionsPresentCallback,
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

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package session

type Listener interface {
	Added(token string)
	Removed(token string)
}

type noopListener struct {
}

func (n *noopListener) Added(string) {
}

func (n *noopListener) Removed(string) {
}

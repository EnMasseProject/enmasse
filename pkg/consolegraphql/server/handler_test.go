/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"github.com/stretchr/testify/assert"
	"net/http"
	"testing"
)

func TestAccessTokenFromXForwarded(t *testing.T) {
	req := &http.Request{
		Header: map[string][]string{
			forwardedHeader: {"tok"},
		},
	}

	assert.Equal(t, "tok", getAccessToken(req))
}

func TestAccessTokenFromAuthBearerFormatted(t *testing.T) {
	req := &http.Request{
		Header: map[string][]string{
			authHeader: {"Bearer tok"},
		},
	}

	assert.Equal(t, "tok", getAccessToken(req))
}

func TestAccessTokenFromAuthUnexpectedFormat(t *testing.T) {
	req := &http.Request{
		Header: map[string][]string{
			authHeader: {"tok"},
		},
	}

	assert.Equal(t, "tok", getAccessToken(req))
}

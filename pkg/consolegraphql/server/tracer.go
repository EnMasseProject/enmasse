/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import (
	"log"
	"net/http"
	"net/http/httputil"
)

type Tracer struct {
	http.RoundTripper
}

// http://people.redhat.com/jrivera/openshift-docs_preview/openshift-origin/glusterfs-review/go_client/tracing_api_requests_and_responses.html
// RoundTrip calls the nested RoundTripper while printing each request and
// response/error to os.Stderr on either side of the nested call.  WARNING: this
// may output sensitive information including bearer tokens.
func (t *Tracer) RoundTrip(req *http.Request) (*http.Response, error) {
	// Dump the request to os.Stderr.
	b, err := httputil.DumpRequestOut(req, true)
	if err != nil {
		return nil, err
	}

	log.Printf("TRACE: -> %s\n", string(b))

	// Call the nested RoundTripper.
	resp, err := t.RoundTripper.RoundTrip(req)

	// If an error was returned, dump it to os.Stderr.
	if err != nil {
		return resp, err
	}

	// Dump the response to os.Stderr.
	b, err = httputil.DumpResponse(resp, req.URL.Query().Get("watch") != "true")
	if err != nil {
		return nil, err
	}
	log.Printf("TRACE: <- %s\n", string(b))

	return resp, err
}

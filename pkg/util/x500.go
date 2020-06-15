/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"crypto/x509/pkix"
	"encoding/asn1"
	"fmt"
)

// Convert a name from an X509 certificate into a string
//
// We cannot use the Go method pkix.Name.ToString() function as
// the whole name decoding logic in Go is full of assumptions which
// are not correct. Go takes the RDNSequence and re-arranges and
// re-encodes the entries in a way that they no longer correspond to
// the original sequence. Thus we simply take the raw value, and encode
// it ourselves as string.
//
// Also see: https://github.com/golang/go/issues/27401
func ToX500Name(asn1Name []byte) (string, error) {

	// prepare the sequence

	var name pkix.RDNSequence

	// unmarshal from ASN.1

	rest, err := asn1.Unmarshal(asn1Name, &name)
	if err != nil {
		return "", err
	} else if len(rest) > 0 {
		return "", fmt.Errorf("remaining bytes after decoding name (len: %d)", len(rest))
	}

	// return the plain sequence as string

	return name.String(), nil

}

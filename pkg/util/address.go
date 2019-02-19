/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"regexp"

	"github.com/google/uuid"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var (
	addressNameExpression  *regexp.Regexp = regexp.MustCompile("^[a-zA-Z]+$")
	replaceExpression      *regexp.Regexp = regexp.MustCompile("[^a-zA-Z]")
	replaceStartExpression *regexp.Regexp = regexp.MustCompile("^[^a-zA-Z]+")
	operatorUuidNamespace  uuid.UUID      = uuid.MustParse("1516b246-23aa-11e9-b615-c85b762e5a2c")
)

// Get an address name from an IoTProject
// the name is the prefix (e.g. "telemetry") plus the tenant name. The name is not encoded in any way.
func AddressName(object metav1.Object, prefix string) string {
	return prefix + "/" + TenantNameForObject(object)
}

func EncodeAsMetaName(name string, maxLength int) string {

	if addressNameExpression.MatchString(name) {
		return name
	}

	newPrefix := replaceExpression.ReplaceAllString(name, "")
	if len(newPrefix) > 0 {
		newPrefix = newPrefix + "-"
	}

	name = newPrefix + uuid.NewMD5(operatorUuidNamespace, []byte(name)).String()

	if maxLength > 0 {
		rname := []rune(name)
		l := len(rname)
		if l > maxLength {
			s := l - maxLength
			rname = rname[s:l]
			name = string(rname)
			name = replaceStartExpression.ReplaceAllString(name, "")
		}
	}

	return name
}

// Encode an address name so that it can be put inside the .metadata.name field of an Address object
func EncodeAddressSpaceAsMetaName(addressSpaceName string, addressName string) string {

	return addressSpaceName + "." + EncodeAsMetaName(addressName, 60)

}

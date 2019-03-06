package util

import (
	"os"
	"strings"
)

var (
	openshift *bool
)

func IsOpenshift() bool {
	if openshift == nil {
		b := detectOpenshift()
		openshift = &b
	}
	return *openshift
}

func detectOpenshift() bool {

	value, ok := os.LookupEnv("ENMASSE_OPENSHIFT")
	if ok {
		return strings.ToLower(value) == "true"
	}

	// FIXME: we should do a better job at "detecting"

	return true

}

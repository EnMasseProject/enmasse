package util

import (
	"os"
	"strings"

	"sigs.k8s.io/controller-runtime/pkg/client/config"

	"k8s.io/apimachinery/pkg/api/errors"

	routev1 "github.com/openshift/client-go/route/clientset/versioned/typed/route/v1"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

var (
	openshift *bool
	log       = logf.Log.WithName("util")
)

func IsOpenshift() bool {
	if openshift == nil {
		b := detectOpenshift()
		openshift = &b
	}
	return *openshift
}

func detectOpenshift() bool {

	log.Info("Detect if openshift is running")

	value, ok := os.LookupEnv("ENMASSE_OPENSHIFT")
	if ok {
		log.Info("Set by env-var 'ENMASSE_OPENSHIFT': " + value)
		return strings.ToLower(value) == "true"
	}

	// try to

	cfg, err := config.GetConfig()
	if err != nil {
		log.Error(err, "Error getting config: %v")
		return false
	}

	routeClient, err := routev1.NewForConfig(cfg)
	if err != nil {
		log.Error(err, "Failed to get routeClient")
		return false
	}

	_, err = routeClient.RESTClient().Get().DoRaw()
	if err == nil {
		log.Info("Rest call succeeded")
		return true
	}

	se, ok := err.(*errors.StatusError)
	if !ok {
		log.Info("Result is not a StatusError")
		return false
	}

	code := se.ErrStatus.Code
	log.Info("OpenShift detect", "code", code)
	return true

}

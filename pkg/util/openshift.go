package util

import (
	"fmt"
	"os"
	"strings"

	"sigs.k8s.io/controller-runtime/pkg/client/config"

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

	body, err := routeClient.RESTClient().Get().DoRaw()

	log.Info(fmt.Sprintf("Request error: %v", err))
	log.V(2).Info(fmt.Sprintf("Body: %v", string(body)))

	return err == nil
}

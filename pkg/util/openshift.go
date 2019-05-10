package util

import (
	"encoding/json"
	"fmt"
	"k8s.io/apimachinery/pkg/api/errors"
	"net/url"
	"os"
	"strings"
	"time"

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

	retries := 10
	for retries > 0 {
		body, err := routeClient.RESTClient().Get().DoRaw()
		log.Info(fmt.Sprintf("Request error: %v", err))
		log.V(2).Info(fmt.Sprintf("Body: %v", string(body)))

		if err == nil {
			return true
		}

		se, ok := err.(*errors.StatusError)
		if ok {
			code := se.Status().Code
			log.Info(fmt.Sprintf("Response code: %d", code))
			if code != 503 {
				return code >= 200 && code < 300
			}
		}

		retries -= 1
		time.Sleep(10 * time.Second)
	}

	return false
}

func OpenshiftUri() (*url.URL, bool, error) {

	data, err := WellKnownOauthMetadata()
	if err != nil {
		log.Error(err, "Error getting well-known OAuth metadata: %v")
		return nil, false, err
	}

	openshiftUrl := data["issuer"].(string)
	rewritten := false
	// When oc cluster is run without a  --public-hostname= argument, openshiftUrl will refer to a loopback, which
	// cannot be used from within a pod.  This works around this problem.
	if openshiftUrl == "" || strings.Contains(openshiftUrl, "https://localhost:8443") || strings.Contains(openshiftUrl, "https://127.0.0.1:8443") {
		openshiftUrl = fmt.Sprintf("https://%s:%s", GetEnvOrDefault("KUBERNETES_SERVICE_HOST", "172.30.0.1"), GetEnvOrDefault("KUBERNETES_SERVICE_PORT", "443"))
		rewritten = true
	}

	u, err := url.Parse(openshiftUrl)
	if err != nil {
		return nil, false, err
	}
	return u, rewritten, nil

}

func WellKnownOauthMetadata() (map[string]interface{}, error) {

	config, err := config.GetConfig()
	if err != nil {
		log.Error(err, "Error getting config: %v")
		return nil, err
	}

	client, err := routev1.NewForConfig(config)
	if err != nil {
		return nil, err
	}

	result := client.RESTClient().Get().AbsPath("/.well-known/oauth-authorization-server").Do()
	if err := result.Error(); err != nil {
		return nil, err
	}
	ret, err := result.Raw()
	if err != nil {
		return nil, err
	}
	data := make(map[string]interface{})
	err = json.Unmarshal(ret, &data)
	if err != nil {
		return nil, err
	}

	return data, nil
}

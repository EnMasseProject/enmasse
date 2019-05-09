/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package images

import (
	"fmt"
	"io/ioutil"
	"os"
	"strings"

	"gopkg.in/yaml.v2"
	corev1 "k8s.io/api/core/v1"
)

var (
	imageMap          map[string]string
	defaultPullPolicy corev1.PullPolicy
)

func loadImageMapFrom(fileName string) (map[string]string, error) {

	var data = make(map[string]string)

	file, err := os.Open(fileName)
	if err != nil {
		return nil, err
	}

	defer func() { _ = file.Close() }()

	byteValue, err := ioutil.ReadAll(file)
	if err != nil {
		return nil, err
	}

	if err := yaml.Unmarshal(byteValue, data); err != nil {
		return nil, err
	}

	return data, nil

}

func setIfEnvPresent(data map[string]string, key string, envVar string) {
	value, present := os.LookupEnv(envVar)
	if present {
		data[key] = value
	}
}

func overrideImageMapFromEnv(data map[string]string) {
	setIfEnvPresent(data, "iot-auth-service", "IOT_AUTH_SERVICE_IMAGE")
	setIfEnvPresent(data, "iot-device-registry-file", "IOT_DEVICE_REGISTRY_FILE_IMAGE")
	setIfEnvPresent(data, "iot-device-registry-infinispan", "IOT_DEVICE_REGISTRY_INFINISPAN_IMAGE")
	setIfEnvPresent(data, "iot-gc", "IOT_GC_IMAGE")
	setIfEnvPresent(data, "iot-http-adapter", "IOT_HTTP_ADAPTER_IMAGE")
	setIfEnvPresent(data, "iot-mqtt-adapter", "IOT_MQTT_ADAPTER_IMAGE")
	setIfEnvPresent(data, "iot-tenant-service", "IOT_TENANT_SERVICE_IMAGE")
	setIfEnvPresent(data, "iot-proxy-configurator", "IOT_PROXY_CONFIGURATOR_IMAGE")
	setIfEnvPresent(data, "qdrouterd-base", "QDROUTERD_BASE_IMAGE")
	setIfEnvPresent(data, "none-authservice", "NONE_AUTHSERVICE_IMAGE")
	setIfEnvPresent(data, "keycloak", "KEYCLOAK_IMAGE")
	setIfEnvPresent(data, "keycloak-plugin", "KEYCLOAK_PLUGIN_IMAGE")
	setIfEnvPresent(data, "console-init", "CONSOLE_INIT_IMAGE")
	setIfEnvPresent(data, "console-proxy-openshift", "CONSOLE_PROXY_OPENSHIFT_IMAGE")
	setIfEnvPresent(data, "console-proxy-kubernetes", "CONSOLE_PROXY_KUBERNETES_IMAGE")
	setIfEnvPresent(data, "console-httpd", "CONSOLE_HTTPD_IMAGE")
}

const defaultImageMapFileName = "operatorImageMap.yaml"

func init() {
	defaultPullPolicy = corev1.PullPolicy(os.Getenv("IMAGE_PULL_POLICY"))
}

func lazyLoadImageMap() {
	if imageMap != nil {
		return
	}

	fileName := os.Getenv("ENMASSE_IMAGE_MAP_FILE")
	if fileName == "" {
		fileName = defaultImageMapFileName
	}

	var err error
	imageMap, err = loadImageMapFrom(fileName)
	if err != nil {
		panic(err)
	}

	overrideImageMapFromEnv(imageMap)
}

func GetImage(name string) (string, error) {
	lazyLoadImageMap()

	if imageMap == nil {
		return "", fmt.Errorf("'%s' not found or 'ENMASSE_IMAGE_MAP_FILE' not set to a readable file, unable to lookup image names", defaultImageMapFileName)
	}

	value, ok := imageMap[name]
	if !ok {
		return "", fmt.Errorf("image '%s' not found in the mapping table", name)
	}

	return value, nil
}

func PullPolicyFromImageName(imageName string) corev1.PullPolicy {

	if defaultPullPolicy != "" {
		return defaultPullPolicy
	}

	if strings.HasSuffix(imageName, "-SNAPSHOT") || strings.HasSuffix(imageName, ":latest") {
		return corev1.PullAlways
	} else {
		return corev1.PullIfNotPresent
	}

}

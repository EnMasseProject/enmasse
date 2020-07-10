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
	setIfEnvPresent(data, "controller-manager", "RELATED_IMAGE_CONTROLLER_MANAGER")
	setIfEnvPresent(data, "access-control-server", "RELATED_IMAGE_ACCESS_CONTROL_SERVER")
	setIfEnvPresent(data, "iot-auth-service", "RELATED_IMAGE_IOT_AUTH_SERVICE")
	setIfEnvPresent(data, "iot-device-connection-infinispan", "RELATED_IMAGE_IOT_DEVICE_CONNECTION_INFINISPAN")
	setIfEnvPresent(data, "iot-device-connection-jdbc", "RELATED_IMAGE_IOT_DEVICE_CONNECTION_JDBC")
	setIfEnvPresent(data, "iot-device-registry-infinispan", "RELATED_IMAGE_IOT_DEVICE_REGISTRY_INFINISPAN")
	setIfEnvPresent(data, "iot-device-registry-jdbc", "RELATED_IMAGE_IOT_DEVICE_REGISTRY_JDBC")
	setIfEnvPresent(data, "iot-http-adapter", "RELATED_IMAGE_IOT_HTTP_ADAPTER")
	setIfEnvPresent(data, "iot-lorawan-adapter", "RELATED_IMAGE_IOT_LORAWAN_ADAPTER")
	setIfEnvPresent(data, "iot-mqtt-adapter", "RELATED_IMAGE_IOT_MQTT_ADAPTER")
	setIfEnvPresent(data, "iot-sigfox-adapter", "RELATED_IMAGE_IOT_SIGFOX_ADAPTER")
	setIfEnvPresent(data, "iot-project-cleaner", "RELATED_IMAGE_IOT_TENANT_CLEANER")
	setIfEnvPresent(data, "iot-project-service", "RELATED_IMAGE_IOT_TENANT_SERVICE")
	setIfEnvPresent(data, "iot-proxy-configurator", "RELATED_IMAGE_IOT_PROXY_CONFIGURATOR")
	setIfEnvPresent(data, "router", "RELATED_IMAGE_ROUTER")
	setIfEnvPresent(data, "none-authservice", "RELATED_IMAGE_NONE_AUTHSERVICE")
	setIfEnvPresent(data, "keycloak", "RELATED_IMAGE_KEYCLOAK")
	setIfEnvPresent(data, "keycloak-plugin", "RELATED_IMAGE_KEYCLOAK_PLUGIN")
	setIfEnvPresent(data, "console-init", "RELATED_IMAGE_CONSOLE_INIT")
	setIfEnvPresent(data, "console-proxy-openshift", "RELATED_IMAGE_CONSOLE_PROXY_OPENSHIFT")
	setIfEnvPresent(data, "console-proxy-kubernetes", "RELATED_IMAGE_CONSOLE_PROXY_KUBERNETES")
	setIfEnvPresent(data, "console-server", "RELATED_IMAGE_CONSOLE_SERVER")
	setIfEnvPresent(data, "address-space-controller", "RELATED_IMAGE_ADDRESS_SPACE_CONTROLLER")
	setIfEnvPresent(data, "standard-controller", "RELATED_IMAGE_STANDARD_CONTROLLER")
	setIfEnvPresent(data, "agent", "RELATED_IMAGE_AGENT")
	setIfEnvPresent(data, "broker", "RELATED_IMAGE_BROKER")
	setIfEnvPresent(data, "broker-plugin", "RELATED_IMAGE_BROKER_PLUGIN")
	setIfEnvPresent(data, "topic-forwarder", "RELATED_IMAGE_TOPIC_FORWARDER")
	setIfEnvPresent(data, "mqtt-gateway", "RELATED_IMAGE_MQTT_GATEWAY")
	setIfEnvPresent(data, "mqtt-lwt", "RELATED_IMAGE_MQTT_LWT")
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

func GetDefaultPullPolicy() corev1.PullPolicy {
	return defaultPullPolicy
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

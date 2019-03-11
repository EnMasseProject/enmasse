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

const defaultImageMapFileName = "/etc/operatorImageMap.yaml"

func init() {
	fileName := os.Getenv("ENMASSE_IMAGE_MAP_FILE")
	if fileName == "" {
		fileName = defaultImageMapFileName
	}
	if fileName != "" {
		var err error
		imageMap, err = loadImageMapFrom(fileName)
		if err != nil {
			panic(err)
		}
	}
	defaultPullPolicy = corev1.PullPolicy(os.Getenv("ENMASSE_DEFAULT_PULL_POLICY"))
}

func GetImage(name string) (string, error) {
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

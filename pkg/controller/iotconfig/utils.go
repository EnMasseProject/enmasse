/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	corev1 "k8s.io/api/core/v1"
)

func FindAdapterSpec(config *iotv1alpha1.IoTConfig, adapterType string) *iotv1alpha1.AdapterSpec {

	for _, a := range config.Spec.Adapters {
		if a.AdapterType == adapterType {
			return &a
		}
	}

	return nil
}

func MakeImageProperties(config *iotv1alpha1.IoTConfig) iotv1alpha1.ImageProperties {
	return install.FlattenImageProperties([]*iotv1alpha1.ImageProperties{
		&config.Spec.DefaultImageProperties,
	})
}

func SetContainerImage(container *corev1.Container, imageName string, properties iotv1alpha1.ImageProperties) error {

	image, err := install.MakeImage(imageName, properties)
	if err != nil {
		return err
	}

	container.Image = image
	container.ImagePullPolicy = *properties.PullPolicy

	return nil
}

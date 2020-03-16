/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package ext

import (
	"fmt"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	corev1 "k8s.io/api/core/v1"
	"strings"
)

// Do a standard mapping of the extension volume into the container
func MapExtensionVolume(container *corev1.Container) {
	install.ApplyVolumeMountSimple(container, "extensions", "/extensions", true)
}

// Add the standard extension folder to the deployment
func AddExtensionVolume(pod *corev1.PodSpec) {
	install.ApplyEmptyDirVolume(pod, "extensions")
}

// Add all provided extensions to the deployment
func AddExtensionContainers(extensions []iotv1alpha1.ExtensionImage, pod *corev1.PodSpec) error {

	// add extension containers

	expectedContainers := make([]string, 0)

	// add if we have some

	if extensions != nil {
		for _, ext := range extensions {
			if ext.Container.Name == "" {
				continue
			}

			if !strings.HasPrefix(ext.Container.Name, "ext-") {
				return fmt.Errorf("extension container names must start with 'ext-'")
			}

			expectedContainers = append(expectedContainers, ext.Container.Name)

			if containers, err := install.ApplyContainerWithError(pod.InitContainers, ext.Container.Name, func(container *corev1.Container) error {
				ext.Container.DeepCopyInto(container)
				return nil
			}); err != nil {
				return err
			} else {
				pod.InitContainers = containers
			}
		}
	}

	// cleanup containers

	pod.InitContainers = install.DeleteOtherContainers(
		pod.InitContainers,
		expectedContainers)

	// done

	return nil

}

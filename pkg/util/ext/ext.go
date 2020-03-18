/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package ext

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	corev1 "k8s.io/api/core/v1"
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
func AddExtensionContainers(extensions []iotv1alpha1.ExtensionImage, pod *corev1.PodSpec, prefix string) error {

	// add extension containers

	expectedContainers := make([]string, 0, len(extensions))

	for _, ext := range extensions {

		// no name, no container

		if ext.Container.Name == "" {
			continue
		}

		containerName := prefix + ext.Container.Name
		expectedContainers = append(expectedContainers, containerName)

		if containers, err := install.ApplyContainerWithError(pod.InitContainers, containerName, func(container *corev1.Container) error {
			ext.Container.DeepCopyInto(container)
			// restore container name, overwritten by DeepCopyInto
			container.Name = containerName
			return nil
		}); err != nil {
			return err
		} else {
			pod.InitContainers = containers
		}
	}

	// cleanup containers

	pod.InitContainers = install.DeleteOtherContainers(
		pod.InitContainers,
		prefix,
		expectedContainers)

	// done

	return nil

}

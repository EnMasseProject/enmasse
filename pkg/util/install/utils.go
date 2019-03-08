/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util/images"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

func createDefaultLabels(labels map[string]string, component string, app string, name string) map[string]string {

	if labels == nil {
		labels = make(map[string]string)
	}

	labels["component"] = component
	labels["app"] = app
	labels["name"] = name

	return labels
}

// Apply standard set of labels
func ApplyDefaultLabels(meta *v1.ObjectMeta, component string, app string, name string) {
	meta.Labels = createDefaultLabels(meta.Labels, component, app, name)
}

// Apply some default service values
func ApplyServiceDefaults(service *corev1.Service, component string, app string, name string) {

	ApplyDefaultLabels(&service.ObjectMeta, component, app, name)
	service.Spec.Selector = createDefaultLabels(nil, component, app, name)

}

// Apply some default deployment values
func ApplyDeploymentDefaults(deployment *appsv1.Deployment, component string, app string, name string) {

	ApplyDefaultLabels(&deployment.ObjectMeta, component, app, name)

	deployment.Spec.Selector = &v1.LabelSelector{
		MatchLabels: createDefaultLabels(nil, component, app, name),
	}

	deployment.Spec.Template.ObjectMeta.Labels = createDefaultLabels(deployment.Spec.Template.ObjectMeta.Labels, component, app, name)

}

func ApplyContainer(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container)) {
	// call "with error", and eat up the error
	_ = ApplyContainerWithError(deployment, name, func(container *corev1.Container) error {
		mutator(container)
		return nil
	})
}

func ApplyContainerWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container) error) error {

	if deployment.Spec.Template.Spec.Containers == nil {
		deployment.Spec.Template.Spec.Containers = make([]corev1.Container, 0)
	}

	for i, c := range deployment.Spec.Template.Spec.Containers {
		if c.Name == name {
			return mutator(&deployment.Spec.Template.Spec.Containers[i])
		}
	}

	c := &corev1.Container{
		Name: name,
	}

	err := mutator(c)
	if err == nil {
		deployment.Spec.Template.Spec.Containers = append(deployment.Spec.Template.Spec.Containers, *c)
	}

	return err
}

func ApplyPersistentVolume(deployment *appsv1.Deployment, name string, claimName string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.PersistentVolumeClaim == nil {
			volume.PersistentVolumeClaim = &corev1.PersistentVolumeClaimVolumeSource{}
		}
		volume.PersistentVolumeClaim.ClaimName = claimName
	})
}

func ApplyConfigMapVolume(deployment *appsv1.Deployment, name string, configMapName string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.ConfigMap == nil {
			volume.ConfigMap = &corev1.ConfigMapVolumeSource{}
		}
		volume.ConfigMap.Name = configMapName
	})
}

func ApplySecretVolume(deployment *appsv1.Deployment, name string, secretName string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.Secret == nil {
			volume.Secret = &corev1.SecretVolumeSource{}
		}
		volume.Secret.SecretName = secretName
	})
}

func ApplyEmptyDirVolume(deployment *appsv1.Deployment, name string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.EmptyDir == nil {
			volume.EmptyDir = &corev1.EmptyDirVolumeSource{}
		}
	})
}

func ApplyVolume(deployment *appsv1.Deployment, name string, mutator func(*corev1.Volume)) {
	// call "with error", and eat up the error
	_ = ApplyVolumeWithError(deployment, name, func(volume *corev1.Volume) error {
		mutator(volume)
		return nil
	})
}

func ApplyVolumeWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Volume) error) error {

	if deployment.Spec.Template.Spec.Volumes == nil {
		deployment.Spec.Template.Spec.Volumes = make([]corev1.Volume, 0)
	}

	for i, c := range deployment.Spec.Template.Spec.Volumes {
		if c.Name == name {
			return mutator(&deployment.Spec.Template.Spec.Volumes[i])
		}
	}

	v := &corev1.Volume{
		Name: name,
	}

	err := mutator(v)
	if err == nil {
		deployment.Spec.Template.Spec.Volumes = append(deployment.Spec.Template.Spec.Volumes, *v)
	}

	return err
}

func SetContainerImage(container *corev1.Container, imageName string, overrides v1beta1.ImageOverridesProvider) error {

	resolved, err := images.GetImage(imageName)
	if err != nil {
		return err
	}

	var pullPolicy corev1.PullPolicy

	overrideMap := overrides.GetImageOverrides()
	if overrideMap != nil {
		val, ok := overrideMap[imageName]
		if ok {
			if val.Name != "" {
				resolved = val.Name
			}
			if val.PullPolicy != "" {
				pullPolicy = val.PullPolicy
			}
		}
	}

	container.Image = resolved
	if pullPolicy != "" {
		container.ImagePullPolicy = pullPolicy
	} else {
		container.ImagePullPolicy = images.PullPolicyFromImageName(resolved)
	}

	return nil
}

// Apply a simple HTTP probe
func ApplyHttpProbe(probe *corev1.Probe, initialDelaySeconds int32, path string, port uint16) *corev1.Probe {
	if probe == nil {
		probe = &corev1.Probe{}
	}

	probe.InitialDelaySeconds = initialDelaySeconds
	probe.Exec = nil
	probe.TCPSocket = nil
	probe.HTTPGet = &corev1.HTTPGetAction{
		Path:   path,
		Port:   intstr.FromInt(int(port)),
		Scheme: corev1.URISchemeHTTP,
	}

	return probe
}

/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"fmt"
	"strings"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
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

// Extracts the default pull policy from a tag, unless provided
// The result of this function is guaranteed to be non-nil.
func PullPolicyFromTag(tag string, pullPolicy *corev1.PullPolicy) *corev1.PullPolicy {

	if pullPolicy != nil && *pullPolicy != "" {
		return pullPolicy
	}

	// eval

	if strings.HasSuffix(tag, "-SNAPSHOT") {
		s := corev1.PullAlways
		return &s
	} else {
		s := corev1.PullIfNotPresent
		return &s
	}

}

// Provide a default set of image properties.
func MakeDefaultImageProperties() iotv1alpha1.ImageProperties {

	useImageStream := defaultUseImageStreams
	repository := defaultRepository
	pullPolicy := defaultPullPolicy

	return iotv1alpha1.ImageProperties{
		Repository:     &repository,
		UseImageStream: &useImageStream,
		PullPolicy:     &pullPolicy,
		Tag:            defaultTag,
	}
}

func applyImageProperties(result *iotv1alpha1.ImageProperties, provided iotv1alpha1.ImageProperties) {

	if result == nil {
		return
	}

	if provided.Repository != nil {
		result.Repository = provided.Repository
	}
	if provided.UseImageStream != nil {
		result.UseImageStream = provided.UseImageStream
	}
	if provided.Tag != "" {
		result.Tag = provided.Tag
	}
	if provided.PullPolicy != nil {
		result.PullPolicy = provided.PullPolicy
	}

}

func FlattenImageProperties(properties []*iotv1alpha1.ImageProperties) iotv1alpha1.ImageProperties {

	// start with default

	result := MakeDefaultImageProperties()

	// Flatten

	for _, p := range properties {
		if p != nil {
			applyImageProperties(&result, *p)
		}
	}

	// eval pull policy, if not set

	result.PullPolicy = PullPolicyFromTag(result.Tag, result.PullPolicy)

	// return

	return result
}

func MakeImage(baseName string, properties iotv1alpha1.ImageProperties) (string, error) {
	if properties.Tag == "" {
		return "", fmt.Errorf("missing tag in image properties")
	}

	name := baseName + ":" + properties.Tag

	if properties.UseImageStream != nil && *properties.UseImageStream {
		return name, nil
	}

	if properties.Repository == nil || *properties.Repository == "" {
		return "", fmt.Errorf("missing repository in image properties")
	}

	name = *properties.Repository + "/" + name

	return name, nil
}

func SetContainerImage(container *corev1.Container, imageName string, properties iotv1alpha1.ImageProperties) error {

	image, err := MakeImage(imageName, properties)
	if err != nil {
		return err
	}

	container.Image = image
	container.ImagePullPolicy = *properties.PullPolicy

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

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
)

// Apply some default deployment values
func ApplyDeploymentDefaults(deployment *appsv1.Deployment, component string, app string, name string) {
	if deployment.Labels == nil {
		deployment.Labels = make(map[string]string)
	}

	deployment.Labels["component"] = component
	deployment.Labels["app"] = app
	deployment.Labels["name"] = name

	deployment.Spec.Selector = &v1.LabelSelector{
		MatchLabels: map[string]string{
			"component": component,
			"app":       app,
			"name":      name,
		},
	}

	if deployment.Spec.Template.ObjectMeta.Labels == nil {
		deployment.Spec.Template.ObjectMeta.Labels = make(map[string]string)
	}

	deployment.Spec.Template.ObjectMeta.Labels["component"] = component
	deployment.Spec.Template.ObjectMeta.Labels["app"] = app
	deployment.Spec.Template.ObjectMeta.Labels["name"] = name

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

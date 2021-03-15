/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"github.com/stretchr/testify/assert"
	"reflect"
	"testing"

	appv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func checkLabels(t *testing.T, component string, name string, labels map[string]string) {
	if labels["app"] != "enmasse" {
		t.Error("Metadata 'app' label is not 'enmasse'")
	}

	if labels["component"] != component {
		t.Errorf("Metadata 'component' label is not '%s'", component)
	}

	if labels["name"] != name {
		t.Errorf("Metadata 'name' label is not '%s'", name)
	}
}

func checkSelector(t *testing.T, component string, name string, selector *metav1.LabelSelector) {
	if selector == nil {
		t.Fatal("Null selector")
	}

	checkLabels(t, component, name, selector.MatchLabels)
}

func TestApplyDeploymentDefaults(t *testing.T) {
	d := appv1.Deployment{}
	ApplyDeploymentDefaults(&d, "mycomponent", "myname")

	checkLabels(t, "mycomponent", "myname", d.ObjectMeta.Labels)
	checkSelector(t, "mycomponent", "myname", d.Spec.Selector)
}

func TestAddContainer(t *testing.T) {
	d := appv1.Deployment{}

	_ = ApplyDeploymentContainerWithError(&d, "foo", func(container *corev1.Container) error {
		container.Image = "bar"
		return nil
	})

	if d.Spec.Template.Spec.Containers == nil {
		t.Fatalf("Nil container array")
	}
	if l := len(d.Spec.Template.Spec.Containers); l != 1 {
		t.Fatalf("Expected exactly one entry, got: %d", l)
	}
	if d.Spec.Template.Spec.Containers[0].Name != "foo" {
		t.Errorf("Container name must be 'foo', was: '%s'", d.Spec.Template.Spec.Containers[0].Name)
	}
	if d.Spec.Template.Spec.Containers[0].Image != "bar" {
		t.Errorf("Container image must be 'bar', was: '%s'", d.Spec.Template.Spec.Containers[0].Image)
	}
}

func TestReplaceContainer(t *testing.T) {
	d := appv1.Deployment{}

	_ = ApplyDeploymentContainerWithError(&d, "foo", func(container *corev1.Container) error {
		container.Image = "bar"
		return nil
	})
	_ = ApplyDeploymentContainerWithError(&d, "foo", func(container *corev1.Container) error {
		container.Image = "baz"
		return nil
	})

	if d.Spec.Template.Spec.Containers == nil {
		t.Fatalf("Nil container array")
	}
	if l := len(d.Spec.Template.Spec.Containers); l != 1 {
		t.Fatalf("Expected exactly one entry, got: %d", l)
	}
	if d.Spec.Template.Spec.Containers[0].Name != "foo" {
		t.Errorf("Container name must be 'foo', was: '%s'", d.Spec.Template.Spec.Containers[0].Name)
	}
	if d.Spec.Template.Spec.Containers[0].Image != "baz" {
		t.Errorf("Container image must be 'baz', was: '%s'", d.Spec.Template.Spec.Containers[0].Image)
	}
}

func TestTwoContainers(t *testing.T) {
	d := appv1.Deployment{}

	_ = ApplyDeploymentContainerWithError(&d, "foo", func(container *corev1.Container) error {
		container.Image = "bar"
		return nil
	})
	_ = ApplyDeploymentContainerWithError(&d, "foo2", func(container *corev1.Container) error {
		container.Image = "baz"
		return nil
	})
	_ = ApplyDeploymentContainerWithError(&d, "foo", func(container *corev1.Container) error {
		container.Image = "baz2"
		return nil
	})

	if d.Spec.Template.Spec.Containers == nil {
		t.Fatalf("Nil container array")
	}
	if l := len(d.Spec.Template.Spec.Containers); l != 2 {
		t.Fatalf("Expected exactly one entry, got: %d", l)
	}

	if d.Spec.Template.Spec.Containers[0].Name != "foo" {
		t.Errorf("Container name must be 'foo', was: '%s'", d.Spec.Template.Spec.Containers[0].Name)
	}
	if d.Spec.Template.Spec.Containers[0].Image != "baz2" {
		t.Errorf("Container image must be 'baz2', was: '%s'", d.Spec.Template.Spec.Containers[0].Image)
	}

	if d.Spec.Template.Spec.Containers[1].Name != "foo2" {
		t.Errorf("Container name must be 'foo2', was: '%s'", d.Spec.Template.Spec.Containers[0].Name)
	}
	if d.Spec.Template.Spec.Containers[1].Image != "baz" {
		t.Errorf("Container image must be 'baz', was: '%s'", d.Spec.Template.Spec.Containers[0].Image)
	}
}

func TestDeleteOthersWithPrefix(t *testing.T) {

	input := []corev1.Container{
		{Name: "foo"},
		{Name: "ext-bar"},
		{Name: "ext-baz"},
	}
	output := DeleteOtherContainers(input, "ext-", []string{"ext-baz"})
	expected := []corev1.Container{
		{Name: "foo"},
		{Name: "ext-baz"},
	}

	if !reflect.DeepEqual(output, expected) {
		t.Errorf("Failed\n\texpected: %v\n\tactual: %v", expected, output)
	}
}

func TestDeleteOthersWithoutPrefix(t *testing.T) {

	input := []corev1.Container{
		{Name: "foo"},
		{Name: "ext-bar"},
		{Name: "ext-baz"},
	}
	output := DeleteOtherContainers(input, "", []string{"ext-baz"})
	expected := []corev1.Container{
		{Name: "ext-baz"},
	}

	if !reflect.DeepEqual(output, expected) {
		t.Errorf("Failed\n\texpected: %v\n\tactual: %v", expected, output)
	}
}

func TestComputeConfigMapHash(t *testing.T) {

	hash := func(cm corev1.ConfigMap) string {
		hash, err := ComputeConfigMapHash(cm)
		assert.NoError(t, err)
		return hash
	}
	empty := corev1.ConfigMap{}

	assert.NotEqual(t, "", hash(empty))

	nameonly1 := corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name: "foo",
		},
	}
	nameonly2 := corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name: "foo",
		},
	}
	bar := corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name: "bar",
		},
	}
	data1 := corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name: "bar",
		},
		Data: map[string]string{
			"wibble": "wobble",
			"wobble": "wibble",
		},
	}
	data2 := corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name: "bar",
		},
		Data: map[string]string{
			"wobble": "wibble",
			"wibble": "wobble",
		},
	}

	assert.Equal(t, hash(nameonly1), hash(nameonly2))
	assert.NotEqual(t, hash(nameonly1), hash(bar))
	assert.Equal(t, hash(data1), hash(data2))
	assert.NotEqual(t, hash(bar), hash(data2))
}

func TestAppendEnvVar(t *testing.T) {

	container := corev1.Container{}

	assert.Nil(t, container.Env)
	assert.Len(t, container.Env, 0)

	// append first element

	AppendEnvVarValue(&container, JavaOptsEnvVarName, "foo")

	assert.NotNil(t, container.Env)
	assert.Len(t, container.Env, 1)
	assert.Equal(t, JavaOptsEnvVarName, container.Env[0].Name)
	assert.Equal(t, "foo", container.Env[0].Value)

	// append second element

	AppendEnvVarValue(&container, JavaOptsEnvVarName, "bar")

	assert.NotNil(t, container.Env)
	assert.Len(t, container.Env, 1)
	assert.Equal(t, JavaOptsEnvVarName, container.Env[0].Name)
	assert.Equal(t, "foo bar", container.Env[0].Value)

}

func TestApplyServiceAnnotationDefaults(t *testing.T) {
	annotations := make(map[string]string)
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return true})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameBeta])

	annotations = make(map[string]string)
	annotations[ServingCertSecretNameBeta] = "bar"
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return true})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameBeta])

	annotations = make(map[string]string)
	annotations[ServingCertSecretNameBeta] = "baz"
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return true})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameBeta])

	annotations = make(map[string]string)
	annotations[ServingCertSecretNameAlpha] = "baz"
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return true})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameBeta])

	annotations = make(map[string]string)
	annotations[ServingCertSecretNameAlpha] = "bar"
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return true})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameAlpha])

	annotations = make(map[string]string)
	annotations["foo"] = "goo"
	annotations[ServingCertSecretNameAlpha] = "bar"
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return true})
	assert.Len(t, annotations, 2)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameAlpha])
	assert.Equal(t, "goo", annotations["foo"])

	annotations = make(map[string]string)
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return false})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameAlpha])

	annotations = make(map[string]string)
	annotations[ServingCertSecretNameAlpha] = "baz"
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return true},
		func() bool {return false})
	assert.Len(t, annotations, 1)
	assert.Equal(t, "bar", annotations[ServingCertSecretNameAlpha])

	annotations = make(map[string]string)
	ApplyOpenShiftServingCertAnnotation(annotations, "bar",
		func() bool {return false},
		func() bool {return false})
	assert.Len(t, annotations, 0)
}

/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
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

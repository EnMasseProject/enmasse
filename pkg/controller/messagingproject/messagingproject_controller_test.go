/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingproject

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
)

func setup(t *testing.T, ns *corev1.Namespace) *ReconcileMessagingProject {
	s := scheme.Scheme
	s.AddKnownTypes(corev1.SchemeGroupVersion, &corev1.Namespace{})
	objs := []runtime.Object{
		ns,
	}
	cl := fake.NewFakeClientWithScheme(s, objs...)
	r := &ReconcileMessagingProject{client: cl}
	return r
}

func assertSelector(t *testing.T, expected bool, selector *v1.NamespaceSelector) {
	r := setup(t, &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "app1",
			Labels: map[string]string{"key1": "value1"},
		},
	})
	matches, err := r.matchesSelector(context.TODO(), "app1", selector)
	assert.Nil(t, err)
	assert.Equal(t, expected, matches)
}

func createSelectable(selector *v1.NamespaceSelector) v1.Selectable {
	return &v1.MessagingInfrastructure{
		Spec: v1.MessagingInfrastructureSpec{
			NamespaceSelector: selector,
		},
	}
}

func assertBestMatch(t *testing.T, expected v1.Selectable, selectable []v1.Selectable) {
	r := setup(t, &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "app1",
			Labels: map[string]string{"key1": "value1"},
		},
	})
	bestMatch, err := r.findBestMatch(context.TODO(), "app1", selectable)
	assert.Nil(t, err)
	assert.Equal(t, expected, bestMatch)
}

func TestMatchesSelectorGlobal(t *testing.T) {
	s := createSelectable(nil)
	assertSelector(t, true, s.GetSelector())
	assertBestMatch(t, s, []v1.Selectable{s})
}

func TestMatchesSelectorByName(t *testing.T) {
	s := createSelectable(&v1.NamespaceSelector{
		MatchNames: []string{"app3", "app1", "app2"},
	})
	assertSelector(t, true, s.GetSelector())
	assertBestMatch(t, s, []v1.Selectable{s})
}

func TestMatchesSelectorByMatchLabels(t *testing.T) {
	s := createSelectable(&v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1"},
	})
	assertSelector(t, true, s.GetSelector())
	assertBestMatch(t, s, []v1.Selectable{s})

	s = createSelectable(&v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1", "key2": "value2"},
	})
	assertSelector(t, false, s.GetSelector())
	assertBestMatch(t, nil, []v1.Selectable{s})
}

func TestMatchesSelectorByMatchExpressions(t *testing.T) {
	s := createSelectable(&v1.NamespaceSelector{
		MatchExpressions: []metav1.LabelSelectorRequirement{
			{
				Key:      "key1",
				Operator: metav1.LabelSelectorOpIn,
				Values:   []string{"value1"},
			},
		},
	})
	assertSelector(t, true, s.GetSelector())
	assertBestMatch(t, s, []v1.Selectable{s})

	s = createSelectable(&v1.NamespaceSelector{
		MatchExpressions: []metav1.LabelSelectorRequirement{
			{
				Key:      "key1",
				Operator: metav1.LabelSelectorOpExists,
			},
		},
	})
	assertSelector(t, true, s.GetSelector())
	assertBestMatch(t, s, []v1.Selectable{s})

	s = createSelectable(&v1.NamespaceSelector{
		MatchExpressions: []metav1.LabelSelectorRequirement{
			{
				Key:      "key1",
				Operator: metav1.LabelSelectorOpDoesNotExist,
			},
		},
	})
	assertSelector(t, false, s.GetSelector())
	assertBestMatch(t, nil, []v1.Selectable{s})
}

func TestBestMatchOrder(t *testing.T) {
	globalSelector := createSelectable(nil)
	nameSelector := createSelectable(&v1.NamespaceSelector{
		MatchNames: []string{"app3", "app1", "app2"},
	})
	labelSelector := createSelectable(&v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1"},
	})

	assertBestMatch(t, globalSelector, []v1.Selectable{globalSelector})
	assertBestMatch(t, nameSelector, []v1.Selectable{globalSelector, nameSelector})
	assertBestMatch(t, nameSelector, []v1.Selectable{globalSelector, nameSelector, labelSelector})
}

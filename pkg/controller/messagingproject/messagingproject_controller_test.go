/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingproject

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
)

type testCase struct {
	t *testing.T
	r *ReconcileMessagingProject
}

func setup(t *testing.T) *testCase {
	ns := &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "app1",
			Labels: map[string]string{"key1": "value1"},
		},
	}
	s := scheme.Scheme
	s.AddKnownTypes(corev1.SchemeGroupVersion, &corev1.Namespace{})
	objs := []runtime.Object{
		ns,
	}
	cl := fake.NewFakeClientWithScheme(s, objs...)
	r := &ReconcileMessagingProject{client: cl}
	return &testCase{
		r: r,
		t: t,
	}
}

func createSelectable(creationTime time.Time, selector *v1.NamespaceSelector) v1.Selectable {
	return &v1.MessagingInfrastructure{
		ObjectMeta: metav1.ObjectMeta{
			CreationTimestamp: metav1.Time{
				Time: creationTime,
			},
		},
		Spec: v1.MessagingInfrastructureSpec{
			NamespaceSelector: selector,
		},
	}
}

func (tc *testCase) assertSelector(expected bool, selector *v1.NamespaceSelector) {
	matches, err := tc.r.matchesSelector(context.TODO(), "app1", selector)
	assert.Nil(tc.t, err)
	assert.Equal(tc.t, expected, matches)
}

func (tc *testCase) assertBestMatch(expected v1.Selectable, selectable []v1.Selectable) {
	bestMatch, err := tc.r.findBestMatch(context.TODO(), "app1", selectable)
	assert.Nil(tc.t, err)
	assert.Equal(tc.t, expected, bestMatch)
}

func TestMatchesSelectorGlobal(t *testing.T) {
	tc := setup(t)
	s := createSelectable(time.Time{}, nil)
	tc.assertSelector(true, s.GetSelector())
	tc.assertBestMatch(s, []v1.Selectable{s})
}

func TestMatchesSelectorByName(t *testing.T) {
	tc := setup(t)
	s := createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchNames: []string{"app3", "app1", "app2"},
	})
	tc.assertSelector(true, s.GetSelector())
	tc.assertBestMatch(s, []v1.Selectable{s})

	s = createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchNames: []string{"app3", "app2"},
	})
	tc.assertSelector(false, s.GetSelector())
	tc.assertBestMatch(nil, []v1.Selectable{s})
}

func TestMatchesSelectorByMatchLabels(t *testing.T) {
	tc := setup(t)
	s := createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1"},
	})
	tc.assertSelector(true, s.GetSelector())
	tc.assertBestMatch(s, []v1.Selectable{s})

	s = createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1", "key2": "value2"},
	})
	tc.assertSelector(false, s.GetSelector())
	tc.assertBestMatch(nil, []v1.Selectable{s})
}

func TestMatchesSelectorByMatchExpressions(t *testing.T) {
	tc := setup(t)
	s := createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchExpressions: []metav1.LabelSelectorRequirement{
			{
				Key:      "key1",
				Operator: metav1.LabelSelectorOpIn,
				Values:   []string{"value1"},
			},
		},
	})
	tc.assertSelector(true, s.GetSelector())
	tc.assertBestMatch(s, []v1.Selectable{s})

	s = createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchExpressions: []metav1.LabelSelectorRequirement{
			{
				Key:      "key1",
				Operator: metav1.LabelSelectorOpExists,
			},
		},
	})
	tc.assertSelector(true, s.GetSelector())
	tc.assertBestMatch(s, []v1.Selectable{s})

	s = createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchExpressions: []metav1.LabelSelectorRequirement{
			{
				Key:      "key1",
				Operator: metav1.LabelSelectorOpDoesNotExist,
			},
		},
	})
	tc.assertSelector(false, s.GetSelector())
	tc.assertBestMatch(nil, []v1.Selectable{s})
}

func TestBestMatchOrder(t *testing.T) {
	tc := setup(t)
	oldGlobalSelector := createSelectable(time.Time{}, nil)
	globalSelector := createSelectable(time.Time{}.Add(1*time.Hour), nil)
	nameSelector := createSelectable(time.Time{}.Add(1*time.Hour), &v1.NamespaceSelector{
		MatchNames: []string{"app3", "app1", "app2"},
	})
	oldLabelSelector := createSelectable(time.Time{}, &v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1"},
	})
	labelSelector := createSelectable(time.Time{}.Add(1*time.Hour), &v1.NamespaceSelector{
		MatchLabels: map[string]string{"key1": "value1"},
	})

	tc.assertBestMatch(globalSelector, []v1.Selectable{globalSelector})
	tc.assertBestMatch(oldGlobalSelector, []v1.Selectable{oldGlobalSelector, globalSelector})
	tc.assertBestMatch(oldGlobalSelector, []v1.Selectable{globalSelector, oldGlobalSelector})
	tc.assertBestMatch(nameSelector, []v1.Selectable{globalSelector, nameSelector})
	tc.assertBestMatch(nameSelector, []v1.Selectable{globalSelector, nameSelector, labelSelector})
	tc.assertBestMatch(oldLabelSelector, []v1.Selectable{oldLabelSelector, labelSelector})
	tc.assertBestMatch(oldLabelSelector, []v1.Selectable{labelSelector, oldLabelSelector})
}

/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package finalizer

import (
	"context"
	"fmt"
	"os"
	"reflect"
	"testing"
	"time"

	"k8s.io/client-go/kubernetes/scheme"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"

	"k8s.io/apimachinery/pkg/runtime"

	"sigs.k8s.io/controller-runtime/pkg/client/fake"
)

func TestMain(m *testing.M) {
	if err := v1alpha1.SchemeBuilder.AddToScheme(scheme.Scheme); err != nil {
		panic("Failed to register schema")
	}
	os.Exit(m.Run())
}

func TestRemoveFinalizer(t *testing.T) {
	data := []struct {
		finalizers []string
		remove     string
		result     []string
	}{
		{[]string{"foo"}, "foo", []string{}},
		{[]string{}, "foo", []string{}},
		{[]string{"foo", "bar"}, "foo", []string{"bar"}},
		{[]string{"foo", "bar"}, "bar", []string{"foo"}},
	}

	for _, e := range data {
		result := removeFinalizer(e.remove, e.finalizers)
		if !reflect.DeepEqual(result, e.result) {
			t.Errorf("Failed to remove '%s' from '%v' - expected: %v, got: %v", e.remove, e.finalizers, e.result, result)
		}
	}
}

func TestAdd(t *testing.T) {

	project := &v1alpha1.IoTProject{}

	objs := []runtime.Object{project}

	client := fake.NewFakeClientWithScheme(scheme.Scheme, objs...)

	result, err := ProcessFinalizers(context.TODO(), client, client, project, []Finalizer{
		{Name: "foo"},
	})

	if err != nil {
		t.Error("Should not have an error")
	}

	//noinspection GoNilness
	if !result.Requeue {
		t.Error("Expected requeue")
	}

	if len(project.Finalizers) != 1 {
		t.Fatal("Must have one element")
	}
	if project.Finalizers[0] != "foo" {
		t.Error("Must be 'foo'")
	}

}

func TestRemove1(t *testing.T) {

	ts := v1.Time{}
	project := &v1alpha1.IoTProject{
		ObjectMeta: v1.ObjectMeta{
			DeletionTimestamp: &ts,
			Finalizers:        []string{"foo"},
		},
	}

	objs := []runtime.Object{project}

	client := fake.NewFakeClientWithScheme(scheme.Scheme, objs...)

	i := 0

	result, err := ProcessFinalizers(context.TODO(), client, client, project, []Finalizer{
		{
			Name: "foo", Deconstruct: func(ctx DeconstructorContext) (result reconcile.Result, e error) {
				i++
				return reconcile.Result{Requeue: true}, nil
			},
		},
	})

	if err != nil {
		t.Fatal("Should not have an error")
	}

	if !result.Requeue {
		t.Error("Expected requeue")
	}

	if i != 1 {
		t.Error("Should have called destructor")
	}

	if len(project.Finalizers) != 1 {
		t.Fatal("Must have one element")
	}

	result, err = ProcessFinalizers(context.TODO(), client, client, project, []Finalizer{
		{
			Name: "foo", Deconstruct: func(ctx DeconstructorContext) (result reconcile.Result, e error) {
				i++
				return reconcile.Result{Requeue: false}, nil
			},
		},
	})

	if err != nil {
		t.Fatal("Should not have an error")
	}

	if !result.Requeue {
		t.Error("Expected requeue")
	}

	if i != 2 {
		t.Error("Should have called destructor again")
	}

	if len(project.Finalizers) != 0 {
		t.Fatal("Must have no element")
	}

	result, err = ProcessFinalizers(context.TODO(), client, client, project, []Finalizer{
		{
			Name: "foo", Deconstruct: func(ctx DeconstructorContext) (result reconcile.Result, e error) {
				i++
				return reconcile.Result{Requeue: false}, nil
			},
		},
	})

	if err != nil {
		t.Fatal("Should not have an error")
	}

	if result.Requeue {
		t.Error("Expected NO requeue")
	}

	if i != 2 {
		t.Error("Should NOT have called destructor again")
	}

}

type FinalizerTestStep struct {
	RequestRequeue      bool
	RequestRequeueAfter time.Duration
	RequestError        error

	Called       string
	Error        error
	Requeue      bool
	RequeueAfter time.Duration
	Finalizers   []string
}

//noinspection GoNilness
func RunFinalizerSteps(t *testing.T, project *v1alpha1.IoTProject, finalizers []string, steps []FinalizerTestStep) {
	objs := []runtime.Object{project}

	client := fake.NewFakeClientWithScheme(scheme.Scheme, objs...)

	i := 0

	for _, s := range steps {

		i++

		t.Run(fmt.Sprintf("Step%03d", i), func(t *testing.T) {
			called := ""

			mockFinalizers := make([]Finalizer, 0)
			for _, f := range finalizers {
				var fcopy = f
				mockFinalizers = append(mockFinalizers, Finalizer{
					Name: fcopy, Deconstruct: func(ctx DeconstructorContext) (result reconcile.Result, e error) {
						called += fcopy
						return reconcile.Result{Requeue: s.RequestRequeue, RequeueAfter: s.RequestRequeueAfter}, s.RequestError
					},
				})
			}

			result, err := ProcessFinalizers(context.TODO(), client, client, project, mockFinalizers)

			if (err != nil) != (s.Error != nil) {
				t.Errorf("Error state mismatch - expected: %v, found: %v", s.Error, err)
			}
			if called != s.Called {
				t.Errorf("Called state mismatch - expected: %v, found: %v", s.Called, called)
			}
			if result.Requeue != s.Requeue {
				t.Errorf("Requeue state mismatch - expected: %v, found: %v", s.Requeue, result.Requeue)
			}
			if result.RequeueAfter != s.RequeueAfter {
				t.Errorf("RequeueAfter state mismatch - expected: %v, found: %v", s.RequeueAfter, result.RequeueAfter)
			}
			if !reflect.DeepEqual(project.Finalizers, s.Finalizers) {
				t.Errorf("Finalizer mismatch - expected: %v, found: %v", s.Finalizers, project.Finalizers)
			}
		})

	}
}

func TestFinalizersSimple1(t *testing.T) {

	ts := v1.Time{}
	project := &v1alpha1.IoTProject{
		ObjectMeta: v1.ObjectMeta{
			DeletionTimestamp: &ts,
			Finalizers:        []string{"foo"},
		},
	}

	RunFinalizerSteps(t, project, []string{"foo"}, []FinalizerTestStep{

		// #1: First call, process started, but delayed
		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "foo",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"foo"},
		},

		// #2: "foo" is done
		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "foo",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{},
		},

		// #3: deletion complete
		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "",
			Error:        nil,
			Requeue:      false,
			RequeueAfter: 0,
			Finalizers:   []string{},
		},
	})

}

// Test running on an object which is not deleted
func TestFinalizersNotDeleted(t *testing.T) {

	project := &v1alpha1.IoTProject{
		ObjectMeta: v1.ObjectMeta{
			Finalizers: []string{"foo"},
		},
	}

	RunFinalizerSteps(t, project, []string{"foo"}, []FinalizerTestStep{
		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "",
			Error:        nil,
			Requeue:      false,
			RequeueAfter: 0,
			Finalizers:   []string{"foo"},
		},
	})

}

func TestTwoFinalizers(t *testing.T) {

	ts := v1.Time{}
	project := &v1alpha1.IoTProject{
		ObjectMeta: v1.ObjectMeta{
			Finalizers:        []string{"foo", "bar"},
			DeletionTimestamp: &ts,
		},
	}

	RunFinalizerSteps(t, project, []string{"foo", "bar"}, []FinalizerTestStep{

		// #1: we report that we need another try

		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "foo",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"foo", "bar"},
		},

		// #2: no change ... another try

		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "foo",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"foo", "bar"},
		},

		// #3: "foo" is done

		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "foo",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"bar"},
		},

		// #4: "bar" called, we ask for more time

		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "bar",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"bar"},
		},

		// #5: "bar" is still working

		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "bar",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"bar"},
		},

		// #6: "bar" is done

		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "bar",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{},
		},

		// #7: we should be all done

		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "",
			Error:        nil,
			Requeue:      false,
			RequeueAfter: 0,
			Finalizers:   []string{},
		},
	})

}

func TestTwoFinalizersOneUnknown(t *testing.T) {

	ts := v1.Time{}
	project := &v1alpha1.IoTProject{
		ObjectMeta: v1.ObjectMeta{
			Finalizers:        []string{"foo", "bar"},
			DeletionTimestamp: &ts,
		},
	}

	RunFinalizerSteps(t, project, []string{"bar"}, []FinalizerTestStep{

		// #1: we report that we need another try

		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "bar",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"foo", "bar"},
		},

		// #2: no change ... another try

		{
			RequestRequeue:      true,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "bar",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"foo", "bar"},
		},

		// #3: "bar" is done

		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "bar",
			Error:        nil,
			Requeue:      true,
			RequeueAfter: 0,
			Finalizers:   []string{"foo"},
		},

		// #4: but we don't known about "foo"

		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "",
			Error:        nil,
			Requeue:      false,
			RequeueAfter: 0,
			Finalizers:   []string{"foo"},
		},

		// #5: still don't known about "foo"

		{
			RequestRequeue:      false,
			RequestRequeueAfter: 0,
			RequestError:        nil,

			Called:       "",
			Error:        nil,
			Requeue:      false,
			RequeueAfter: 0,
			Finalizers:   []string{"foo"},
		},
	})

}

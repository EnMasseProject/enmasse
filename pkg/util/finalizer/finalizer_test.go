/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package finalizer

import (
	"context"
	"fmt"
	"k8s.io/client-go/tools/record"
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
	recorder := record.NewFakeRecorder(10)

	result, err := ProcessFinalizers(context.TODO(), client, client, recorder, project, []Finalizer{
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
	recorder := record.NewFakeRecorder(10)

	i := 0

	result, err := ProcessFinalizers(context.TODO(), client, client, recorder, project, []Finalizer{
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

	result, err = ProcessFinalizers(context.TODO(), client, client, recorder, project, []Finalizer{
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

	result, err = ProcessFinalizers(context.TODO(), client, client, recorder, project, []Finalizer{
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

type finalizerInput struct {
	Name         string
	Requeue      bool
	RequeueAfter time.Duration
	Error        error
}

type FinalizerTestStep struct {
	Input []finalizerInput

	ExpectedCalled       []string
	ExpectedError        error
	ExpectedRequeue      bool
	ExpectedRequeueAfter time.Duration
	ExpectedFinalizers   []string
}

//noinspection GoNilness
func RunFinalizerSteps(t *testing.T, project *v1alpha1.IoTProject, steps []FinalizerTestStep) {
	objs := []runtime.Object{project}

	client := fake.NewFakeClientWithScheme(scheme.Scheme, objs...)
	recorder := record.NewFakeRecorder(10)

	i := 0

	for _, s := range steps {

		i++

		t.Run(fmt.Sprintf("Step%03d", i), func(t *testing.T) {
			called := make([]string, 0)

			mockFinalizers := make([]Finalizer, 0)
			for _, f := range s.Input {
				// you need to copy iterator values in go, otherwise it will be replaced with the next iteration value
				var fcopy = f
				mockFinalizers = append(mockFinalizers, Finalizer{
					Name: fcopy.Name, Deconstruct: func(ctx DeconstructorContext) (result reconcile.Result, e error) {
						called = append(called, fcopy.Name)
						return reconcile.Result{Requeue: fcopy.Requeue, RequeueAfter: fcopy.RequeueAfter}, fcopy.Error
					},
				})
			}

			result, err := ProcessFinalizers(context.TODO(), client, client, recorder, project, mockFinalizers)

			if (err != nil) != (s.ExpectedError != nil) {
				t.Errorf("Error state mismatch - expected: %v, found: %v", s.ExpectedError, err)
			}
			if !reflect.DeepEqual(called, s.ExpectedCalled) {
				t.Errorf("Called state mismatch - expected: %v, found: %v", s.ExpectedCalled, called)
			}
			if result.Requeue != s.ExpectedRequeue {
				t.Errorf("Requeue state mismatch - expected: %v, found: %v", s.ExpectedRequeue, result.Requeue)
			}
			if result.RequeueAfter != s.ExpectedRequeueAfter {
				t.Errorf("RequeueAfter state mismatch - expected: %v, found: %v", s.ExpectedRequeueAfter, result.RequeueAfter)
			}
			if !reflect.DeepEqual(project.Finalizers, s.ExpectedFinalizers) {
				t.Errorf("Finalizers mismatch - expected: %v, found: %v", s.ExpectedFinalizers, project.Finalizers)
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

	RunFinalizerSteps(t, project, []FinalizerTestStep{

		// #1: First call, process started for "foo", but delayed
		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
		},

		// #2: "foo" is done
		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      false,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{},
		},

		// #3: deletion complete
		{
			Input: []finalizerInput{},

			ExpectedCalled:       []string{},
			ExpectedError:        nil,
			ExpectedRequeue:      false,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{},
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

	RunFinalizerSteps(t, project, []FinalizerTestStep{
		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{},
			ExpectedError:        nil,
			ExpectedRequeue:      false,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
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

	RunFinalizerSteps(t, project, []FinalizerTestStep{

		// #1: we report that we need another try for "foo" and "bar

		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo", "bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo", "bar"},
		},

		// #2: no change ... another try

		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo", "bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo", "bar"},
		},

		// #3: "foo" is done, "bar" needs more time

		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      false,
					RequeueAfter: 0,
					Error:        nil,
				},
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo"}, // "foo" must be persisted, so "bar" not called
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"bar"},
		},

		// #4: "bar" called again, we ask for more time

		{
			Input: []finalizerInput{
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"bar"},
		},

		// #5: "bar" is still working

		{
			Input: []finalizerInput{
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"bar"},
		},

		// #6: "bar" is done

		{
			Input: []finalizerInput{
				{
					Name:         "bar",
					Requeue:      false,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{},
		},

		// #7: we should be all done

		{
			Input: []finalizerInput{},

			ExpectedCalled:       []string{},
			ExpectedError:        nil,
			ExpectedRequeue:      false,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{},
		},
	})

}

func TestTwoFinalizersOneEarly(t *testing.T) {

	ts := v1.Time{}
	project := &v1alpha1.IoTProject{
		ObjectMeta: v1.ObjectMeta{
			Finalizers:        []string{"foo", "bar"},
			DeletionTimestamp: &ts,
		},
	}

	RunFinalizerSteps(t, project, []FinalizerTestStep{

		// #1: we report that we need another try for "foo", but "bar" passes

		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
				{
					Name:         "bar",
					Requeue:      false,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo", "bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
		},

		// #2: no change ... another try

		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
		},

		// #3: "foo" is done

		{
			Input: []finalizerInput{
				{
					Name:         "foo",
					Requeue:      false,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"foo"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{},
		},

		// #4: we should be all done

		{
			Input: []finalizerInput{},

			ExpectedCalled:       []string{},
			ExpectedError:        nil,
			ExpectedRequeue:      false,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{},
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

	RunFinalizerSteps(t, project, []FinalizerTestStep{

		// #1: we report that we need another try

		{
			Input: []finalizerInput{
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo", "bar"},
		},

		// #2: no change ... another try

		{
			Input: []finalizerInput{
				{
					Name:         "bar",
					Requeue:      true,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo", "bar"},
		},

		// #3: "bar" is done

		{
			Input: []finalizerInput{
				{
					Name:         "bar",
					Requeue:      false,
					RequeueAfter: 0,
					Error:        nil,
				},
			},

			ExpectedCalled:       []string{"bar"},
			ExpectedError:        nil,
			ExpectedRequeue:      true,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
		},

		// #4: but we don't known about "foo"

		{
			Input: []finalizerInput{},

			ExpectedCalled:       []string{},
			ExpectedError:        nil,
			ExpectedRequeue:      false,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
		},

		// #5: still don't known about "foo"

		{
			Input: []finalizerInput{},

			ExpectedCalled:       []string{},
			ExpectedError:        nil,
			ExpectedRequeue:      false,
			ExpectedRequeueAfter: 0,
			ExpectedFinalizers:   []string{"foo"},
		},
	})

}

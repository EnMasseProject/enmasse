/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package recon

import (
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"testing"
	"time"
)

func TestRescheduleAfter(t *testing.T) {
	rc := ReconcileContext{}

	if rc.NeedRequeue() != false {
		t.Error("NeedRequeue must be false initially")
	}

	rc.AddResult(reconcile.Result{RequeueAfter: time.Second * 5}, nil)

	if rc.NeedRequeue() != true {
		t.Error("NeedRequeue must be true now")
	}
	if rc.requeueAfter != time.Second*5 {
		t.Errorf("requeueAfter must be 5 seconds, is %d ms", rc.requeueAfter)
	}

	rc.AddResult(reconcile.Result{RequeueAfter: time.Second * 10}, nil)

	if rc.NeedRequeue() != true {
		t.Error("NeedRequeue must be true now")
	}
	if rc.requeueAfter != time.Second*5 {
		t.Errorf("requeueAfter must still be 5 seconds, is %d ms", rc.requeueAfter)
	}

	rc.AddResult(reconcile.Result{RequeueAfter: time.Second * 1}, nil)

	if rc.NeedRequeue() != true {
		t.Error("NeedRequeue must still be true")
	}
	if rc.requeueAfter != time.Second*1 {
		t.Errorf("requeueAfter must now be 1 second, is %d ms", rc.requeueAfter)
	}

	rc.AddResult(reconcile.Result{}, nil)

	if rc.NeedRequeue() != true {
		t.Error("NeedRequeue must still be true")
	}
	if rc.requeueAfter != time.Second*1 {
		t.Errorf("requeueAfter must still be 1 second, is %d ms", rc.requeueAfter)
	}

}

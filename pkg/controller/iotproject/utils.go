/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	"k8s.io/apimachinery/pkg/apis/meta/v1"
)

const annotationBase = "iot.enmasse.io"

// Ensure that a controller owner is set
// As there may only be one, we only to this when the creation timestamp is zero
func (r *ReconcileIoTProject) ensureControllerOwnerIsSet(owner, object v1.Object) error {

	if util.IsNewObject(object) {
		err := controllerutil.SetControllerReference(owner, object, r.scheme)
		if err != nil {
			return err
		}
	}

	return nil
}

func StringOrDefault(value string, defaultValue string) string {
	if value == "" {
		return defaultValue
	} else {
		return value
	}
}

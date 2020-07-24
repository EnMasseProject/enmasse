/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iottenant

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util/iot"

	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	"k8s.io/apimachinery/pkg/apis/meta/v1"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

const annotationBase = "iot.enmasse.io"

type AddressType int

const (
	AddressTypeAnycast AddressType = iota
	AddressTypeQueue
)

// Ensure that a controller owner is set
// As there may only be one, we only to this when the creation timestamp is zero
func (r *ReconcileIoTTenant) ensureControllerOwnerIsSet(owner, object v1.Object) error {

	if util.IsNewObject(object) {
		err := controllerutil.SetControllerReference(owner, object, r.scheme)
		if err != nil {
			return err
		}
	}

	return nil
}

// get infrastructure config
func getIoTConfigInstance(ctx context.Context, r client.Reader) (*iotv1.IoTInfrastructure, error) {

	namespace, err := util.GetInfrastructureNamespace()
	if err != nil {
		return nil, err
	}
	name, err := iot.GetIoTInfrastructureName()
	if err != nil {
		return nil, err
	}

	config := &iotv1.IoTInfrastructure{}
	if err := r.Get(ctx, client.ObjectKey{
		Namespace: namespace,
		Name:      name,
	}, config); err != nil {
		return nil, err
	}

	return config, err
}

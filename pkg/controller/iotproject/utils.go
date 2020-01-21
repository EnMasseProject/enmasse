/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"

	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	"k8s.io/apimachinery/pkg/apis/meta/v1"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
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

func getIoTConfigName() (string, error) {
	return util.GetEnvOrError("IOT_CONFIG_NAME")
}

func getInfrastructureNamespace() (string, error) {
	return util.GetEnvOrError("NAMESPACE")
}

// get infrastructure config
func getIoTConfigInstance(ctx context.Context, r client.Reader) (*iotv1alpha1.IoTConfig, error) {

	namespace, err := getInfrastructureNamespace()
	if err != nil {
		return nil, err
	}
	name, err := getIoTConfigName()
	if err != nil {
		return nil, err
	}

	config := &iotv1alpha1.IoTConfig{}
	if err := r.Get(ctx, client.ObjectKey{
		Namespace: namespace,
		Name:      name,
	}, config); err != nil {
		return nil, err
	}

	return config, err
}

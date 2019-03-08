/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
)

var _ v1beta1.ImageOverridesProvider = &IoTConfig{}
var _ v1beta1.ImageOverridesProvider = &IoTConfigSpec{}

func (config *IoTConfig) WantDefaultRoutes() bool {
	return config.Spec.WantDefaultRoutes()
}

func (spec *IoTConfigSpec) WantDefaultRoutes() bool {

	if spec.EnableDefaultRoutes != nil {
		return *spec.EnableDefaultRoutes
	}

	return util.IsOpenshift()

}

func (config *IoTConfig) GetImageOverrides() []v1beta1.ImageOverride {
	return config.Spec.ImageOverrides
}

func (spec *IoTConfigSpec) GetImageOverrides() []v1beta1.ImageOverride {
	return spec.ImageOverrides
}

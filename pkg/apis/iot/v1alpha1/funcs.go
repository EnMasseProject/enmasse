/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"crypto/md5"
	"encoding/pem"
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
)

var _ v1beta1.ImageOverridesProvider = &IoTConfig{}
var _ v1beta1.ImageOverridesProvider = &IoTConfigSpec{}

// Get the IoT tenant name from an IoT project.
// This is not in any way encoded
func (project *IoTProject) TenantName() string {
	return util.TenantName(project.Namespace, project.Name)
}

func (config *IoTConfig) WantDefaultRoutes(adapter *AdapterEndpointConfig) bool {

	if adapter != nil && adapter.EnableDefaultRoute != nil {
		return *adapter.EnableDefaultRoute
	}

	return config.Spec.WantDefaultRoutes()
}

func (spec *IoTConfigSpec) WantDefaultRoutes() bool {

	if spec.EnableDefaultRoutes != nil {
		return *spec.EnableDefaultRoutes
	}

	return util.IsOpenshift()

}

func (config *IoTConfig) GetImageOverrides() map[string]v1beta1.ImageOverride {
	return config.Spec.ImageOverrides
}

func (spec *IoTConfigSpec) GetImageOverrides() map[string]v1beta1.ImageOverride {
	return spec.ImageOverrides
}

func (spec *IoTConfigSpec) HasNoInterServiceConfig() bool {
	if spec.InterServiceCertificates == nil {
		return true
	}

	return spec.InterServiceCertificates.ServiceCAStrategy == nil && spec.InterServiceCertificates.SecretCertificatesStrategy == nil
}

func (k *KeyCertificateStrategy) HashString() string {
	p, _ := pem.Decode(k.Key)
	// we are using an MD5 fingerprint here, since this is only a name
	return fmt.Sprintf("%x", md5.Sum(p.Bytes))
}

// Evaluates if the adapter endpoint uses a custom certificate setup
func (a *AdapterEndpointConfig) HasCustomCertificate() bool {
	return a.KeyCertificateStrategy != nil ||
		a.SecretNameStrategy != nil
}

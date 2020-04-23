/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var _ v1beta1.ImageOverridesProvider = &IoTConfig{}
var _ v1beta1.ImageOverridesProvider = &IoTConfigSpec{}

// Get the IoT tenant name from an IoT project.
// This is not in any way encoded
func (project *IoTProject) TenantName() string {
	return util.TenantNameForObject(project)
}

func (config *IoTConfig) WantDefaultRoutes(endpoint EndpointConfig) bool {

	if endpoint.EnableDefaultRoute != nil {
		return *endpoint.EnableDefaultRoute
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

// Evaluates if the adapter endpoint uses a custom certificate setup
func (a *EndpointConfig) HasCustomCertificate() bool {
	return a.SecretNameStrategy != nil
}

func (c *IoTConfigSpec) DefaultNativeTlsRequired() bool {
	if c.JavaDefaults.RequireNativeTls != nil {
		return *c.JavaDefaults.RequireNativeTls
	}
	return util.DefaultJavaRequiresNativeTls()
}

//region CommonAdapterConfig

func (c *CommonAdapterConfig) IsNativeTlsRequired(config *IoTConfig) bool {
	if c.Java.RequireNativeTls == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	return *c.Java.RequireNativeTls
}

func (c *CommonAdapterConfig) TlsVersions(config *IoTConfig) []string {
	if len(c.Tls.Versions) > 0 {
		return c.Tls.Versions
	} else {
		return config.Spec.TlsDefaults.Versions
	}
}

//endregion

//region CommonServiceConfig

func (c *CommonServiceConfig) IsNativeTlsRequired(config *IoTConfig) bool {
	if c.Java == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	if c.Java.RequireNativeTls == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	return *c.Java.RequireNativeTls
}

func (c *CommonServiceConfig) TlsVersions(config *IoTConfig) []string {
	if len(c.Tls.Versions) > 0 {
		return c.Tls.Versions
	} else {
		return config.Spec.TlsDefaults.Versions
	}
}

//endregion

func (p *IoTProjectStatus) GetProjectCondition(t ProjectConditionType) *ProjectCondition {
	for i, c := range p.Conditions {
		if c.Type == t {
			return &p.Conditions[i]
		}
	}

	nc := ProjectCondition{
		Type: t,
		CommonCondition: CommonCondition{
			Status:             corev1.ConditionUnknown,
			LastTransitionTime: metav1.Now(),
		},
	}

	p.Conditions = append(p.Conditions, nc)

	return &p.Conditions[len(p.Conditions)-1]
}

func (config *IoTConfigStatus) GetConfigCondition(t ConfigConditionType) *ConfigCondition {
	for i, c := range config.Conditions {
		if c.Type == t {
			return &config.Conditions[i]
		}
	}

	nc := ConfigCondition{
		Type: t,
		CommonCondition: CommonCondition{
			Status:             corev1.ConditionUnknown,
			LastTransitionTime: metav1.Now(),
		},
	}

	config.Conditions = append(config.Conditions, nc)

	return &config.Conditions[len(config.Conditions)-1]
}

func (c *CommonCondition) SetStatus(status corev1.ConditionStatus, reason string, message string) {

	if c.Status != status {
		c.Status = status
		c.LastTransitionTime = metav1.Now()
	}

	c.Reason = reason
	c.Message = message

}

func (c *CommonCondition) IsOk() bool {
	return c.Status == corev1.ConditionTrue
}

// Sets the status to "True". "Reason" and "Message" to empty.
func (c *CommonCondition) SetStatusOk() {
	c.SetStatus(corev1.ConditionTrue, "", "")
}

// Call SetStatusOk() when "ok" is true. Otherwise calls SetStatus() with the provided
// reason and message.
func (c *CommonCondition) SetStatusOkOrElse(ok bool, reason string, message string) {
	if ok {
		c.SetStatusOk()
	} else {
		c.SetStatus(corev1.ConditionFalse, reason, message)
	}
}

//region DeviceConnection

type DeviceConnectionImplementation int

const (
	DeviceConnectionDefault = iota
	DeviceConnectionIllegal
	DeviceConnectionInfinispan
	DeviceConnectionJdbc
)

func (config IoTConfig) EvalDeviceConnectionImplementation() DeviceConnectionImplementation {

	var infinispan = config.Spec.ServicesConfig.DeviceConnection.Infinispan
	if infinispan != nil && infinispan.Disabled {
		infinispan = nil
	}
	var jdbc = config.Spec.ServicesConfig.DeviceConnection.JDBC
	if jdbc != nil && jdbc.Disabled {
		jdbc = nil
	}

	if false { // this is just here to align the other lines
	} else if infinispan == nil && jdbc == nil {
		return DeviceConnectionIllegal
	} else if infinispan != nil && jdbc == nil {
		return DeviceConnectionInfinispan
	} else if infinispan == nil && jdbc != nil {
		return DeviceConnectionJdbc
	}
	return DeviceConnectionIllegal
}

//endregion

//region Common DeviceRegistry

type DeviceRegistryImplementation int

const (
	DeviceRegistryDefault = iota
	DeviceRegistryIllegal
	DeviceRegistryInfinispan
	DeviceRegistryJdbc
)

func (config IoTConfig) EvalDeviceRegistryImplementation() DeviceRegistryImplementation {

	var infinispan = config.Spec.ServicesConfig.DeviceRegistry.Infinispan
	if infinispan != nil && infinispan.Disabled {
		infinispan = nil
	}
	var jdbc = config.Spec.ServicesConfig.DeviceRegistry.JDBC
	if jdbc != nil && jdbc.Disabled {
		jdbc = nil
	}

	if false { // this is just here to align the other lines
	} else if infinispan == nil && jdbc == nil {
		return DeviceRegistryIllegal
	} else if infinispan != nil && jdbc == nil {
		return DeviceRegistryInfinispan
	} else if infinispan == nil && jdbc != nil {
		return DeviceRegistryJdbc
	}

	return DeviceRegistryIllegal
}

//endregion

//region JDBC device registry

func (r JdbcDeviceRegistry) IsSplitRegistry() (bool, error) {
	if r.Server.External != nil {
		return r.Server.External.Management != nil && r.Server.External.Adapter != nil, nil
	} else {
		return false, util.NewConfigurationError("unsupported device registry configuration")
	}
}

//endregion

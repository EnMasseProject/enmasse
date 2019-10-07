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

func (c *IoTConfigSpec) DefaultNativeTlsRequired() bool {
	if c.JavaDefaults.RequireNativeTls != nil {
		return *c.JavaDefaults.RequireNativeTls
	}
	return util.DefaultJavaRequiresNativeTls()
}

func (c *CommonAdapterConfig) IsNativeTlsRequired(config *IoTConfig) bool {
	if c.Java == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	if c.Java.RequireNativeTls == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	return *c.Java.RequireNativeTls
}

func (c *CommonServiceConfig) IsNativeTlsRequired(config *IoTConfig) bool {
	if c.Java == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	if c.Java.RequireNativeTls == nil {
		return config.Spec.DefaultNativeTlsRequired()
	}
	return *c.Java.RequireNativeTls
}

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
		c.SetStatus(corev1.ConditionTrue, "", "")
	}
}

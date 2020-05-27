/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1beta2

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (m *MessagingTenantStatus) GetMessagingTenantCondition(t MessagingTenantConditionType) *MessagingTenantCondition {
	for i, c := range m.Conditions {
		if c.Type == t {
			return &m.Conditions[i]
		}
	}

	mc := MessagingTenantCondition{
		Type:               t,
		Status:             corev1.ConditionUnknown,
		LastTransitionTime: metav1.Now(),
	}

	m.Conditions = append(m.Conditions, mc)

	return &m.Conditions[len(m.Conditions)-1]
}

func (c *MessagingTenantCondition) SetStatus(status corev1.ConditionStatus, reason string, message string) {

	if c.Status != status {
		c.Status = status
		c.LastTransitionTime = metav1.Now()
	}

	c.Reason = reason
	c.Message = message
}

func (t *MessagingTenant) IsBound() bool {
	return t.Status.MessagingInfrastructureRef.Name != "" && t.Status.MessagingInfrastructureRef.Namespace != ""
}

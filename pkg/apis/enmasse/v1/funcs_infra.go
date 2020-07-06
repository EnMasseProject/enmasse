/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	"fmt"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (m *MessagingInfrastructureStatus) GetMessagingInfrastructureCondition(t MessagingInfrastructureConditionType) *MessagingInfrastructureCondition {
	for i, c := range m.Conditions {
		if c.Type == t {
			return &m.Conditions[i]
		}
	}

	mc := MessagingInfrastructureCondition{
		Type:               t,
		Status:             corev1.ConditionUnknown,
		LastTransitionTime: metav1.Now(),
	}

	m.Conditions = append(m.Conditions, mc)

	return &m.Conditions[len(m.Conditions)-1]
}

func (c *MessagingInfrastructureCondition) SetStatus(status corev1.ConditionStatus, reason string, message string) {

	if c.Status != status {
		c.Status = status
		c.LastTransitionTime = metav1.Now()
	}

	c.Reason = reason
	c.Message = message

}

func (c *MessagingInfrastructure) GetRouterInfraName() string {
	return fmt.Sprintf("router-%s", c.Name)
}

func (c *MessagingInfrastructure) GetInternalClusterServiceName() string {
	return fmt.Sprintf("%s-cluster", c.GetRouterInfraName())
}

func (m *MessagingInfrastructure) GetSelector() *NamespaceSelector {
	return m.Spec.NamespaceSelector
}

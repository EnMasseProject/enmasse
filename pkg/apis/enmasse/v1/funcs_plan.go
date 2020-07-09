/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package v1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (m *MessagingPlanStatus) GetMessagingPlanCondition(t MessagingPlanConditionType) *MessagingPlanCondition {
	for i, c := range m.Conditions {
		if c.Type == t {
			return &m.Conditions[i]
		}
	}

	mc := MessagingPlanCondition{
		Type:               t,
		Status:             corev1.ConditionUnknown,
		LastTransitionTime: metav1.Now(),
	}

	m.Conditions = append(m.Conditions, mc)

	return &m.Conditions[len(m.Conditions)-1]
}

func (c *MessagingPlanCondition) SetStatus(status corev1.ConditionStatus, reason string, message string) {

	if c.Status != status {
		c.Status = status
		c.LastTransitionTime = metav1.Now()
	}

	c.Reason = reason
	c.Message = message
}

func (m *MessagingPlan) GetSelector() *NamespaceSelector {
	return m.Spec.NamespaceSelector
}

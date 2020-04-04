/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package common

import (
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"

	corev1 "k8s.io/api/core/v1"
)

func WithConditionUpdate(condition *v1beta2.MessagingInfraCondition, fn func() error) error {
	err := fn()
	if err != nil {
		condition.SetStatus(corev1.ConditionFalse, "", err.Error())
	} else {
		condition.SetStatus(corev1.ConditionTrue, "", "")
	}
	return err
}

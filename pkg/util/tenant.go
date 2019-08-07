/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

func tenantName(namespace string, name string) string {
	return namespace + "." + name
}

func TenantNameForObject(object metav1.Object) string {
	return tenantName(object.GetNamespace(), object.GetName())
}

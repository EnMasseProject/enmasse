/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package util

import (
	"context"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

type ApplySecretFn func(secret *corev1.Secret) error

func CreateSecret(ctx context.Context, client client.Client, scheme *runtime.Scheme, namespace string, secretName string, owner metav1.Object, applyfn ApplySecretFn) error {
	secret := &corev1.Secret{}
	err := client.Get(ctx, types.NamespacedName{Namespace: namespace, Name: secretName}, secret)
	if err != nil && errors.IsNotFound(err) {
		secret.ObjectMeta = metav1.ObjectMeta{Namespace: namespace, Name: secretName}

		err := applyfn(secret)
		if err != nil {
			return err
		}

		if err := controllerutil.SetControllerReference(owner, secret, scheme); err != nil {
			return err
		}
		err = client.Create(ctx, secret)
		if err != nil {
			return err
		}
	}
	return nil
}

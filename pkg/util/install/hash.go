/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// Get the hash of the content of a config map, and apply this to the pod so
// that a change in the content of the config map will trigger a re-rollout.
func ApplyConfigMapHash(c client.Client, pod *corev1.PodTemplateSpec, annotationName string, namespace string, configMapName string, keys ...string) error {

	cm := &corev1.ConfigMap{}

	// fetch secret

	if err := c.Get(context.Background(), client.ObjectKey{Namespace: namespace, Name: configMapName}, cm); err != nil {
		if apierrors.IsNotFound(err) {
			// we could pause reconciling until the config map is provided
			return util.NewConfigurationError("ConfigMap '" + configMapName + "' missing for deployment")
		}
		return err
	}

	// create hash

	rec := cchange.NewRecorder()
	if len(keys) == 0 {
		rec.AddStringsFromMap(cm.Data)
	} else {
		for _, k := range keys {
			if v, ok := cm.Data[k]; ok {
				rec.AddString(v)
			} else {
				return util.NewConfigurationError("Missing key '%s' in data section of ConfigMap %s/%s", k, namespace, configMapName)
			}
		}
	}

	// store hash

	setHashInformation(pod, annotationName, rec)

	// done

	return nil
}

// Get the hash of the content of a secret, and apply this to the pod so
// that a change in the content of the secret will trigger a re-rollout.
func ApplySecretHash(c client.Client, pod *corev1.PodTemplateSpec, annotationName string, namespace string, secretName string, keys ...string) error {

	secret := &corev1.Secret{}

	// fetch secret

	if err := c.Get(context.Background(), client.ObjectKey{Namespace: namespace, Name: secretName}, secret); err != nil {
		if apierrors.IsNotFound(err) {
			// we could pause reconciling until the secret is provided
			return util.NewConfigurationError("Secret '" + secretName + "' missing for deployment")
		}
		return err
	}

	// create hash

	rec := cchange.NewRecorder()
	if len(keys) == 0 {
		rec.AddBytesFromMap(secret.Data)
	} else {
		for _, k := range keys {
			if v, ok := secret.Data[k]; ok {
				rec.AddBytes(v)
			} else {
				return util.NewConfigurationError("Missing key '%s' in data section of Secret %s/%s", k, namespace, secretName)
			}
		}
	}

	// store hash

	setHashInformation(pod, annotationName, rec)

	// done

	return nil

}

func setHashInformation(pod *corev1.PodTemplateSpec, labelName string, rec *cchange.ConfigChangeRecorder) {
	if pod.Annotations == nil {
		pod.Annotations = make(map[string]string)
	}
	pod.Annotations[labelName] = rec.HashString()
}

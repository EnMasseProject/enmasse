/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const nameAuthServicePskSecret = "iot-service-psk"
const keyInterServicePsk = "inter-service-psk"

const keyAdapterUsername = "username"
const keyAdapterPassword = "password"

const minInterServicePskLength = 1024
const minAdapterPasswordLength = 128

func (r *ReconcileIoTConfig) processAdapterPskCredentials(ctx context.Context, config *iotv1alpha1.IoTConfig, configTracker *configTracker) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	// for all adapters

	for _, a := range adapters {

		name := a.FullName() + "-credentials"

		rc.ProcessSimple(func() error {
			return r.processSecret(ctx, name, config, !a.IsEnabled(config), func(config *iotv1alpha1.IoTConfig, secret *corev1.Secret) error {

				if secret.Data == nil {
					secret.Data = make(map[string][]byte, 0)
				}

				// if the auth adapter password does not exist or has the wrong length

				l := len(secret.Data[keyAdapterPassword])
				if l < minAdapterPasswordLength {

					log.Info("Adapter password has wrong length - expected: %d, actual: %d -> generating new one", minAdapterPasswordLength, l)

					// generate a new one

					s, err := util.GeneratePassword(minAdapterPasswordLength)
					if err != nil {
						return errors.Wrap(err, "Failed to generate secure password for adapter")
					}
					secret.Data[keyAdapterPassword] = []byte(s)

				}

				// set password

				secret.Data[keyAdapterUsername] = []byte(a.Name + "-adapter@HONO")

				// record password hash

				configTracker.RecordAdapterPassword(a, secret.Data[keyAdapterUsername], secret.Data[keyAdapterPassword])

				// done

				return nil

			})
		})

	}

	return rc.Result()

}

func (r *ReconcileIoTConfig) processAuthServicePskSecret(ctx context.Context, config *iotv1alpha1.IoTConfig, authServiceConfigCtx *cchange.ConfigChangeRecorder) error {

	return r.processSecret(ctx, nameAuthServicePskSecret, config, false, func(config *iotv1alpha1.IoTConfig, secret *corev1.Secret) error {

		// ensure we have a map

		if secret.Data == nil {
			secret.Data = make(map[string][]byte, 0)
		}

		// if the auth service PSK does not exist or has the wrong length

		l := len(secret.Data[keyInterServicePsk])
		if l < minInterServicePskLength {

			log.Info("Inter service PSK has wrong length - expected: %d, actual: %d -> generating new one", minInterServicePskLength, l)

			// generate a new one
			// Hono uses a string as key, and takes the bytes from that using Java's String#getBytes(...) method.
			// So we cannot use a PSK (based on bytes) but must also use something that we can represent as string
			// especially without null-characters. If we also use the GeneratePassword function here, we are on the
			// safe side regarding the string. But as we exclude a range of characters, we loose some bits in randomness.

			s, err := util.GeneratePassword(minInterServicePskLength)
			if err != nil {
				return errors.Wrap(err, "Failed to generate secure PSK for services")
			}
			secret.Data[keyInterServicePsk] = []byte(s)

		}

		// record for hash

		authServiceConfigCtx.AddBytes(secret.Data[keyInterServicePsk])

		// done

		return nil
	})

}

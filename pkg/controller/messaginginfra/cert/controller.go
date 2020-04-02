/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cert

import (
	"context"
	"fmt"
	"time"

	"crypto/sha256"
	"encoding/hex"

	logr "github.com/go-logr/logr"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

type CertController struct {
	client             client.Client
	scheme             *runtime.Scheme
	caExpirationTime   time.Duration
	certExpirationTime time.Duration
}

func NewCertController(client client.Client, scheme *runtime.Scheme, caExpirationTime time.Duration, certExpirationTime time.Duration) *CertController {
	return &CertController{
		client:             client,
		scheme:             scheme,
		caExpirationTime:   caExpirationTime,
		certExpirationTime: certExpirationTime,
	}
}

const ANNOTATION_CA_DIGEST = "enmasse.io/ca-digest"

/*
 * Reconciles the CA for an instance of shared infrastructure. This function also handles renewal of the CA.
 */
func (c *CertController) ReconcileCa(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra) error {
	secretName := fmt.Sprintf("%s-ca", infra.Name)
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: secretName},
	}

	caCreated := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraCaCreated)
	return common.WithConditionUpdate(caCreated, func() error {
		_, err := controllerutil.CreateOrUpdate(ctx, c.client, secret, func() error {
			if err := controllerutil.SetControllerReference(infra, secret, c.scheme); err != nil {
				return err
			}
			return c.applyCaSecret(secret, logger)
		})
		return err
	})
}

func (c *CertController) applyCaSecret(secret *corev1.Secret, logger logr.Logger) error {
	install.ApplyDefaultLabels(&secret.ObjectMeta, "", secret.Name)
	if secret.ObjectMeta.Annotations == nil {
		secret.ObjectMeta.Annotations = make(map[string]string)
	}

	now := time.Now()
	_, hasCert := secret.Data["tls.key"]

	var err error
	var expiryDate time.Time
	if hasCert {
		expiryDate, err = getCertExpiryDate(secret.Data["tls.crt"])
		if err != nil {
			return err
		}
	}

	logger.Info("Checking CA expiry", "expiryDate", expiryDate.Format(time.UnixDate))

	// If this is a new secret or has expired, generate cert
	if !hasCert {
		logger.Info("Creating CA certificate")
		expiryDate := now.Add(c.caExpirationTime)
		caKey, caCert, err := generateCa(nil, nil, expiryDate)
		if err != nil {
			return err
		}
		secret.Data = make(map[string][]byte)
		secret.Data["tls.key"] = caKey
		secret.Data["tls.crt"] = caCert
	} else if shouldRenewCert(now, expiryDate) {
		logger.Info("Renewing CA certificate")
		expiryDate := now.Add(c.caExpirationTime)
		caKey, caCert, err := generateCa(secret.Data["tls.key"], secret.Data["tls.crt"], expiryDate)
		if err != nil {
			return err
		}
		secret.Data = make(map[string][]byte)
		secret.Data["tls.key"] = caKey
		secret.Data["tls.crt"] = caCert
	}

	return nil
}

/*
 * Reconciles the internal certificate for a given component in a shared infrastructure.
 * This function also handles renewal of the certificate.
 */
func (c *CertController) ReconcileCert(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra, set *appsv1.StatefulSet, dnsNames ...string) (*corev1.Secret, error) {

	caSecretName := fmt.Sprintf("%s-ca", infra.Name)
	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: caSecretName},
	}
	err := c.client.Get(ctx, types.NamespacedName{Namespace: infra.Namespace, Name: caSecretName}, caSecret)
	if err != nil {
		return nil, err
	}

	secretName := GetCertSecretName(set.Name)
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: secretName},
	}
	_, err = controllerutil.CreateOrUpdate(ctx, c.client, secret, func() error {
		if err := controllerutil.SetControllerReference(set, secret, c.scheme); err != nil {
			return err
		}
		return c.applyCertSecret(secret, caSecret, logger, dnsNames...)
	})
	return secret, err
}

func (c *CertController) applyCertSecret(secret *corev1.Secret, caSecret *corev1.Secret, logger logr.Logger, dnsNames ...string) error {

	caExpiryDate, err := getCertExpiryDate(caSecret.Data["tls.crt"])
	if err != nil {
		return err
	}

	install.ApplyDefaultLabels(&secret.ObjectMeta, "", secret.Name)
	if secret.ObjectMeta.Annotations == nil {
		secret.ObjectMeta.Annotations = make(map[string]string)
	}

	// Get the expiry date of the existing certificate if it exists
	var expiryDate time.Time
	_, hasCert := secret.Data["tls.key"]
	if hasCert {
		expiryDate, err = getCertExpiryDate(secret.Data["tls.crt"])
		if err != nil {
			return err
		}
	}

	// Retrieve the SHA-256 digest of our existing CA.
	var expectedCaDigest string
	if value, ok := secret.Annotations[ANNOTATION_CA_DIGEST]; ok {
		expectedCaDigest = value
	}

	actualCaDigest := getDigest(caSecret.Data["tls.crt"])

	logger.Info("Checking cert", "secret", secret.Name, "expiryDate", expiryDate.Format(time.UnixDate), "expectedCaDigest", expectedCaDigest, "actualCaDigest", actualCaDigest)

	now := time.Now()

	// Create the initial certificate if this is a new secret to be created
	if !hasCert {
		logger.Info("Creating component certificate", "secret", secret.Name)
		expiryDate := now.Add(c.certExpirationTime)
		key, cert, keystore, err := generateCert(nil, nil, caSecret.Data["tls.key"], caSecret.Data["tls.crt"], expiryDate, dnsNames)
		if err != nil {
			return err
		}
		secret.Data = make(map[string][]byte)
		secret.Data["tls.key"] = key
		secret.Data["tls.crt"] = cert
		secret.Data["ca.crt"] = caSecret.Data["tls.crt"]
		secret.Data["keystore.p12"] = keystore
		secret.Annotations[ANNOTATION_CA_DIGEST] = actualCaDigest

	} else if shouldRenewCert(now, expiryDate) || expectedCaDigest != actualCaDigest {
		// If we should renew or if CA has changed, renew this certificate

		logger.Info("Renewing component certificate", "secret", secret.Name)
		expiryDate := now.Add(c.certExpirationTime)
		//  Making sure that we don't expire after CA and cap it at the CA expiry date
		if expiryDate.After(caExpiryDate) {
			expiryDate = caExpiryDate
		}
		key, cert, keystore, err := generateCert(secret.Data["tls.key"], secret.Data["tls.crt"], caSecret.Data["tls.key"], caSecret.Data["tls.crt"], expiryDate, dnsNames)
		if err != nil {
			return err
		}
		secret.Data = make(map[string][]byte)
		secret.Data["tls.key"] = key
		secret.Data["tls.crt"] = cert
		secret.Data["ca.crt"] = caSecret.Data["tls.crt"]
		secret.Data["keystore.p12"] = keystore
		secret.Annotations[ANNOTATION_CA_DIGEST] = actualCaDigest
	}

	return nil
}

// Certs should be renewed 1 hour before expiry
func shouldRenewCert(now time.Time, expiryDate time.Time) bool {
	return now.Add(1 * time.Hour).After(expiryDate)
}

func getDigest(data []byte) string {
	sum := sha256.Sum256(data)
	return hex.EncodeToString(sum[:])
}

func GetCertSecretName(name string) string {
	return fmt.Sprintf("%s-cert", name)
}

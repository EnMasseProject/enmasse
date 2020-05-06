/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package cert

import (
	"context"
	"fmt"
	"reflect"
	"time"

	"crypto/sha256"
	"encoding/hex"

	logr "github.com/go-logr/logr"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

type Clock interface {
	Now() time.Time
}

type systemClock struct{}

func (s *systemClock) Now() time.Time {
	return time.Now()
}

type CertController struct {
	client             client.Client
	scheme             *runtime.Scheme
	caExpirationTime   time.Duration
	certExpirationTime time.Duration
	clock              Clock
}

type CertInfo struct {
	NotBefore time.Time
	NotAfter  time.Time
}

func NewCertController(client client.Client, scheme *runtime.Scheme, caExpirationTime time.Duration, certExpirationTime time.Duration) *CertController {
	return &CertController{
		client:             client,
		scheme:             scheme,
		caExpirationTime:   caExpirationTime,
		certExpirationTime: certExpirationTime,
		clock:              &systemClock{},
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

	_, err := controllerutil.CreateOrUpdate(ctx, c.client, secret, func() error {
		if err := controllerutil.SetControllerReference(infra, secret, c.scheme); err != nil {
			return err
		}
		return c.applyCaSecret(secret, logger)
	})
	return err
}

/*
 * Reconciles the CA for an instance of a messaging tenant. This function also handles renewal of the CA.
 */
func (c *CertController) ReconcileTenantCa(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra, namespace string) error {
	secretName := GetTenantCaSecretName(namespace)
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: secretName},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, c.client, secret, func() error {
		return c.applyCaSecret(secret, logger)
	})
	return err
}

func (c *CertController) applyCaSecret(secret *corev1.Secret, logger logr.Logger) error {
	install.ApplyDefaultLabels(&secret.ObjectMeta, "", secret.Name)

	now := c.clock.Now()
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
		caKey, caCert, err := generateCa(now, nil, nil, expiryDate)
		if err != nil {
			return err
		}
		secret.Data = make(map[string][]byte)
		secret.Data["tls.key"] = caKey
		secret.Data["tls.crt"] = caCert
	} else if shouldRenewCert(now, expiryDate) {
		logger.Info("Renewing CA certificate")
		expiryDate := now.Add(c.caExpirationTime)
		caKey, caCert, err := generateCa(now, secret.Data["tls.key"], secret.Data["tls.crt"], expiryDate)
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
 * Reconciles the internal certificate for a given endpoint in a tenant.
 * This function also handles renewal of the certificate.
 */
func (c *CertController) ReconcileEndpointCert(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra, endpoint *v1beta2.MessagingEndpoint) (*CertInfo, error) {

	caSecretName := fmt.Sprintf("%s-tenant-ca", endpoint.Namespace)
	caSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: caSecretName},
	}
	err := c.client.Get(ctx, types.NamespacedName{Namespace: infra.Namespace, Name: caSecretName}, caSecret)
	if err != nil {
		return nil, err
	}

	// Secret already exists and shared, so we do not create it
	secretName := GetTenantSecretName(infra.Name)
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: secretName},
	}
	err = c.client.Get(ctx, types.NamespacedName{Namespace: infra.Namespace, Name: secretName}, secret)
	if err != nil {
		return nil, err
	}

	// Modfy only fields that are specific to this endpoint
	config := certConfig{
		key: fmt.Sprintf("%s.%s.key", endpoint.Namespace, endpoint.Name),
		crt: fmt.Sprintf("%s.%s.crt", endpoint.Namespace, endpoint.Name),
	}
	originalKey := secret.Data[config.key]
	originalCrt := secret.Data[config.crt]
	info, err := c.applyCertSecret(secret, caSecret, config, logger, endpoint.Status.Host, endpoint.Status.Host)
	if err != nil {
		return nil, err
	}

	if !reflect.DeepEqual(originalKey, secret.Data[config.key]) || !reflect.DeepEqual(originalCrt, secret.Data[config.crt]) {
		err = c.client.Update(ctx, secret)
		return info, err
	}
	return info, nil
}

/*
 * Reconciles the internal certificate for a given endpoint in a tenant so that it matches the provided values.
 */
func (c *CertController) ReconcileEndpointCertFromValues(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra, endpoint *v1beta2.MessagingEndpoint, key []byte, value []byte) error {
	// Secret already exists and shared, so we do not create it
	secretName := GetTenantSecretName(infra.Name)
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: secretName},
	}
	err := c.client.Get(ctx, types.NamespacedName{Namespace: infra.Namespace, Name: secretName}, secret)
	if err != nil {
		return err
	}

	// Modfy only fields that are specific to this endpoint
	config := certConfig{
		key: fmt.Sprintf("%s.%s.key", endpoint.Namespace, endpoint.Name),
		crt: fmt.Sprintf("%s.%s.crt", endpoint.Namespace, endpoint.Name),
	}
	if secret.Data == nil {
		secret.Data = make(map[string][]byte, 0)
	}
	originalKey := secret.Data[config.key]
	originalCrt := secret.Data[config.crt]

	secret.Data[config.key] = key
	secret.Data[config.crt] = value

	if !reflect.DeepEqual(originalKey, secret.Data[config.key]) || !reflect.DeepEqual(originalCrt, secret.Data[config.crt]) {
		return c.client.Update(ctx, secret)
	}
	return nil
}

/*
 * Deletes the certificates of a given endpoint from the tenant secret.
 */
func (c *CertController) DeleteEndpointCert(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra, endpoint *v1beta2.MessagingEndpoint) error {
	// Secret already exists and shared, so we do not create it
	secretName := GetTenantSecretName(infra.Name)
	secret := &corev1.Secret{}
	err := c.client.Get(ctx, types.NamespacedName{Namespace: infra.Namespace, Name: secretName}, secret)
	if err != nil {
		return err
	}

	// Modfy only fields that are specific to this endpoint
	config := certConfig{
		key: fmt.Sprintf("%s.%s.key", endpoint.Namespace, endpoint.Name),
		crt: fmt.Sprintf("%s.%s.crt", endpoint.Namespace, endpoint.Name),
	}
	originalKey := secret.Data[config.key]
	originalCrt := secret.Data[config.crt]

	delete(secret.Data, config.key)
	delete(secret.Data, config.crt)

	if !reflect.DeepEqual(originalKey, secret.Data[config.key]) || !reflect.DeepEqual(originalCrt, secret.Data[config.crt]) {
		err = c.client.Update(ctx, secret)
		return err
	}
	return nil
}

/*
 * Reconciles the internal certificate for a given component in a shared infrastructure.
 * This function also handles renewal of the certificate.
 */
func (c *CertController) ReconcileCert(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra, set *appsv1.StatefulSet, commonName string, dnsNames ...string) (*CertInfo, error) {

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
	var certInfo *CertInfo
	_, err = controllerutil.CreateOrUpdate(ctx, c.client, secret, func() error {
		if err := controllerutil.SetControllerReference(set, secret, c.scheme); err != nil {
			return err
		}
		config := certConfig{
			key:        "tls.key",
			crt:        "tls.crt",
			ca:         "ca.crt",
			keystore:   "keystore.p12",
			truststore: "truststore.p12",
		}
		certInfo, err = c.applyCertSecret(secret, caSecret, config, logger, commonName, dnsNames...)
		return err
	})
	return certInfo, err
}

// Get CertInfo for a given PEM cert.
func GetCertInfo(pemCert []byte) (*CertInfo, error) {
	cert, err := parsePemCertificate(pemCert)
	if err != nil {
		return nil, err
	}

	return &CertInfo{
		NotBefore: cert.NotBefore,
		NotAfter: cert.NotAfter,
	}, nil
}

type certConfig struct {
	ca         string
	key        string
	crt        string
	keystore   string
	truststore string
}

func (c *CertController) applyCertSecret(secret *corev1.Secret, caSecret *corev1.Secret, config certConfig, logger logr.Logger, commonName string, dnsNames ...string) (*CertInfo, error) {
	if config.key == "" {
		return nil, fmt.Errorf("key config must be set")
	}

	if config.crt == "" {
		return nil, fmt.Errorf("crt config must be set")
	}
	caExpiryDate, err := getCertExpiryDate(caSecret.Data["tls.crt"])
	if err != nil {
		return nil, err
	}

	install.ApplyDefaultLabels(&secret.ObjectMeta, "", secret.Name)
	if secret.ObjectMeta.Annotations == nil {
		secret.ObjectMeta.Annotations = make(map[string]string)
	}

	// Get the expiry date of the existing certificate if it exists
	var expiryDate time.Time
	_, hasCert := secret.Data[config.key]
	if hasCert {
		expiryDate, err = getCertExpiryDate(secret.Data[config.crt])
		if err != nil {
			return nil, err
		}
	}

	// Retrieve the SHA-256 digest of our existing CA.
	var expectedCaDigest string
	if value, ok := secret.Annotations[ANNOTATION_CA_DIGEST]; ok {
		expectedCaDigest = value
	}

	actualCaDigest := getDigest(caSecret.Data["tls.crt"])

	logger.Info("Checking cert", "secret", secret.Name, "expiryDate", expiryDate.Format(time.UnixDate), "expectedCaDigest", expectedCaDigest, "actualCaDigest", actualCaDigest)

	now := c.clock.Now()
	var certInfo *CertInfo

	// Create the initial certificate if this is a new secret to be created
	if !hasCert {
		logger.Info("Creating component certificate", "secret", secret.Name)
		expiryDate := now.Add(c.certExpirationTime)
		key, cert, keystore, truststore, err := generateCert(now, nil, nil, caSecret.Data["tls.key"], caSecret.Data["tls.crt"], expiryDate, commonName, dnsNames)
		if err != nil {
			return nil, err
		}
		secret.Data = make(map[string][]byte)

		if config.key != "" {
			secret.Data[config.key] = key
		}
		if config.crt != "" {
			secret.Data[config.crt] = cert
		}
		if config.ca != "" {
			secret.Data[config.ca] = caSecret.Data["tls.crt"]
		}
		if config.keystore != "" {
			secret.Data[config.keystore] = keystore
		}
		if config.truststore != "" {
			secret.Data[config.truststore] = truststore
		}

		secret.Annotations[ANNOTATION_CA_DIGEST] = actualCaDigest
		certInfo = &CertInfo{
			NotBefore: now,
			NotAfter:  expiryDate,
		}

	} else if shouldRenewCert(now, expiryDate) || expectedCaDigest != actualCaDigest {
		// If we should renew or if CA has changed, renew this certificate
		if expectedCaDigest != actualCaDigest {
			logger.Info("Renewing component certificate with new CA", "secret", secret.Name)
		} else {
			logger.Info("Renewing expired component certificate", "secret", secret.Name)
		}
		expiryDate := now.Add(c.certExpirationTime)
		//  Making sure that we don't expire after CA and cap it at the CA expiry date
		if expiryDate.After(caExpiryDate) {
			expiryDate = caExpiryDate
		}
		key, cert, keystore, truststore, err := generateCert(now, secret.Data[config.key], secret.Data[config.crt], caSecret.Data["tls.key"], caSecret.Data["tls.crt"], expiryDate, commonName, dnsNames)
		if err != nil {
			return nil, err
		}
		secret.Data = make(map[string][]byte)
		if config.key != "" {
			secret.Data[config.key] = key
		}
		if config.crt != "" {
			secret.Data[config.crt] = cert
		}
		if config.ca != "" {
			secret.Data[config.ca] = caSecret.Data["tls.crt"]
		}
		if config.keystore != "" {
			secret.Data[config.keystore] = keystore
		}
		if config.truststore != "" {
			secret.Data[config.truststore] = truststore
		}
		secret.Annotations[ANNOTATION_CA_DIGEST] = actualCaDigest

		certInfo = &CertInfo{
			NotBefore: now,
			NotAfter:  expiryDate,
		}
	} else {
		certInfo, err = GetCertInfo(secret.Data[config.crt])
		if err != nil {
			return nil, err
		}
	}

	return certInfo, nil
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

func GetTenantCaSecretName(namespace string) string {
	return fmt.Sprintf("%s-tenant-ca", namespace)
}

func GetTenantSecretName(infraName string) string {
	return fmt.Sprintf("%s.tenant-certs", infraName)
}

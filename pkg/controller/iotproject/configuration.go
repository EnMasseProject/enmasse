/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"crypto/x509"
	"encoding/pem"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/pkg/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func (r *ReconcileIoTProject) acceptConfiguration(project *iotv1alpha1.IoTProject) error {

	// set the tenant name in the status section

	project.Status.TenantName = project.TenantName()
	project.Status.Accepted = iotv1alpha1.AcceptedStatus{}

	// prepare variables

	config := &project.Spec.Configuration
	accepted := &project.Status.Accepted.Configuration

	// copy some basics

	accepted.Enabled = config.Enabled
	accepted.MinimumMessageSize = config.MinimumMessageSize

	accepted.Defaults = config.Defaults
	accepted.Extensions = config.Extensions

	// accept tracing

	accepted.Tracing = acceptTracing(config.Tracing)

	// accept adapters

	accepted.Adapters = acceptAdapters(config.Adapters)

	// accept trust anchors

	anchors, err := acceptTrustAnchors(config.TrustAnchors)
	if err != nil {
		return err
	}
	accepted.TrustAnchors = anchors

	// done

	return nil

}

func acceptTrustAnchors(anchors []iotv1alpha1.TrustAnchor) ([]iotv1alpha1.AcceptedTrustAnchor, error) {
	result := make([]iotv1alpha1.AcceptedTrustAnchor, 0)

	for _, t := range anchors {
		at, err := acceptTrustAnchor(t)
		if err != nil {
			return nil, err
		}
		result = append(result, at)
	}

	return result, nil
}

func acceptTrustAnchor(trustAnchor iotv1alpha1.TrustAnchor) (iotv1alpha1.AcceptedTrustAnchor, error) {

	block, _ := pem.Decode([]byte(trustAnchor.Certificate))

	if block == nil {
		return iotv1alpha1.AcceptedTrustAnchor{}, util.NewConfigurationError("Failed to parse PEM certificate")
	}

	cert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return iotv1alpha1.AcceptedTrustAnchor{}, util.WrapAsNonRecoverable(errors.Wrap(err, "Failed to parse tenant certificate"))
	}

	var algorithm string
	switch cert.PublicKeyAlgorithm {
	case x509.RSA:
		algorithm = "RSA"
	case x509.ECDSA:
		algorithm = "EC"
	default:
		return iotv1alpha1.AcceptedTrustAnchor{}, util.NewConfigurationError("Unsupported public key algorithm: %s", cert.PublicKeyAlgorithm)
	}

	publicKey, err := x509.MarshalPKIXPublicKey(cert.PublicKey)
	if err != nil {
		return iotv1alpha1.AcceptedTrustAnchor{}, util.WrapAsNonRecoverable(errors.Wrap(err, "Failed to encode public key into DER format"))
	}

	return iotv1alpha1.AcceptedTrustAnchor{
		SubjectDN: cert.Subject.String(),
		PublicKey: publicKey,
		Algorithm: algorithm,

		Enabled: trustAnchor.Enabled,

		NotAfter:  metav1.NewTime(cert.NotAfter),
		NotBefore: metav1.NewTime(cert.NotBefore),

		AutoProvisioningEnabled: util.FalsePtr(),
	}, nil
}

func acceptAdapters(adapters map[string]iotv1alpha1.AdapterConfiguration) []iotv1alpha1.AcceptedAdapterConfiguration {

	// We basically transform a map[string]Config into an array of Config's with a name property, ensuring uniqueness
	// of the name.

	result := make([]iotv1alpha1.AcceptedAdapterConfiguration, 0)

	for k, a := range adapters {

		aa := iotv1alpha1.AcceptedAdapterConfiguration{
			Type:                 acceptAdapterName(k),
			AdapterConfiguration: a,
		}

		result = append(result, aa)

	}

	return result
}

// Translate our plain adapter names into the corresponding Hono name
func acceptAdapterName(name string) string {
	switch name {
	case "lorawan":
		return "hono-lora"
	default:
		return "hono-" + name
	}
}

func acceptTracing(tracing iotv1alpha1.TracingConfiguration) *iotv1alpha1.AcceptedTracingConfiguration {

	if tracing.SamplingMode != "" ||
		len(tracing.SamplingModePerAuthId) > 0 {

		return &iotv1alpha1.AcceptedTracingConfiguration{
			SamplingMode:          tracing.SamplingMode,
			SamplingModePerAuthId: tracing.SamplingModePerAuthId,
		}

	} else {
		return nil
	}

}

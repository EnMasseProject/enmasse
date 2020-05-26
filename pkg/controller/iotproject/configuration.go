/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"go.uber.org/multierr"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"strings"

	"github.com/google/uuid"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

const labelProjectOwnerNamespace = "iot.enmasse.io/project-owner-namespace"
const labelProjectOwnerName = "iot.enmasse.io/project-owner-name"

var namespaceSubjectDn = uuid.MustParse("dedf9da2-98e5-11ea-b0a4-f875a464d175")

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

	// accept resource limits

	accepted.ResourceLimits = r.acceptResourceLimits(config.ResourceLimits)

	// accept tracing

	accepted.Tracing = r.acceptTracing(config.Tracing)

	// accept adapters

	accepted.Adapters = r.acceptAdapters(config.Adapters)

	// accept trust anchors

	anchors, err := r.acceptTrustAnchors(project, config.TrustAnchors)
	if err != nil {
		return err
	}
	accepted.TrustAnchors = anchors

	// done

	return nil

}

func (r *ReconcileIoTProject) acceptResourceLimits(limits *iotv1alpha1.ResourceLimits) *iotv1alpha1.AcceptedResourceLimits {

	// we have no limits

	if limits == nil {
		return nil
	}

	// fill with defaults

	var maxConnections int64 = -1
	var maxTtl int64 = -1

	// accept overrides

	if limits.MaximumConnections != nil {
		maxConnections = int64(*limits.MaximumConnections)
	}
	if limits.MaximumTimeToLiveSeconds != nil {
		maxTtl = int64(*limits.MaximumTimeToLiveSeconds)
	}

	// return result

	return &iotv1alpha1.AcceptedResourceLimits{
		MaximumConnections:       maxConnections,
		MaximumTimeToLiveSeconds: maxTtl,
	}

}

func (r *ReconcileIoTProject) acceptTrustAnchors(project *iotv1alpha1.IoTProject, anchors []iotv1alpha1.TrustAnchor) ([]iotv1alpha1.AcceptedTrustAnchor, error) {

	var err error
	acceptedTrustAnchors := make(map[string]bool, 0)
	duplicateTrustAnchors := make(map[string]bool, 0)

	// create new trust anchors

	result := make([]iotv1alpha1.AcceptedTrustAnchor, 0)

	// we iterate over all trust anchors and see of we can accept them

	for _, t := range anchors {
		at, acceptErr := r.acceptTrustAnchor(project, t, acceptedTrustAnchors, duplicateTrustAnchors)
		err = multierr.Append(err, acceptErr)
		result = append(result, at)
	}

	// next we merge that with the existing status

	for _, t := range project.Status.Accepted.Configuration.TrustAnchors {
		has := acceptedTrustAnchors[t.SubjectDN]
		if !has {
			// the trust anchor got removed
			unacceptErr := r.unacceptTrustAnchor(project, t.SubjectDN)
			err = multierr.Append(err, unacceptErr)
		}
	}

	// trust anchor condition

	cond := project.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeTrustAnchorsUnique)
	if len(duplicateTrustAnchors) > 0 {
		// go doesn't have '.keys()'
		tas := make([]string, 0, len(duplicateTrustAnchors))
		for k := range duplicateTrustAnchors {
			tas = append(tas, k)
		}
		cond.SetStatusError("DuplicateTrustAnchors", fmt.Sprintf("Configuration has subjectDNs which are already claimed: %s", strings.Join(tas, ", ")))
	} else {
		// go doesn't have '.keys()'
		tas := make([]string, 0, len(acceptedTrustAnchors))
		for k := range duplicateTrustAnchors {
			tas = append(tas, k)
		}
		cond.SetStatusAsBoolean(false, "AsExpected", fmt.Sprintf("All configured trust anchors are claimed by this instance: %s", strings.Join(tas, ", ")))
	}

	// return result

	return result, err
}

// Accept a trust anchor.
//
// Accepting a trust anchor not only involves checking if the trust anchor content is valid, but also if the the
// subject name is unique. This is a requirement of Eclipse Hono. We do this by creating a config map in the
// infrastructure project, for locking name of the subject. If the config map already exists, this is ok for the
// same project, but it is an error for a different project.
//
// This means that we also need to properly clean up locks. When deleting the project, a finalizer will take care
// of deleting all locks. Unfortunately we cannot use owner references for this, as they would need to be cross-namespace,
// which is not supported by Kubernetes. The lock maps reside in the infrastructure namespace, the ensure uniqueness.
//
// When dropping a trust anchor from the configuration, we ensure that we delete if first, before accepting the new
// configuration.
func (r *ReconcileIoTProject) acceptTrustAnchor(project *iotv1alpha1.IoTProject, trustAnchor iotv1alpha1.TrustAnchor, accepted map[string]bool, duplicate map[string]bool) (iotv1alpha1.AcceptedTrustAnchor, error) {

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

	// ensure the subjectDN is unique

	subjectDn := cert.Subject.String()
	if err := r.ensureSubjectDnIsUnique(project, cert); err != nil {
		duplicate[subjectDn] = true
		return iotv1alpha1.AcceptedTrustAnchor{}, err
	}

	// return result

	accepted[subjectDn] = true
	return iotv1alpha1.AcceptedTrustAnchor{
		SubjectDN: subjectDn,
		PublicKey: publicKey,
		Algorithm: algorithm,

		Enabled: trustAnchor.Enabled,

		NotAfter:  metav1.NewTime(cert.NotAfter),
		NotBefore: metav1.NewTime(cert.NotBefore),

		AutoProvisioningEnabled: util.FalsePtr(),
	}, nil
}

func (r *ReconcileIoTProject) ensureSubjectDnIsUnique(project *iotv1alpha1.IoTProject, cert *x509.Certificate) error {

	ctx := context.Background()
	subjectDn := cert.Subject.String()

	cm := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: util.MustGetInfrastructureNamespace(),
			Name:      lockMapForSubjectDn(subjectDn),
		},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, cm, func() error {

		// assign our self as owner, using labels, because we need a cross-namespace reference

		if cm.Labels == nil {
			cm.Labels = make(map[string]string, 0)
		}

		if cm.CreationTimestamp.IsZero() {

			// when creating, we simply set ourselves as owner
			cm.Labels[labelProjectOwnerNamespace] = project.Namespace
			cm.Labels[labelProjectOwnerName] = project.Name

		} else {

			// verify existing owner

			if isLockMapOwnedByProject(cm, project) {
				// same owner, so simply return
				return nil
			} else {
				// different owner, already claimed
				return util.NewConfigurationError("SubjectDN of trust anchor is already claimed by another tenant: %s", cert.Subject.String())
			}

		}

		// done

		return nil
	})

	return err
}

func isLockMapOwnedByProject(cm *corev1.ConfigMap, project *iotv1alpha1.IoTProject) bool {
	ownerNamespace := cm.Labels[labelProjectOwnerNamespace]
	ownerName := cm.Labels[labelProjectOwnerName]
	return project.Namespace == ownerNamespace && project.Name == ownerName
}

// remove the lock of a previously accepted trust anchor
func (r *ReconcileIoTProject) unacceptTrustAnchor(project *iotv1alpha1.IoTProject, subjectDn string) error {

	ctx := context.Background()
	cm := &corev1.ConfigMap{}

	err := r.client.Get(ctx, client.ObjectKey{
		Namespace: util.MustGetInfrastructureNamespace(),
		Name:      lockMapForSubjectDn(subjectDn),
	}, cm)

	if apierrors.IsNotFound(err) {
		// is already gone
		return nil
	}

	if !isLockMapOwnedByProject(cm, project) {
		// owned by someone else, we don't touch it
		return nil
	}

	err = r.client.Delete(ctx, cm, client.Preconditions{
		UID:             &cm.UID,
		ResourceVersion: &cm.ResourceVersion,
	})

	// return

	return err

}

func lockMapForSubjectDn(subjectDn string) string {
	return "iot-trust-anchor-" + nameToId(subjectDn)
}

// return an ID from the name
func nameToId(subjectDn string) string {
	return uuid.NewSHA1(namespaceSubjectDn, []byte(subjectDn)).String()
}

func (r *ReconcileIoTProject) acceptAdapters(adapters map[string]iotv1alpha1.AdapterConfiguration) []iotv1alpha1.AcceptedAdapterConfiguration {

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

func (r *ReconcileIoTProject) acceptTracing(tracing *iotv1alpha1.TracingConfiguration) *iotv1alpha1.AcceptedTracingConfiguration {

	if tracing == nil {
		return nil
	}

	return &iotv1alpha1.AcceptedTracingConfiguration{
		SamplingMode:          tracing.SamplingMode,
		SamplingModePerAuthId: tracing.SamplingModePerAuthId,
	}

}

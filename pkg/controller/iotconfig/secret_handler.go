/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sort"
)

// the struct

type secretHandler struct {
	client     client.Client
	configName string
	namespace  string
	openshift  bool
}

// constructor

func NewSecretHandler(client client.Client, configName string) (*secretHandler, error) {
	namespace, err := util.GetInfrastructureNamespace()
	if err != nil {
		return nil, err
	}

	return &secretHandler{
		client:     client,
		configName: configName,
		namespace:  namespace,
		openshift:  util.IsOpenshift(),
	}, nil
}

func (s *secretHandler) getConfig() (*iotv1alpha1.IoTConfig, error) {

	config := &iotv1alpha1.IoTConfig{}

	// we need to lookup the configuration on every change event
	if err := s.client.Get(context.Background(), client.ObjectKey{Namespace: s.namespace, Name: s.configName}, config); err != nil {
		return nil, err
	} else {
		return config, nil
	}

}

func (s secretHandler) allSecrets(meta v1.Object) []string {

	// prepare result

	result := make([]string, 0)

	// check config

	if meta.GetNamespace() != s.namespace {
		return result
	}

	config, err := s.getConfig()
	if err != nil {
		return result
	}

	// add all inter service certificates

	if config.Spec.InterServiceCertificates != nil {

		if config.Spec.InterServiceCertificates.SecretCertificatesStrategy != nil {
			result = append(result, config.Spec.InterServiceCertificates.SecretCertificatesStrategy.CASecretName)
			for _, v := range config.Spec.InterServiceCertificates.SecretCertificatesStrategy.ServiceSecretNames {
				result = append(result, v)
			}
		}
		if config.Spec.InterServiceCertificates.ServiceCAStrategy != nil {
			result = append(result,
				nameAuthService+"-tls",
				nameDeviceConnection+"-tls",
			)
		}

	}

	// add all endpoint certificates

	for _, a := range adapters {
		if a.IsEnabled(config) {
			result = s.addSecretsFromEndpoint(result, a.AdapterConfigProvider(config), a.FullName())
		}
	}

	// sort before returning

	sort.Strings(result)

	// return result

	return result

}

func (s secretHandler) addSecretsFromEndpoint(result []string, config *iotv1alpha1.CommonAdapterConfig, fullName string) []string {

	if config != nil && config.EndpointConfig != nil && config.EndpointConfig.SecretNameStrategy != nil {
		result = append(result, config.EndpointConfig.SecretNameStrategy.TlsSecretName)
	} else if config != nil && config.EndpointConfig != nil && config.EndpointConfig.KeyCertificateStrategy != nil {
		// do nothing
	} else if s.openshift {
		result = append(result, fullName+"-tls")
	}

	return result
}

func (s secretHandler) common(meta v1.Object, q workqueue.RateLimitingInterface) {
	log.V(2).Info("Check if secret is used by infrastructure", "secretName", meta.GetName())

	secrets := s.allSecrets(meta)
	log.V(2).Info("Secrets used by infrastructure", "allSecrets", secrets)

	if util.ContainsString(secrets, meta.GetName()) {
		log.Info("Secret is being referenced by IoTConfig, trigger IoTConfig reconciliation", "namespace", meta.GetNamespace(), "name", meta.GetName())
		q.Add(reconcile.Request{NamespacedName: types.NamespacedName{
			Namespace: s.namespace,
			Name:      s.configName,
		}})
	}
}

// region handler.EventHandler

// ensure we implement handler.EventHandler

var _ handler.EventHandler = &secretHandler{}

// the implementation of handler.EventHandler

func (s *secretHandler) Create(e event.CreateEvent, q workqueue.RateLimitingInterface) {
	s.common(e.Meta, q)
}

func (s *secretHandler) Update(e event.UpdateEvent, q workqueue.RateLimitingInterface) {
	// we only need to check one of the Meta (Old/New) because the name will not change
	s.common(e.MetaOld, q)
}

func (s *secretHandler) Delete(e event.DeleteEvent, q workqueue.RateLimitingInterface) {
	s.common(e.Meta, q)
}

func (s *secretHandler) Generic(e event.GenericEvent, q workqueue.RateLimitingInterface) {
	s.common(e.Meta, q)
}

// endregion

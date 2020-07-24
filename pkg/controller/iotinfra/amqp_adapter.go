/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"

	"github.com/enmasseproject/enmasse/pkg/util/cchange"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	corev1 "k8s.io/api/core/v1"
)

func (r *ReconcileIoTInfrastructure) processAmqpAdapter(ctx context.Context, iotInfra *iotv1.IoTInfrastructure, msgInfra *enmassev1.MessagingInfrastructure, qdrProxyConfigCtx *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	// find adapter

	adapter := findAdapter("amqp")

	// prepare

	rc := &recon.ReconcileContext{}
	change := qdrProxyConfigCtx.Clone()

	// reconcile

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, adapter.FullName()+"-config", iotInfra, !adapter.IsEnabled(iotInfra), func(infra *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap) error {
			return r.reconcileAmqpAdapterConfigMap(infra, adapter, configMap, change)
		})
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processStandardAdapter(ctx, iotInfra, msgInfra, qdrProxyConfigCtx, adapter)
	})

	// done

	return rc.Result()

}

func (r *ReconcileIoTInfrastructure) reconcileAmqpAdapterConfigMap(infra *iotv1.IoTInfrastructure, a adapter, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	configMap.Data["logback-spring.xml"] = a.RenderLoggingConfig(infra, configMap.Data["logback-custom.xml"])

	configMap.Data["application.yml"] = `
hono:
  app:
    maxInstances: 1
  vertx:
    preferNative: true
  healthCheck:
    insecurePortBindAddress: 0.0.0.0
    insecurePortEnabled: true
    insecurePort: 8088
  amqp:
    bindAddress: 0.0.0.0
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
  registration:
    port: 5671
    trustStoreFormat: PEM
  credentials:
    port: 5671
    trustStoreFormat: PEM
  deviceConnection:
    port: 5671
    trustStoreFormat: PEM
  tenant:
    port: 5671
    trustStoreFormat: PEM
`

	change.AddStringsFromMap(configMap.Data, "application.yml", "logback-spring.xml")

	return nil
}

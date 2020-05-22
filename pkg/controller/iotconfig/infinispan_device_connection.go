/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"encoding/xml"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	netv1 "k8s.io/api/networking/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"strconv"
	"strings"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/api/core/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

const pathEphemeralInfinispanConfigurationParent = "/etc/hono/device-connection"
const pathEphemeralInfinispanConfiguration = pathEphemeralInfinispanConfigurationParent + "/infinispan.xml"

func (r *ReconcileIoTConfig) processInfinispanDeviceConnection(ctx context.Context, config *iotv1alpha1.IoTConfig, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	service := config.Spec.ServicesConfig.DeviceConnection.Infinispan

	rc := &recon.ReconcileContext{}
	change := cchange.NewRecorder()

	ephemeral := service.Server.Ephemeral
	external := service.Server.External
	if external == nil && ephemeral == nil {
		// default to ephemeral
		ephemeral = &iotv1alpha1.EphemeralInfinispanDeviceConnectionServer{}
	}

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceConnection+"-config", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileInfinispanDeviceConnectionConfigMap(config, service, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		// also delete this in "cleanupInfinispanDeviceConnection"
		return r.processConfigMap(ctx, nameDeviceConnection+"-infinispan", config, ephemeral == nil, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileInfinispanDeviceConnectionConfigMapInfinispan(config, service, ephemeral, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceConnection, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileInfinispanDeviceConnectionDeployment(config, deployment, ephemeral, external, change, authServicePsk)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processNetworkPolicy(ctx, nameDeviceConnection, config, ephemeral == nil, r.reconcileInfinispanDeviceConnectionNetworkPolicy)
	})
	rc.ProcessSimple(func() error {
		// also delete this in "cleanupInfinispanDeviceConnection"
		return r.processService(ctx, nameDeviceConnection+"-headless", config, ephemeral == nil, r.reconcileInfinispanDeviceConnectionServiceHeadless)
	})

	return rc.Result()
}

// Cleanup resources which only used when using "infinispan".
// This is called when "processInfinispanDeviceConnection" is not called.
func (r *ReconcileIoTConfig) cleanupInfinispanDeviceConnection(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.Delete(ctx, r.client, &corev1.ConfigMap{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceConnection + "-infinispan"}})
	rc.Delete(ctx, r.client, &corev1.Service{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceConnection + "-headless"}})
	rc.Delete(ctx, r.client, &netv1.NetworkPolicy{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceConnection}})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, ephemeral *iotv1alpha1.EphemeralInfinispanDeviceConnectionServer, external *iotv1alpha1.ExternalInfinispanDeviceConnectionServer, change *cchange.ConfigChangeRecorder, authServicePsk *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[DeviceConnectionTypeAnnotation] = "infinispan"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-connection"
	deployment.Spec.Template.Annotations[DeviceConnectionTypeAnnotation] = "infinispan"

	service := config.Spec.ServicesConfig.DeviceConnection
	applyDefaultDeploymentConfig(deployment, service.Infinispan.ServiceConfig, change)
	cchange.ApplyTo(authServicePsk, "iot.enmasse.io/auth-psk-hash", &deployment.Spec.Template.Annotations)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "device-connection", func(container *corev1.Container) error {

		tracingContainer = container

		// we indeed re-use the device registry image here
		if err := install.SetContainerImage(container, "iot-device-connection-infinispan", config); err != nil {
			return err
		}

		container.Args = []string{"/iot-device-connection-infinispan.jar"}
		container.Command = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(512*1024*1024 /* 512Mi */, resource.BinarySI),
			},
		}

		container.Ports = []corev1.ContainerPort{
			{Name: "amqps", ContainerPort: 5671, Protocol: corev1.ProtocolTCP},
			{Name: "jgroups", ContainerPort: 7800, Protocol: corev1.ProtocolTCP},
		}

		container.Ports = appendHonoStandardPorts(container.Ports)
		SetHonoProbes(container)

		profiles := []string{"device-connection"}
		if external == nil {
			profiles = append(profiles, "embedded-cache")
		}

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: strings.Join(profiles, ",")},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			// required for Hono and Infinispan
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", ValueFrom: install.FromSecret(nameAuthServicePskSecret, keyInterServicePsk)},
		}

		appendCommonHonoJavaEnv(container, "HONO_DEVICECONNECTION_AMQP_", config, &service.Infinispan.CommonServiceConfig)

		SetupTracing(config, deployment, container)
		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)
		if ephemeral != nil {
			install.ApplyVolumeMountSimple(container, "config-infinispan", pathEphemeralInfinispanConfigurationParent, true)
		} else {
			install.DropVolumeMount(container, "config-infinispan")
		}

		// apply container options

		if service.Infinispan != nil {
			applyContainerConfig(container, service.Infinispan.Container.ContainerConfig)
		}

		// apply infinispan server options

		if external != nil {
			if err := appendInfinispanExternalConnectionServer(container, external); err != nil {
				return err
			}
		} else if ephemeral != nil {
			if err := appendInfinispanEphemeralConnectionServer(config, container, ephemeral); err != nil {
				return err
			}
		} else {
			// we should never reach this point anymore, because we fall back to ephemeral
			return util.NewConfigurationError("infinispan backend server configuration missing")
		}

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// reset init containers

	deployment.Spec.Template.Spec.InitContainers = nil

	// tracing

	SetupTracing(config, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config", nameDeviceConnection+"-config")
	if ephemeral != nil {
		install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config-infinispan", nameDeviceConnection+"-infinispan")
	} else {
		install.DropVolume(&deployment.Spec.Template.Spec, "config-infinispan")
	}

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, tlsServiceKeyVolumeName, nameDeviceConnection); err != nil {
		return err
	}

	// return

	return nil
}

func appendInfinispanEphemeralConnectionServer(config *iotv1alpha1.IoTConfig, container *v1.Container, ephemeral *iotv1alpha1.EphemeralInfinispanDeviceConnectionServer) error {

	// config file

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_EMBEDDED_CONFIGURATIONFILE", pathEphemeralInfinispanConfiguration)

	// cache name

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_COMMON_CACHENAME", ephemeral.GetCacheName())

	// append DNS_PING query

	install.AppendEnvVarValue(container, install.JavaOptsEnvVarName, "-Djgroups.dns.query="+nameDeviceConnection+"-headless."+config.Namespace+".svc")

	// done

	return nil

}

func appendInfinispanExternalConnectionServer(container *v1.Container, external *iotv1alpha1.ExternalInfinispanDeviceConnectionServer) error {

	// basic connection

	install.ApplyEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_SERVERLIST", external.Host+":"+strconv.Itoa(int(external.Port)))
	install.ApplyEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHUSERNAME", external.Username)
	install.ApplyEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHPASSWORD", external.Password)

	// SASL

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHSERVERNAME", external.SaslServerName)
	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHREALM", external.SaslRealm)

	// cache names

	deviceStates := ""
	if external.CacheNames != nil {
		deviceStates = external.CacheNames.DeviceConnections
	}

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_COMMON_CACHENAME", deviceStates)

	// done

	return nil
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionConfigMap(config *iotv1alpha1.IoTConfig, service *iotv1alpha1.InfinispanDeviceConnection, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	configMap.Data["logback-spring.xml"] = service.RenderConfiguration(config, logbackDefault, configMap.Data["logback-custom.xml"])

	configMap.Data["application.yml"] = `
hono:

  auth:
    port: 5671
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM

  app:
    maxInstances: 1

  vertx:
    preferNative: true

  healthCheck:
    insecurePortBindAddress: 0.0.0.0
    startupTimeout: 90

  deviceConnection:
    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
`

	change.AddStringsFromMap(configMap.Data, "application.yml", "logback-spring.xml")

	return nil
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionConfigMapInfinispan(_ *iotv1alpha1.IoTConfig, infinispan *iotv1alpha1.InfinispanDeviceConnection, ephemeral *iotv1alpha1.EphemeralInfinispanDeviceConnectionServer, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	// owners

	replicas := uint64(1)
	if infinispan.Replicas != nil {
		replicas = uint64(*infinispan.Replicas)
	}
	owners := uint64(1)
	if ephemeral.Owners > 0 {
		owners = uint64(ephemeral.Owners)
	}

	if owners > replicas {
		return util.NewConfigurationError("Number of key owners (%d) must not exceed number of replicas (%d)", owners, replicas)
	}

	// prepare data section

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	// escape cache name

	sw := strings.Builder{}
	if err := xml.EscapeText(&sw, []byte(ephemeral.GetCacheName())); err != nil {
		return err
	}
	cacheName := sw.String()

	// create XML configuration (don't use tabs, this garbles up the output on the command line)

	ownersStr := strconv.FormatUint(owners, 10)
	conf := `<?xml version="1.0" encoding="UTF-8"?>
<infinispan>
  <jgroups>
    <stack name="enmasse" extends="kubernetes">
      <TCP
        port_range="0"
        stack.combine="COMBINE"
        />
    </stack>
  </jgroups>
  <cache-container default-cache="` + cacheName + `">
    <transport stack="enmasse" initial-cluster-size="` + ownersStr + `" initial-cluster-timeout="30000"/>
    <distributed-cache owners="` + ownersStr + `" name="` + cacheName + `" mode="SYNC"/>
  </cache-container>
</infinispan>`
	configMap.Data["infinispan.xml"] = conf

	// record for config hash

	change.AddStringsFromMap(configMap.Data, "infinispan.xml")

	// done

	return nil
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionServiceHeadless(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	service.Spec.Type = corev1.ServiceTypeClusterIP

	// AMQPS port

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "jgroups",
			Port:       7800,
			TargetPort: intstr.FromString("jgroups"),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameDeviceConnection); err != nil {
		return err
	}

	// for headless service

	service.Spec.Selector["name"] = nameDeviceConnection
	service.Spec.ClusterIP = "None"

	// done

	return nil
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionNetworkPolicy(_ *iotv1alpha1.IoTConfig, policy *netv1.NetworkPolicy) error {

	// prepare

	deviceConnectionMatcher := install.CreateDefaultLabels(nil, "iot", nameDeviceConnection)

	// pod selector for device connection pods

	policy.Spec.PodSelector = metav1.LabelSelector{
		MatchLabels: deviceConnectionMatcher,
	}

	// ingress

	TCP := corev1.ProtocolTCP
	jgroupsPort := intstr.FromString("jgroups")
	servicePort := intstr.FromString("amqps")
	policy.Spec.Ingress = []netv1.NetworkPolicyIngressRule{
		// allow ingress traffic to the jgroups port from any device-connection pod in this namespace
		{
			Ports: []netv1.NetworkPolicyPort{
				{
					Protocol: &TCP,
					Port:     &jgroupsPort,
				},
			},
			From: []netv1.NetworkPolicyPeer{
				{
					PodSelector: &metav1.LabelSelector{
						MatchLabels: deviceConnectionMatcher,
					},
				},
			},
		},
		// allow ingress traffic to the amqps port from any pod in any namespace
		{
			Ports: []netv1.NetworkPolicyPort{
				{
					Protocol: &TCP,
					Port:     &servicePort,
				},
			},
			From: []netv1.NetworkPolicyPeer{
				{
					PodSelector:       &metav1.LabelSelector{MatchLabels: map[string]string{}},
					NamespaceSelector: &metav1.LabelSelector{MatchLabels: map[string]string{}},
				},
			},
		},
	}

	// egress

	policy.Spec.Egress = []netv1.NetworkPolicyEgressRule{
		{}, // allow all egress
	}

	policy.Spec.PolicyTypes = []netv1.PolicyType{
		netv1.PolicyTypeIngress,
		netv1.PolicyTypeEgress,
	}

	// done

	return nil
}

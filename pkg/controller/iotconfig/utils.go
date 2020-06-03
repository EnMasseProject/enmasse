/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"fmt"
	"strconv"
	"strings"

	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/enmasseproject/enmasse/pkg/util/cchange"

	"sigs.k8s.io/controller-runtime/pkg/client"

	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	routev1 "github.com/openshift/api/route/v1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
)

// This sets the default Hono probes
func SetHonoProbes(container *corev1.Container) {

	container.ReadinessProbe = install.ApplyHttpProbe(container.ReadinessProbe, 10, "/readiness", 8088)
	container.LivenessProbe = install.ApplyHttpProbe(container.LivenessProbe, 10, "/liveness", 8088)
	container.LivenessProbe.FailureThreshold = 10

}

func FullHostNameForEnvVar(serviceName string) string {
	return serviceName + ".$(KUBERNETES_NAMESPACE).svc"
}

// block injection of sidecar variables, for containers not using jaeger
func BlockTracingSidecarConfig(config *iotv1alpha1.IoTConfig, container *corev1.Container) {

	if config.Spec.Tracing.Strategy.Sidecar != nil || config.Spec.Tracing.Strategy.DaemonSet != nil {

		install.ApplyEnvSimple(container, "JAEGER_SERVICE_NAME", "")
		install.ApplyEnvSimple(container, "JAEGER_PROPAGATION", "")

	} else {

		install.RemoveEnv(container, "JAEGER_SERVICE_NAME")
		install.RemoveEnv(container, "JAEGER_PROPAGATION")

	}

}

// setup tracing for a container
func SetupTracing(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, container *corev1.Container) {

	if config.Spec.Tracing.Strategy.Sidecar != nil {

		// sidecar

		install.ApplyEnvSimple(container, "JAEGER_SERVICE_NAME", deployment.Name)
		install.ApplyEnvSimple(container, "JAEGER_PROPAGATION", "jaeger,b3")
		install.ApplyEnvSimple(container, "JAEGER_AGENT_HOST", "localhost")

	} else if config.Spec.Tracing.Strategy.DaemonSet != nil {

		// daemon set

		install.ApplyEnvSimple(container, "JAEGER_SERVICE_NAME", deployment.Name)
		install.ApplyEnvSimple(container, "JAEGER_PROPAGATION", "jaeger,b3")
		install.ApplyEnv(container, "JAEGER_AGENT_HOST", func(envvar *corev1.EnvVar) {
			envvar.Value = ""
			envvar.ValueFrom = &corev1.EnvVarSource{
				FieldRef: &corev1.ObjectFieldSelector{
					FieldPath: "status.hostIP",
				},
			}
		})

	} else {

		// disabled

		install.RemoveEnv(container, "JAEGER_AGENT_HOST")
		install.RemoveEnv(container, "JAEGER_SERVICE_NAME")
		install.RemoveEnv(container, "JAEGER_PROPAGATION")

	}

	if config.Spec.Tracing.Strategy.Sidecar != nil {

		if deployment.Annotations["sidecar.jaegertracing.io/inject"] == "" {
			// we only set this to true when unset, because the tracing operator
			// will replace this with the actual tracing instance
			deployment.Annotations["sidecar.jaegertracing.io/inject"] = "true"
		}

	} else {

		delete(deployment.Labels, "sidecar.jaegertracing.io/injected")
		delete(deployment.Annotations, "sidecar.jaegertracing.io/inject")

		for i, c := range deployment.Spec.Template.Spec.Containers {
			if c.Name == "jaeger-agent" {
				log.Info(fmt.Sprintf("Removing jaeger tracing sidecar from deployment: %s", deployment.Name))
				deployment.Spec.Template.Spec.Containers = append(deployment.Spec.Template.Spec.Containers[:i], deployment.Spec.Template.Spec.Containers[i+1:]...)
				break
			}
		}

	}

}

func AppendStandardHonoJavaOptions(container *corev1.Container) {

	install.AppendEnvVarValue(
		container,
		install.JavaOptsEnvVarName,
		"-Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory",
	)

}

func applyDefaultStatefulSetConfig(statefulSet *appsv1.StatefulSet, serviceConfig iotv1alpha1.ServiceConfig, configCtx *cchange.ConfigChangeRecorder) {

	statefulSet.Spec.Replicas = serviceConfig.Replicas
	statefulSet.Spec.UpdateStrategy.Type = appsv1.RollingUpdateStatefulSetStrategyType

	cchange.ApplyTo(configCtx, "iot.enmasse.io/config-hash", &statefulSet.Spec.Template.Annotations)

}

func applyDefaultDeploymentConfig(deployment *appsv1.Deployment, serviceConfig iotv1alpha1.ServiceConfig, configCtx *cchange.ConfigChangeRecorder) {

	deployment.Spec.Replicas = serviceConfig.Replicas
	deployment.Spec.Strategy.Type = appsv1.RollingUpdateDeploymentStrategyType

	cchange.ApplyTo(configCtx, "iot.enmasse.io/config-hash", &deployment.Spec.Template.Annotations)

}

func applyContainerConfig(container *corev1.Container, config iotv1alpha1.ContainerConfig) {

	if config.Resources != nil {
		container.Resources = *config.Resources
	}

}

func (r *ReconcileIoTConfig) cleanupSecrets(ctx context.Context, config *iotv1alpha1.IoTConfig, adapterName string) error {

	// we need to use an unstructured list, as "SecretList" doesn't work
	// due to kubernetes-sigs/controller-runtime#362

	ul := unstructured.UnstructuredList{}
	ul.SetKind("SecretList")
	ul.SetAPIVersion("")

	ls, err := install.LabelSelectorFromMap(install.CreateDefaultLabels(nil, "iot", adapterName+"-tls"))
	if err != nil {
		return err
	}

	n, err := install.BulkRemoveOwner(ctx, r.client, config, true, &ul, client.ListOptions{
		Namespace:     config.GetNamespace(),
		LabelSelector: ls,
	})

	if err == nil {
		log.Info("cleaned up adapter secrets", "adapter", adapterName, "secretsDeleted", n)
	}

	return err
}

func updateEndpointStatus(protocol string, forcePort bool, service *routev1.Route, status *iotv1alpha1.EndpointStatus) {

	status.URI = ""

	if service.Spec.Host == "" {
		return
	}

	status.URI = protocol + "://" + service.Spec.Host

	if forcePort {
		status.URI += ":443"
	}

}

// Append the standard Hono ports
func appendHonoStandardPorts(ports []corev1.ContainerPort) []corev1.ContainerPort {
	if ports == nil {
		ports = make([]corev1.ContainerPort, 0)
	}
	ports = append(ports, corev1.ContainerPort{
		ContainerPort: 8088,
		Name:          "health",
	})
	return ports
}

func (r *ReconcileIoTConfig) reconcileMetricsService(serviceName string) func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {
	return func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {
		return processReconcileMetricsService(config, serviceName, service)
	}
}

// Configure a metrics service for hono standard components.
// Hono exposes metrics on /prometheus on the health endpoint. We create a "<component>-metrics" service and map
// the "prometheus" port from the service to the "health" port of the container. So we can define a "prometheus"
// port on the ServiceMonitor on EnMasse with a custom path of "/prometheus".
func processReconcileMetricsService(_ *iotv1alpha1.IoTConfig, serviceName string, service *corev1.Service) error {

	install.ApplyMetricsServiceDefaults(service, "iot", serviceName)

	service.Spec.Type = corev1.ServiceTypeClusterIP

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "prometheus",
			Port:       8088,
			TargetPort: intstr.FromString("health"),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	return nil
}

func appendCommonHonoJavaEnv(container *corev1.Container, envVarPrefix string, config *iotv1alpha1.IoTConfig, commonJavaService iotv1alpha1.CommonJavaContainerOptions) {

	// add native tls flag

	install.ApplyOrRemoveEnvSimple(container, envVarPrefix+"NATIVE_TLS_REQUIRED", strconv.FormatBool(commonJavaService.IsNativeTlsRequired(config)))

	// configure tls versions

	install.ApplyOrRemoveEnvSimple(container, envVarPrefix+"SECURE_PROTOCOLS", strings.Join(commonJavaService.TlsVersions(config), ","))

}

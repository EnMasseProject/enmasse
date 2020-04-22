/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	routev1 "github.com/openshift/api/route/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const nameDeviceRegistry = "iot-device-registry"
const nameDeviceRegistryAdapter = "iot-device-registry-adapter"
const nameDeviceRegistryManagement = "iot-device-registry-management"
const routeDeviceRegistry = "device-registry"

func isSplitRegistry(config *iotv1alpha1.IoTConfig) (bool, error) {
	switch config.EvalDeviceRegistryImplementation() {
	case iotv1alpha1.DeviceRegistryInfinispan:
		return false, nil
	case iotv1alpha1.DeviceRegistryJdbc:
		return config.Spec.ServicesConfig.DeviceRegistry.JDBC.IsSplitRegistry()
	default:
		return false, util.NewConfigurationError("illegal device registry configuration")
	}
}

func (r *ReconcileIoTConfig) processDeviceRegistry(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	// detect mode of deployment

	splitRegistry, err := isSplitRegistry(config)
	if err != nil {
		return reconcile.Result{}, err
	}

	rc := recon.ReconcileContext{}

	// process

	rc.Process(func() (reconcile.Result, error) {
		switch config.EvalDeviceRegistryImplementation() {
		case iotv1alpha1.DeviceRegistryInfinispan:
			return r.processInfinispanDeviceRegistry(ctx, config)
		case iotv1alpha1.DeviceRegistryJdbc:
			return r.processJdbcDeviceRegistry(ctx, config)
		default:
			return reconcile.Result{}, util.NewConfigurationError("illegal device registry configuration")
		}
	})

	// create services

	if splitRegistry {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistryAdapter, config, false, r.reconcileDeviceRegistryAdapterService)
		})
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistryManagement, config, false, r.reconcileDeviceRegistryManagementService)
		})
		rc.Delete(ctx, r.client, &corev1.Service{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceRegistry}})
	} else {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistry, config, false, r.reconcileDeviceRegistryCombinedService)
		})
		rc.Delete(ctx, r.client, &corev1.Service{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceRegistryAdapter}})
		rc.Delete(ctx, r.client, &corev1.Service{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceRegistryManagement}})
	}

	// create metrics service

	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceRegistry+"-metrics", config, false, r.reconcileMetricsService(nameDeviceRegistry))
	})

	// create routes

	rc.ProcessSimple(func() error {
		return r.processServiceRoute(ctx, config, routeDeviceRegistry, config.Spec.ServicesConfig.DeviceRegistry.Management.Endpoint, r.reconcileDeviceRegistryRoute, r.reconcileDeviceRegistryManagementServiceExternal)
	})

	// done

	return rc.Result()

}

func getRegistryAdapterServiceName(config *iotv1alpha1.IoTConfig) (string, error) {
	is, err := isSplitRegistry(config)
	if err != nil {
		return "", err
	}
	if is {
		return nameDeviceRegistryAdapter, nil
	} else {
		return nameDeviceRegistry, nil
	}
}

func getRegistryManagementServiceName(config *iotv1alpha1.IoTConfig) (string, error) {
	is, err := isSplitRegistry(config)
	if err != nil {
		return "", err
	}
	if is {
		return nameDeviceRegistryManagement, nil
	} else {
		return nameDeviceRegistry, nil
	}
}

func (r *ReconcileIoTConfig) reconcileDeviceRegistryRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "iot", route.Name)

	// Port

	route.Spec.Port = &routev1.RoutePort{
		TargetPort: intstr.FromString("https"),
	}

	// Path

	route.Spec.Path = ""

	// TLS

	if route.Spec.TLS == nil {
		route.Spec.TLS = &routev1.TLSConfig{}
	}

	if config.Spec.ServicesConfig.DeviceRegistry.Management.Endpoint.HasCustomCertificate() {
		route.Spec.TLS.Termination = routev1.TLSTerminationPassthrough
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone
	} else {
		route.Spec.TLS.Termination = routev1.TLSTerminationReencrypt
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone
	}

	// Service

	route.Spec.To.Kind = "Service"
	if name, err := getRegistryManagementServiceName(config); err != nil {
		return err
	} else {
		route.Spec.To.Name = name
	}

	// Update endpoint

	updateEndpointStatus("https", false, route, endpointStatus)

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileDeviceRegistryManagementServiceExternal(_ *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", nameDeviceRegistry)
	service.Spec.Selector[RegistryManagementFeatureLabel] = "true"

	service.Spec.Type = corev1.ServiceTypeLoadBalancer

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "https",
			Port:       31443,
			TargetPort: intstr.FromInt(8443),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileDeviceRegistryCombinedService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", nameDeviceRegistry)
	service.Spec.Selector[RegistryAdapterFeatureLabel] = "true"
	service.Spec.Selector[RegistryManagementFeatureLabel] = "true"

	service.Spec.Type = corev1.ServiceTypeClusterIP

	// AMQPS port

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "amqps",
			Port:       5671,
			TargetPort: intstr.FromInt(5671),
			Protocol:   corev1.ProtocolTCP,
		},
		{
			Name:       "https",
			Port:       8443,
			TargetPort: intstr.FromInt(8443),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameDeviceRegistry); err != nil {
		return err
	}

	if err := applyEndpointService(config.Spec.ServicesConfig.DeviceRegistry.Management.Endpoint, service, nameDeviceRegistry); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileDeviceRegistryAdapterService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", nameDeviceRegistry)
	service.Spec.Selector[RegistryAdapterFeatureLabel] = "true"

	service.Spec.Type = corev1.ServiceTypeClusterIP

	// AMQPS port

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "amqps",
			Port:       5671,
			TargetPort: intstr.FromInt(5671),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameDeviceRegistryAdapter); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileDeviceRegistryManagementService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", nameDeviceRegistry)
	service.Spec.Selector[RegistryManagementFeatureLabel] = "true"

	service.Spec.Type = corev1.ServiceTypeClusterIP

	// HTTP port

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "https",
			Port:       8443,
			TargetPort: intstr.FromInt(8443),
			Protocol:   corev1.ProtocolTCP,
		},
	}

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameDeviceRegistryManagement); err != nil {
		return err
	}

	return nil
}

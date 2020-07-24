/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	routev1 "github.com/openshift/api/route/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const nameDeviceRegistry = "iot-device-registry"
const nameDeviceRegistryManagement = "iot-device-registry-management"
const routeDeviceRegistry = "device-registry"

func isSplitRegistry(infra *iotv1.IoTInfrastructure) (bool, error) {
	switch infra.EvalDeviceRegistryImplementation() {
	case iotv1.DeviceRegistryInfinispan:
		return false, nil
	case iotv1.DeviceRegistryJdbc:
		return infra.Spec.ServicesConfig.DeviceRegistry.JDBC.IsSplitRegistry()
	default:
		return false, util.NewConfigurationError("illegal device registry configuration")
	}
}

func (r *ReconcileIoTInfrastructure) processDeviceRegistry(ctx context.Context, infra *iotv1.IoTInfrastructure, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	// detect mode of deployment

	splitRegistry, err := isSplitRegistry(infra)
	if err != nil {
		return reconcile.Result{}, err
	}

	rc := recon.ReconcileContext{}

	// process

	rc.Process(func() (reconcile.Result, error) {
		switch infra.EvalDeviceRegistryImplementation() {
		case iotv1.DeviceRegistryInfinispan:
			return r.processInfinispanDeviceRegistry(ctx, infra, authServicePsk)
		case iotv1.DeviceRegistryJdbc:
			return r.processJdbcDeviceRegistry(ctx, infra, authServicePsk)
		default:
			return reconcile.Result{}, util.NewConfigurationError("illegal device registry configuration")
		}
	})

	// create services

	if splitRegistry {

		// endpoint - adapter

		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistry, infra, false, r.reconcileDeviceRegistryAdapterService)
		})

		// endpoint - add extra management

		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistryManagement, infra, false, r.reconcileDeviceRegistryManagementService)
		})

		// metrics - add extra management

		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistryManagement+"-metrics", infra, false, r.reconcileMetricsService(nameDeviceRegistryManagement))
		})

	} else {

		// endpoint - combined

		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistry, infra, false, r.reconcileDeviceRegistryCombinedService)
		})

		// endpoint - delete extra management

		rc.Delete(ctx, r.client, &corev1.Service{ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: nameDeviceRegistryManagement}})

		// metrics - delete extra management

		rc.Delete(ctx, r.client, &corev1.Service{ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: nameDeviceRegistryManagement + "-metrics"}})

	}

	// metrics - common service

	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceRegistry+"-metrics", infra, false, r.reconcileMetricsService(nameDeviceRegistry))
	})

	// create routes
	rc.ProcessSimple(func() error {
		return r.processServiceRoute(ctx, infra, routeDeviceRegistry, infra.Spec.ServicesConfig.DeviceRegistry.Management.Endpoint, r.reconcileDeviceRegistryRoute, r.reconcileDeviceRegistryManagementServiceExternal)
	})

	// done

	return rc.Result()

}

func getRegistryManagementServiceName(infra *iotv1.IoTInfrastructure) (string, error) {
	is, err := isSplitRegistry(infra)
	if err != nil {
		return "", err
	}
	if is {
		return nameDeviceRegistryManagement, nil
	} else {
		return nameDeviceRegistry, nil
	}
}

func (r *ReconcileIoTInfrastructure) reconcileDeviceRegistryRoute(infra *iotv1.IoTInfrastructure, route *routev1.Route, endpointStatus *iotv1.EndpointStatus) error {

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

	if infra.Spec.ServicesConfig.DeviceRegistry.Management.Endpoint.HasCustomCertificate() {
		route.Spec.TLS.Termination = routev1.TLSTerminationPassthrough
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone
	} else {
		route.Spec.TLS.Termination = routev1.TLSTerminationReencrypt
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone
	}

	// Service

	route.Spec.To.Kind = "Service"
	if name, err := getRegistryManagementServiceName(infra); err != nil {
		return err
	} else {
		route.Spec.To.Name = name
	}

	// Update endpoint

	updateEndpointStatus("https", false, route, endpointStatus)

	// return

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileDeviceRegistryManagementServiceExternal(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	service.Spec.Type = corev1.ServiceTypeLoadBalancer

	// set a single port, but don't overwrite the NodePort

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "https"
	service.Spec.Ports[0].Port = 31443
	service.Spec.Ports[0].TargetPort = intstr.FromInt(8443)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	// we must point the selector to the management deployment

	name, err := getRegistryManagementServiceName(infra)
	if err != nil {
		return err
	}
	service.Spec.Selector["name"] = name

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileDeviceRegistryCombinedService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

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

	if err := ApplyInterServiceForService(infra, service, nameDeviceRegistry); err != nil {
		return err
	}

	if err := applyEndpointService(infra.Spec.ServicesConfig.DeviceRegistry.Management.Endpoint, service, nameDeviceRegistry); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileDeviceRegistryAdapterService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

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

	if err := ApplyInterServiceForService(infra, service, nameDeviceRegistry); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileDeviceRegistryManagementService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

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

	if err := ApplyInterServiceForService(infra, service, nameDeviceRegistryManagement); err != nil {
		return err
	}

	return nil
}

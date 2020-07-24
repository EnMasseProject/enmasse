/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	"encoding/json"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"github.com/pkg/errors"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"strings"
)

const nameServiceMesh = "iot-service-mesh"
const nameCommandMesh = "iot-command-mesh"
const nameServiceMeshInter = "iot-mesh-inter"
const nameSharedInfra = "shared-infra"

const iotMeshUserName = "inter"
const iotMeshDomainName = "iot"
const iotMeshPasswordLength = 32

const iotCommandMeshUserName = "command"
const iotCommandMeshDomainName = iotMeshDomainName

const nameCommandMeshSecretName = nameServiceMesh + "-command"

func (r *ReconcileIoTInfrastructure) processServiceMesh(ctx context.Context, infra *iotv1.IoTInfrastructure) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}
	change := cchange.NewRecorder()

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameServiceMesh+"-config", infra, false, func(infra *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap) error {
			return r.reconcileServiceMeshConfigMap(infra, configMap, change)
		})
	})
	var commandUserPassword []byte
	rc.ProcessSimple(func() error {
		return r.processSecret(ctx, nameServiceMesh+"-users", infra, false, func(infra *iotv1.IoTInfrastructure, secret *corev1.Secret) error {
			pwd, err := r.reconcileServiceMeshUsersSecret(infra, secret, change)
			commandUserPassword = pwd
			return err
		})
	})
	rc.ProcessSimple(func() error {
		return r.processSecret(ctx, nameCommandMeshSecretName, infra, false, func(infra *iotv1.IoTInfrastructure, secret *corev1.Secret) error {
			return r.reconcileServiceMeshCommandUserSecret(infra, secret, commandUserPassword)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processStatefulSet(ctx, nameServiceMesh, infra, false, func(infra *iotv1.IoTInfrastructure, statefulSet *appsv1.StatefulSet) error {
			return r.reconcileServiceMeshStatefulSet(infra, statefulSet, change)
		})
	})
	rc.ProcessSimple(func() error {
		// the service for the "command internal" mesh
		return r.processService(ctx, nameCommandMesh, infra, false, r.reconcileCommandMeshService)
	})
	rc.ProcessSimple(func() error {
		// the service for the "inter router" mesh
		return r.processService(ctx, nameServiceMeshInter, infra, false, r.reconcileServiceMeshInterService)
	})

	return rc.Result()
}

func (r *ReconcileIoTInfrastructure) reconcileServiceMeshStatefulSet(infra *iotv1.IoTInfrastructure, statefulSet *appsv1.StatefulSet, change *cchange.ConfigChangeRecorder) error {

	install.ApplyStatefulSetDefaults(statefulSet, "iot", statefulSet.Name)

	mesh := infra.Spec.Mesh
	applyDefaultStatefulSetConfig(statefulSet, mesh.ServiceConfig, change)

	err := install.ApplyStatefulSetContainerWithError(statefulSet, "router", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, imageNameRouter, infra); err != nil {
			return err
		}

		container.Args = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		// ports

		container.Ports = []corev1.ContainerPort{
			{Name: "amqps", ContainerPort: 5671, Protocol: corev1.ProtocolTCP},
			{Name: "inter-router", ContainerPort: 55671, Protocol: corev1.ProtocolTCP},
		}

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "APPLICATION_NAME", Value: iotMeshDomainName},

			{Name: "QDROUTERD_CONF", Value: "/etc/infra/qdrouterd.json"},
			{Name: "QDROUTERD_CONF_TYPE", Value: "json"},
			{Name: "QDROUTERD_AUTO_MESH_DISCOVERY", Value: "INFER"},
			{Name: "QDROUTERD_AUTO_MESH_SERVICE_NAME", Value: nameServiceMeshInter},

			{Name: "QDROUTERD_AUTO_MESH_SASL_MECHANISMS", Value: "PLAIN"},
			{Name: "QDROUTERD_AUTO_MESH_SASL_USERNAME", Value: iotMeshUserName + "@" + iotMeshDomainName},
			{Name: "QDROUTERD_AUTO_MESH_SASL_PASSWORD", Value: "file:/etc/users/" + iotMeshUserName},

			{Name: "QDROUTERD_AUTO_CREATE_SASLDB_PATH", Value: "/var/lib/qdrouterd/qdrouterd.sasldb"},
			{Name: "QDROUTERD_AUTO_CREATE_SASLDB_SOURCE", Value: "/etc/users"},
		}

		// health check

		container.ReadinessProbe = install.ApplyHttpProbe(container.ReadinessProbe, 10, "/healthz", 8080)
		container.LivenessProbe = install.ApplyHttpProbe(container.LivenessProbe, 10, "/healthz", 8080)

		// block tracing

		BlockTracingSidecarConfig(infra, container)

		// volume mounts

		container.VolumeMounts = nil
		install.ApplyVolumeMountSimple(container, "runtime", "/var/lib/qdrouterd", false)
		install.ApplyVolumeMountSimple(container, "users", "/etc/users", true)

		install.ApplyVolumeMountSimple(container, "infra", "/etc/infra", true)
		install.ApplyVolumeMountSimple(container, "tls-inter", "/etc/tls-inter", true)
		install.ApplyVolumeMountSimple(container, "tls-command", "/etc/tls-command", true)
		install.ApplyVolumeMountSimple(container, tlsServiceCAVolumeName, "/etc/tls-service-ca", true)

		// apply container options

		applyContainerConfig(container, mesh.ContainerConfig)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// set the name of the service we use for looking up the pods via DNS

	statefulSet.Spec.ServiceName = nameServiceMeshInter

	// reset init containers

	statefulSet.Spec.Template.Spec.InitContainers = nil

	// volumes

	install.ApplyEmptyDirVolume(&statefulSet.Spec.Template.Spec, "runtime")
	install.ApplySecretVolume(&statefulSet.Spec.Template.Spec, "users", nameServiceMesh+"-users")
	install.ApplyConfigMapVolume(&statefulSet.Spec.Template.Spec, "infra", nameServiceMesh+"-config")

	// inter service secrets

	if err := ApplyInterServiceForStatefulSet(r.client, infra, statefulSet, "tls-inter", nameServiceMeshInter); err != nil {
		return err
	}
	if err := ApplyInterServiceForStatefulSet(r.client, infra, statefulSet, "tls-command", nameCommandMesh); err != nil {
		return err
	}

	// return

	return nil
}

// ensure that password is set, and that is has a minimum length
func ensurePassword(data map[string][]byte, name string, minLength int) ([]byte, error) {
	current, ok := data[name]
	if !ok || len(current) < minLength {
		pwd, err := util.GeneratePassword(minLength)
		if err != nil {
			return nil, errors.Wrap(err, "Failed to generate random password")
		}
		p := []byte(pwd)
		data[name] = p
		return p, nil
	} else {
		return current, nil
	}
}

func (r *ReconcileIoTInfrastructure) reconcileServiceMeshUsersSecret(_ *iotv1.IoTInfrastructure, secret *corev1.Secret, change *cchange.ConfigChangeRecorder) ([]byte, error) {

	install.ApplyDefaultLabels(&secret.ObjectMeta, "iot", secret.Name)

	// create secret data

	if secret.Data == nil {
		secret.Data = make(map[string][]byte)
	}

	// set users

	// set users - mesh user

	_, err := ensurePassword(secret.Data, iotMeshUserName, iotMeshPasswordLength)
	if err != nil {
		return nil, err
	}

	// set users - command user

	commandUserPwd, err := ensurePassword(secret.Data, iotCommandMeshUserName, iotMeshPasswordLength)
	if err != nil {
		return nil, err
	}

	// record hash

	change.AddBytesFromMap(secret.Data)

	// done

	return commandUserPwd, nil

}

func (r *ReconcileIoTInfrastructure) reconcileServiceMeshCommandUserSecret(_ *iotv1.IoTInfrastructure, secret *corev1.Secret, commandUserPwd []byte) error {

	install.ApplyDefaultLabels(&secret.ObjectMeta, "iot", secret.Name)

	// create secret data

	secret.Data = map[string][]byte{
		"username": []byte(iotCommandMeshUserName),
		"password": commandUserPwd,
	}

	// done

	return nil

}

func (r *ReconcileIoTInfrastructure) reconcileServiceMeshConfigMap(infra *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	// create infra map data

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	// TLS versions

	tlsVersions := strings.Join(infra.Spec.Mesh.TlsVersions(infra), " ")

	sslProfile := func(name string) []interface{} {
		m := map[string]interface{}{
			"name":           name + "-tls",
			"privateKeyFile": "/etc/tls-" + name + "/tls.key",
			"certFile":       "/etc/tls-" + name + "/tls.crt",
			"caCertFile":     "/etc/tls-service-ca/service-ca.crt",
		}
		if len(tlsVersions) > 0 {
			m["protocols"] = tlsVersions
		}
		return []interface{}{"sslProfile", m}
	}

	// build infra

	router := [][]interface{}{
		{
			"router",
			map[string]interface{}{
				"mode":                    "interior",
				"id":                      "Router.${HOSTNAME}",
				"timestampsInUTC":         true,
				"allowResumableLinkRoute": false,
				"saslConfigDir":           "/etc/sasl2/",
			},
		},
		{
			// for local management access
			"listener",
			map[string]interface{}{
				"host":             "127.0.0.1",
				"port":             5672,
				"role":             "normal",
				"authenticatePeer": "no",
				"saslMechanisms":   "ANONYMOUS",
			},
		},
		{
			// for the health endpoint
			"listener",
			map[string]interface{}{
				"host":             "127.0.0.1",
				"port":             8080,
				"authenticatePeer": false,
				"http":             true,
				"metrics":          true,
				"healthz":          true,
				"websockets":       false,
				"httpRootDir":      "invalid",
			},
		},
		sslProfile("inter"),
		sslProfile("command"),
		{
			// for the internal command mesh
			"listener",
			map[string]interface{}{
				"host":             "0.0.0.0",
				"port":             5671,
				"role":             "normal",
				"authenticatePeer": "yes",
				"saslMechanisms":   "PLAIN",
				"sslProfile":       "command-tls",
				"requireSsl":       true,
			},
		},
		{
			// for the intra-router connections
			"listener",
			map[string]interface{}{
				"host":             "0.0.0.0",
				"port":             55671,
				"role":             "inter-router",
				"authenticatePeer": "yes",
				"saslMechanisms":   "PLAIN",
				"sslProfile":       "inter-tls",
				"requireSsl":       true,
			},
		},
		{
			"policy",
			map[string]interface{}{
				"enableVhostPolicy":       true,
				"enableVhostNamePatterns": true,
				"defaultVhost":            "$default",
			},
		},
		{
			"vhost",
			map[string]interface{}{
				"hostname":         "$default",
				"allowUnknownUser": false,
				"groups": map[string]interface{}{
					"inter": map[string]interface{}{
						"users":                  []string{iotMeshUserName + "@" + iotMeshDomainName},
						"remoteHosts":            "*",
						"sources":                "*",
						"targets":                "*",
						"allowDynamicSource":     true,
						"allowAdminStatusUpdate": true,
					},
					"command": map[string]interface{}{
						"users":         []string{iotCommandMeshUserName + "@" + iotCommandMeshDomainName},
						"remoteHosts":   "*",
						"sourcePattern": "command_internal/#",
						"targetPattern": "command_internal/#",
					},
				},
			},
		},
		{
			"address",
			map[string]interface{}{
				"prefix":       "command_internal",
				"distribution": "balanced",
			},
		},
	}

	// serialize infra

	j, err := json.MarshalIndent(router, "", "  ")
	if err != nil {
		return errors.Wrap(err, "Failed serializing router configuration")
	}
	jstr := string(j)

	configMap.Data = map[string]string{
		"qdrouterd.json": jstr,
	}

	// record for infra hash

	change.AddString(jstr)

	// done

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileCommandMeshService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	// use nameServiceMesh instead of our own name, to create the proper service selector
	install.ApplyServiceDefaults(service, "iot", nameServiceMesh)

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "amqps",
			Protocol:   corev1.ProtocolTCP,
			Port:       5671,
			TargetPort: intstr.FromString("amqps"),
		},
	}

	if err := ApplyInterServiceForService(infra, service, nameCommandMesh); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileServiceMeshInterService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

	// use nameServiceMesh instead of our own name, to create the proper service selector
	install.ApplyServiceDefaults(service, "iot", nameServiceMesh)

	service.Spec.Type = corev1.ServiceTypeClusterIP
	service.Spec.ClusterIP = "None"

	service.Spec.Ports = []corev1.ServicePort{
		{
			Name:       "inter-router",
			Protocol:   corev1.ProtocolTCP,
			Port:       55671,
			TargetPort: intstr.FromString("inter-router"),
		},
	}

	service.Spec.PublishNotReadyAddresses = true

	if err := ApplyInterServiceForService(infra, service, nameServiceMeshInter); err != nil {
		return err
	}

	return nil
}

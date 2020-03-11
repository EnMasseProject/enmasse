/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"encoding/json"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/controller/iotconfig"
	"github.com/enmasseproject/enmasse/pkg/util/ext"
	"github.com/pkg/errors"
	"strconv"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util/altclient"

	"github.com/google/uuid"

	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

const EventReasonProjectTermination = "ProjectTermination"

var (
	tenantCleanupNamespace uuid.UUID = uuid.MustParse("20c0db1c-ffc8-11e9-800c-c85b762e5a2c")
)

func isComplete(job *batchv1.Job) bool {
	return job.Status.CompletionTime != nil
}

func isFailed(job *batchv1.Job) bool {
	if job.Status.Conditions == nil {
		return false
	}
	for _, c := range job.Status.Conditions {
		if c.Type == batchv1.JobFailed {
			return true
		}
	}
	return false
}

// Create or get cleanup job
func createIoTTenantCleanerJob(ctx *finalizer.DeconstructorContext, project *iotv1alpha1.IoTProject, config *iotv1alpha1.IoTConfig) (*batchv1.Job, error) {

	tenantId := project.Status.TenantName + "/" + project.CreationTimestamp.UTC().Format(time.RFC3339)
	id := uuid.NewMD5(tenantCleanupNamespace, []byte(tenantId)).String()
	jobName := "tenant-cleanup-" + id

	job := &batchv1.Job{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: config.Namespace,
			Name:      jobName,
		},
	}

	// create a non-caching client to reduce required permissions
	client := altclient.NewForwarding(ctx.Reader, ctx.Client, ctx.Client)

	res, err := controllerutil.CreateOrUpdate(ctx.Context, client, job, func() error {
		return errors.Wrap(
			reconcileIoTTenantCleanerJob(ctx, job, tenantId, config, project),
			"Failed calling CreateOrUpdate for Job")
	})

	switch res {
	case controllerutil.OperationResultCreated:
		ctx.Recorder.Eventf(project, corev1.EventTypeNormal, EventReasonProjectTermination, "Created tenant cleanup job: %s", jobName)
	}

	return job, err
}

func reconcileIoTTenantCleanerJob(ctx *finalizer.DeconstructorContext, job *batchv1.Job, tenantId string, config *iotv1alpha1.IoTConfig, project *iotv1alpha1.IoTProject) error {

	var extensions []iotv1alpha1.ExtensionImage = nil

	job.Spec.Template.Spec.RestartPolicy = corev1.RestartPolicyOnFailure
	var v int32 = 100
	job.Spec.BackoffLimit = &v

	install.ApplyDefaultLabels(&job.ObjectMeta, "iot", "iot-tenant-cleaner")

	if job.Annotations == nil {
		job.Annotations = make(map[string]string, 0)
	}

	job.Annotations["iot.enmasse.io/tenantId"] = tenantId

	err := install.ApplyJobContainerWithError(job, "cleanup", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-tenant-cleaner", config); err != nil {
			return errors.Wrap(err, "Failed to evaluate container image")
		}

		// reset or init env vars

		container.Env = make([]corev1.EnvVar, 0)

		// apply env vars

		install.ApplyEnvSimple(container, "tenantId", tenantId)

		switch config.EvalDeviceRegistryImplementation() {

		case iotv1alpha1.DeviceRegistryInfinispan:

			if external := config.Spec.ServicesConfig.DeviceRegistry.Infinispan.Server.External; external != nil {

				install.ApplyOrRemoveEnvSimple(container, "registry.type", "infinispan")

				install.ApplyOrRemoveEnvSimple(container, "infinispan.host", external.Host)
				install.ApplyOrRemoveEnvSimple(container, "infinispan.port", strconv.Itoa(int(external.Port)))
				install.ApplyOrRemoveEnvSimple(container, "infinispan.username", external.Username)
				install.ApplyOrRemoveEnvSimple(container, "infinispan.password", external.Password)
				install.ApplyOrRemoveEnvSimple(container, "infinispan.saslServerName", external.SaslServerName)
				install.ApplyOrRemoveEnvSimple(container, "infinispan.saslRealm", external.SaslRealm)
				if external.CacheNames != nil {
					install.ApplyOrRemoveEnvSimple(container, "infinispan.devicesCacheName", external.CacheNames.Devices)
					install.ApplyOrRemoveEnvSimple(container, "infinispan.deviceStatesCacheName", external.CacheNames.DeviceStates)
				}
				if external.DeletionChunkSize > 0 {
					install.ApplyEnvSimple(container, "deletionChunkSize", strconv.FormatUint(uint64(external.DeletionChunkSize), 10))
				}

			} else {

				ctx.Recorder.Event(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Unknown Infinispan configuration")
				return fmt.Errorf("unknown infinispan configuration")

			}

		case iotv1alpha1.DeviceRegistryJdbc:
			if external := config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External; external != nil {

				devices, deviceInformation, err := iotconfig.JdbcConnections(config)
				if err != nil {
					return err
				}

				devicesStr, err := json.Marshal(devices)
				if err != nil {
					return err
				}
				deviceInformationStr, err := json.Marshal(deviceInformation)
				if err != nil {
					return err
				}

				install.ApplyOrRemoveEnvSimple(container, "registry.type", "jdbc")
				install.ApplyEnvSimple(container, "jdbc.devices", string(devicesStr))
				install.ApplyEnvSimple(container, "jdbc.deviceInformation", string(deviceInformationStr))

				// extension containers

				extensions = config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Extensions
				if extensions != nil && len(extensions) > 0 {
					ext.MapExtensionVolume(container)
				}

			} else {

				ctx.Recorder.Event(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Unknown JDBC configuration")
				return fmt.Errorf("unknown jdbc configuration")

			}

		case iotv1alpha1.DeviceRegistryFileBased:
			// nothing to do ... this is already checked by the caller,
			// and we should never get called in this case
			return fmt.Errorf("implementation issue, this should never get called")

		default:
			return fmt.Errorf("unknown device registry configuration")
		}

		// add standard hono options

		iotconfig.AppendStandardHonoJavaOptions(container)

		// done

		return nil
	})

	if err != nil {
		return errors.Wrap(err, "Failed to create job spec")
	}

	// add extension volume

	if extensions != nil {
		if err := ext.AddExtensionContainers(extensions, &job.Spec.Template.Spec); err != nil {
			return errors.Wrap(err, "Failed adding extension containers")
		}
		ext.AddExtensionVolume(&job.Spec.Template.Spec)
	}

	log.Info("Creating job", "jobSpec", job.Spec.Template.Spec)

	// done

	return nil
}

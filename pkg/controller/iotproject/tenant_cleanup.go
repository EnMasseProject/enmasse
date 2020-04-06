/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"encoding/json"
	"github.com/enmasseproject/enmasse/pkg/controller/iotconfig"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/ext"
	"github.com/pkg/errors"
	"reflect"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util/altclient"

	"github.com/google/uuid"

	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

const EventReasonProjectTermination = "ProjectTermination"

var (
	tenantCleanupNamespace = uuid.MustParse("20c0db1c-ffc8-11e9-800c-c85b762e5a2c")
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
//
// The function returns either a newly created, or the currently existing job. Or returns "nil" if no cleanup job is needed.
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
	c := altclient.NewForwarding(ctx.Reader, ctx.Client, ctx.Client)

	key, err := client.ObjectKeyFromObject(job)
	if err != nil {
		return nil, errors.Wrap(err, "Failed to get ObjectKey")
	}

	if err := c.Get(ctx.Context, key, job); err != nil {
		log.Info("Failed to get existing job, might just be 'not found'", "error", err)
		if !apierrors.IsNotFound(err) {
			return nil, errors.Wrap(err, "Failed to get job")
		}
		// not found
		need, err := reconcileIoTTenantCleanerJob(ctx, job, tenantId, config, project)
		if err != nil {
			return nil, errors.Wrap(err, "Failed to reconcile cleanup job")
		}
		log.Info("Need cleanup job?", "need", need)
		if !need {
			// no job, but also not needed
			ctx.Recorder.Eventf(project, corev1.EventTypeNormal, EventReasonProjectTermination, "No special cleanup required")
			return nil, nil
		}
		if err := c.Create(ctx.Context, job); err != nil {
			ctx.Recorder.Eventf(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Failed to create tenant cleanup job: %s", jobName)
			return nil, errors.Wrap(err, "Failed to create cleanup job")
		}

		ctx.Recorder.Eventf(project, corev1.EventTypeNormal, EventReasonProjectTermination, "Created tenant cleanup job: %s", jobName)

		return job, nil
	}

	// job existed

	log.Info("Job already exists")
	existing := job.DeepCopyObject()
	need, err := reconcileIoTTenantCleanerJob(ctx, job, tenantId, config, project)
	if err != nil {
		return nil, errors.Wrap(err, "Failed to reconcile cleanup job")
	}
	if !need {
		// job existed, but is not needed, log and wait for it to complete
		log.Info("Cleanup job not needed, but did already exists. This should not happen.")
		ctx.Recorder.Eventf(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Cleanup job exists, but is not needed: %s", jobName)
		// we still continue here as normal, and wait for the job to terminate
	}
	if reflect.DeepEqual(existing, job) {
		// no change
		return job, nil
	}

	log.Info("Job changed, need to update")

	// something changed, need to update

	if err := c.Update(ctx.Context, job); err != nil {
		ctx.Recorder.Eventf(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Failed to update tenant cleanup job: %s", jobName)
		return job, err
	}

	// done

	ctx.Recorder.Eventf(project, corev1.EventTypeNormal, EventReasonProjectTermination, "Updated tenant cleanup job: %s", jobName)
	return job, nil
}

func applyJsonEnvVar(container *corev1.Container, name string, value interface{}) error {

	valueStr, err := json.Marshal(value)
	if err != nil {
		return err
	}
	install.ApplyEnvSimple(container, name, string(valueStr))

	return nil
}

type infinispanProperties struct {
	iotv1alpha1.ExternalInfinispanServer `json:",inline"`
	CacheNames                           map[string]string `json:"cacheNames"`
}

type cleanerConfig struct {
	TenantId   string               `json:"tenantId"`
	Infinispan infinispanProperties `json:"infinispan"`
}

func tenantCleanerConfigInfinispan(tenantId string, external iotv1alpha1.ExternalInfinispanServer, cacheNames map[string]string) *cleanerConfig {

	return &cleanerConfig{
		TenantId: tenantId,
		Infinispan: infinispanProperties{
			ExternalInfinispanServer: external,
			CacheNames:               cacheNames,
		},
	}

}

func reconcileIoTTenantCleanerJob(ctx *finalizer.DeconstructorContext, job *batchv1.Job, tenantId string, config *iotv1alpha1.IoTConfig, project *iotv1alpha1.IoTProject) (bool, error) {

	var connectionExtensions []iotv1alpha1.ExtensionImage = nil
	var registryExtensions []iotv1alpha1.ExtensionImage = nil

	job.Spec.Template.Spec.RestartPolicy = corev1.RestartPolicyOnFailure
	var v int32 = 100
	job.Spec.BackoffLimit = &v

	install.ApplyDefaultLabels(&job.ObjectMeta, "iot", "iot-tenant-cleaner")
	install.ApplyDefaultLabels(&job.Spec.Template.ObjectMeta, "iot", "iot-tenant-cleaner")

	if job.Annotations == nil {
		job.Annotations = make(map[string]string, 0)
	}

	job.Annotations["iot.enmasse.io/tenantId"] = tenantId

	requireCleaner := false

	err := install.ApplyJobContainerWithError(job, "cleanup", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-tenant-cleaner", config); err != nil {
			return errors.Wrap(err, "Failed to evaluate container image")
		}

		// reset or init env vars

		container.Env = make([]corev1.EnvVar, 0)

		// apply env vars

		install.ApplyEnvSimple(container, "tenantId", tenantId)

		// device connection

		switch config.EvalDeviceConnectionImplementation() {

		case iotv1alpha1.DeviceRegistryInfinispan:

			// we need to set this, although we don't need the cleaner in this case
			install.ApplyOrRemoveEnvSimple(container, "deviceConnection.type", "infinispan")

		case iotv1alpha1.DeviceConnectionJdbc:

			requireCleaner = true

			if external := config.Spec.ServicesConfig.DeviceConnection.JDBC.Server.External; external != nil {

				install.ApplyOrRemoveEnvSimple(container, "deviceConnection.type", "jdbc")

				deviceConnection, err := iotconfig.ExternalJdbcConnectionConnections(config)
				if err != nil {
					return err
				}

				if err := applyJsonEnvVar(container, "jdbc.deviceConnection", deviceConnection); err != nil {
					return err
				}

				// extension containers

				connectionExtensions = config.Spec.ServicesConfig.DeviceConnection.JDBC.Server.External.Extensions

			} else {
				ctx.Recorder.Event(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Unknown JDBC device connection configuration")
				return util.NewConfigurationError("illegal device connection configuration")
			}

		default:
			return util.NewConfigurationError("illegal device connection configuration")

		}

		// device registry

		switch config.EvalDeviceRegistryImplementation() {

		case iotv1alpha1.DeviceRegistryInfinispan:

			requireCleaner = true

			if external := config.Spec.ServicesConfig.DeviceRegistry.Infinispan.Server.External; external != nil {

				install.ApplyOrRemoveEnvSimple(container, "registry.type", "infinispan")

				cacheNames := make(map[string]string)

				if external.CacheNames != nil {
					cacheNames["devices"] = external.CacheNames.Devices
					cacheNames["adapterCredentials"] = external.CacheNames.AdapterCredentials
				}

				deviceConnection := tenantCleanerConfigInfinispan(tenantId, external.ExternalInfinispanServer, cacheNames)

				if err := applyJsonEnvVar(container, "infinispan.registry", deviceConnection); err != nil {
					return err
				}

			} else {

				ctx.Recorder.Event(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Unknown Infinispan device registry configuration")
				return util.NewConfigurationError("illegal infinispan device registry configuration")

			}

		case iotv1alpha1.DeviceRegistryJdbc:

			requireCleaner = true

			if external := config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External; external != nil {

				install.ApplyOrRemoveEnvSimple(container, "registry.type", "jdbc")

				devices, err := iotconfig.ExternalJdbcRegistryConnections(config)
				if err != nil {
					return err
				}

				if err := applyJsonEnvVar(container, "jdbc.devices", devices); err != nil {
					return err
				}

				// extension containers

				registryExtensions = config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Extensions

			} else {

				ctx.Recorder.Event(project, corev1.EventTypeWarning, EventReasonProjectTermination, "Unknown JDBC device registry configuration")
				return util.NewConfigurationError("illegal device registry configuration")

			}

		default:
			return util.NewConfigurationError("illegal device registry configuration")
		}

		// add standard hono options

		iotconfig.AppendStandardHonoJavaOptions(container)

		// map volume

		if len(connectionExtensions) > 0 || len(registryExtensions) > 0 {
			ext.MapExtensionVolume(container)
		}

		// done

		return nil
	})

	if err != nil {
		return true, errors.Wrap(err, "Failed to create job spec")
	}

	if !requireCleaner {
		log.Info("We do not require to clean up the tenant")
		return false, nil
	}

	// add extension volume

	if len(connectionExtensions) > 0 || len(registryExtensions) > 0 {
		if len(connectionExtensions) > 0 {
			if err := ext.AddExtensionContainers(connectionExtensions, &job.Spec.Template.Spec, "ext-con-"); err != nil {
				return true, errors.Wrap(err, "Failed adding extension containers for device connection")
			}
		}
		if len(registryExtensions) > 0 {
			if err := ext.AddExtensionContainers(registryExtensions, &job.Spec.Template.Spec, "ext-reg-"); err != nil {
				return true, errors.Wrap(err, "Failed adding extension containers for device registry")
			}
		}
		ext.AddExtensionVolume(&job.Spec.Template.Spec)
	}

	// done
	log.Info("Creating job", "jobSpec", job.Spec.Template.Spec)
	return true, nil
}

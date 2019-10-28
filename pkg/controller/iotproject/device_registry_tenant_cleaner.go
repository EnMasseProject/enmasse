/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"github.com/enmasseproject/enmasse/pkg/controller/iotconfig"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	batchv1 "k8s.io/api/batch/v1"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)


// maybe use a configmap ?
func createIotTenantCleanerJob(ctx finalizer.DeconstructorContext,  project *iotv1alpha1.IoTProject) batchv1.Job {

	name := project.Name
	registryName := name+project.CreationTimestamp.String()

	client.ObjectKey{
		Namespace: project.Namespace,
		Name:      ,
	}

	client := ctx.Client
	infinispanAddess := client.Get()

	job := batchv1.Job{
		ObjectMeta: metav1.ObjectMeta{Name: "iot-registry-tenant-cleaner"},
		Spec: batchv1.JobSpec{
			Template: v1.PodTemplateSpec{
				Spec: v1.PodSpec{
					Containers: []v1.Container{
						{
							Name: "iot-tenant-cleaner-"+name,
							Image: "imageName",

							Env: []v1.EnvVar{
								{
									Name: "iotProject",
									Value: registryName,
								},
								{
									Name: "host",
									Value: iotconfig.infinspan.address,
								},
								{
									Name: "deletionChuckSize",
									Value: "2000",
								},
								{
									Name: "username",
									Value: "app",
								},
								{
									Name: "password",
									Value: "test12",
								},
								{
									Name: "saslServerName",
									Value: "hotrod",
								},
								{
									Name: "saslRealm",
									Value: "ApplicationRealm",
								},
							},
						},
					},
				},
			},
		},
	}

	return job
}
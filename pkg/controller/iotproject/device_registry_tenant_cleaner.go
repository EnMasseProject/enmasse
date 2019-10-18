/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"github.com/enmasseproject/enmasse/pkg/controller/iotconfig"
	batchv1 "k8s.io/api/batch/v1"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

)


func createIotTenantCleanerJob(name string, iotconfig iotconfig) batchv1.Job {

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
									Value: name,
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
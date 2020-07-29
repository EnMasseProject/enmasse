/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package monitoring

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	custom_metrics "github.com/enmasseproject/enmasse/pkg/custom-metrics"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"time"
)
import logf "sigs.k8s.io/controller-runtime/pkg/log"

var log = logf.Log.WithName("monitoring")

func StartIoTMetrics(mgr manager.Manager) {

	log.Info("Starting IoT metrics")

	cache := mgr.GetCache()
	configInformer, err := cache.GetInformer(&iotv1alpha1.IoTInfrastructure{})
	if err != nil {
		log.Error(err, "Failed to get IoTInfrastructure informer")
	}
	projectInformer, err := cache.GetInformer(&iotv1alpha1.IoTTenant{})
	if err != nil {
		log.Error(err, "Failed to get IoTTenant informer")
	}

	go func() {
		ticker := time.NewTicker(10 * time.Second)
		for ; true; <-ticker.C {

			if configInformer != nil && configInformer.HasSynced() {

				configList := iotv1alpha1.IoTInfrastructureList{}
				if err := cache.List(context.Background(), &configList); err != nil {
					log.Error(err, "Failed to gather IoT Config metrics")
				} else {
					total, active, configuring, terminating, failed := sumIoTConfig(&configList)
					custom_metrics.IoTInfrastructure.Set(total)
					custom_metrics.IoTInfrastructureActive.Set(active)
					custom_metrics.IoTInfrastructureConfiguring.Set(configuring)
					custom_metrics.IoTInfrastructureTerminating.Set(terminating)
					custom_metrics.IoTInfrastructureFailed.Set(failed)
				}

			}

			if projectInformer != nil && projectInformer.HasSynced() {
				projectList := iotv1alpha1.IoTTenantList{}
				if err := cache.List(context.Background(), &projectList); err != nil {
					log.Error(err, "Failed to gather IoT Project metrics")
				} else {
					total, active, configuring, terminating, failed := sumIoTProject(&projectList)
					custom_metrics.IoTTenant.Set(total)
					custom_metrics.IoTTenantActive.Set(active)
					custom_metrics.IoTTenantConfiguring.Set(configuring)
					custom_metrics.IoTTenantTerminating.Set(terminating)
					custom_metrics.IoTTenantFailed.Set(failed)
				}
			}

		}
	}()

}

func sumIoTConfig(list *iotv1alpha1.IoTInfrastructureList) (total float64, active float64, configuring float64, terminating float64, failed float64) {

	for _, c := range list.Items {
		total++
		switch c.Status.Phase {
		case iotv1alpha1.InfrastructurePhaseActive:
			active++
		case iotv1alpha1.InfrastructurePhaseConfiguring:
			configuring++
		case iotv1alpha1.InfrastructurePhaseFailed:
			failed++
		case iotv1alpha1.InfrastructurePhaseTerminating:
			terminating++
		}
	}

	return

}

func sumIoTProject(list *iotv1alpha1.IoTTenantList) (total float64, active float64, configuring float64, terminating float64, failed float64) {

	for _, c := range list.Items {
		total++
		switch c.Status.Phase {
		case iotv1alpha1.TenantPhaseActive:
			active++
		case iotv1alpha1.TenantPhaseConfiguring:
			configuring++
		case iotv1alpha1.TenantPhaseFailed:
			failed++
		case iotv1alpha1.TenantPhaseTerminating:
			terminating++
		}
	}

	return

}

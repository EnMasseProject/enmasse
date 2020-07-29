/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package custom_metrics

import (
	"github.com/prometheus/client_golang/prometheus"
	"sigs.k8s.io/controller-runtime/pkg/metrics"
)

var (

	// region IoTInfrastructure

	IoTInfrastructure = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_infrastructures",
			Help:        "Number of IoT Infrastructures",
			ConstLabels: defaultLabels(),
		},
	)
	IoTInfrastructureActive = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_infrastructures_active",
			Help:        "Number of IoT Infrastructures with status 'active'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTInfrastructureConfiguring = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_infrastructures_configuring",
			Help:        "Number of IoT Infrastructures with status 'configuring'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTInfrastructureTerminating = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_infrastructures_terminating",
			Help:        "Number of IoT Infrastructures with status 'terminating'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTInfrastructureFailed = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_infrastructures_failed",
			Help:        "Number of IoT Infrastructures with status 'failed'",
			ConstLabels: defaultLabels(),
		},
	)

	// endregion

	// region IoTTenant

	IoTTenant = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_tenants",
			Help:        "Number of IoT Tenants",
			ConstLabels: defaultLabels(),
		},
	)
	IoTTenantActive = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_tenants_active",
			Help:        "Number of IoT Tenants with status 'active'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTTenantConfiguring = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_tenants_configuring",
			Help:        "Number of IoT Tenants with status 'configuring'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTTenantTerminating = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_tenants_terminating",
			Help:        "Number of IoT Tenants with status 'terminating'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTTenantFailed = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_tenants_failed",
			Help:        "Number of IoT Tenants with status 'failed'",
			ConstLabels: defaultLabels(),
		},
	)

	// endregion

)

func init() {
	// Register custom metrics with the global prometheus registry
	metrics.Registry.MustRegister(

		IoTInfrastructure,
		IoTInfrastructureActive,
		IoTInfrastructureConfiguring,
		IoTInfrastructureTerminating,
		IoTInfrastructureFailed,

		IoTTenant,
		IoTTenantActive,
		IoTTenantConfiguring,
		IoTTenantTerminating,
		IoTTenantFailed,
	)
}

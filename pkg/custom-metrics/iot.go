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

	// region IoTConfig

	IoTConfig = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_configs",
			Help:        "Number of IoT Configs",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigActive = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_configs_active",
			Help:        "Number of IoT Configs with status 'active'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigConfiguring = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_configs_configuring",
			Help:        "Number of IoT Configs with status 'configuring'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigTerminating = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_configs_terminating",
			Help:        "Number of IoT Configs with status 'terminating'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigFailed = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "iot_configs_failed",
			Help:        "Number of IoT Configs with status 'failed'",
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

		IoTConfig,
		IoTConfigActive,
		IoTConfigConfiguring,
		IoTConfigTerminating,
		IoTConfigFailed,

		IoTTenant,
		IoTTenantActive,
		IoTTenantConfiguring,
		IoTTenantTerminating,
		IoTTenantFailed,
	)
}

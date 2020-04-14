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

	//region IoTConfig

	IoTConfig = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_configs",
			Help:        "Number of IoT Configs",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigActive = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_configs_active",
			Help:        "Number of IoT Configs with status 'active'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigConfiguring = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_configs_configuring",
			Help:        "Number of IoT Configs with status 'configuring'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigTerminating = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_configs_terminating",
			Help:        "Number of IoT Configs with status 'terminating'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTConfigFailed = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_configs_failed",
			Help:        "Number of IoT Configs with status 'failed'",
			ConstLabels: defaultLabels(),
		},
	)

	//endregion

	//region IoTProject

	IoTProject = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_projects",
			Help:        "Number of IoT Projects",
			ConstLabels: defaultLabels(),
		},
	)
	IoTProjectActive = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_projects_active",
			Help:        "Number of IoT Projects with status 'active'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTProjectConfiguring = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_projects_configuring",
			Help:        "Number of IoT Projects with status 'configuring'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTProjectTerminating = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_projects_terminating",
			Help:        "Number of IoT Projects with status 'terminating'",
			ConstLabels: defaultLabels(),
		},
	)
	IoTProjectFailed = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "enmasse_iot_projects_failed",
			Help:        "Number of IoT Projects with status 'failed'",
			ConstLabels: defaultLabels(),
		},
	)

	//endregion

)

func init() {
	// Register custom metrics with the global prometheus registry
	metrics.Registry.MustRegister(

		IoTConfig,
		IoTConfigActive,
		IoTConfigConfiguring,
		IoTConfigTerminating,
		IoTConfigFailed,

		IoTProject,
		IoTProjectActive,
		IoTProjectConfiguring,
		IoTProjectTerminating,
		IoTProjectFailed,
	)
}

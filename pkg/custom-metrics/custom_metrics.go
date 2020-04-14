/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package custom_metrics

import (
	"github.com/enmasseproject/enmasse/version"
	"github.com/prometheus/client_golang/prometheus"
	"os"
	customMetrics "sigs.k8s.io/controller-runtime/pkg/metrics"
)

// Custom metrics
var (
	versionInfo = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name:        "version",
			Help:        "Enmasse Operator version and product version",
			ConstLabels: defaultLabels(),
		},
	)
)

func defaultLabels() prometheus.Labels {
	return prometheus.Labels{"name": "enmasse-operator", "operator_version": version.Version, "version": os.Getenv("VERSION")}
}

func init() {
	// Register custom metrics with the global prometheus registry
	customMetrics.Registry.MustRegister(versionInfo)
}

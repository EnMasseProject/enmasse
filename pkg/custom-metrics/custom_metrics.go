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
			ConstLabels: prometheus.Labels{"name": "enmasse-operator", "operator_version": version.Version, "version": os.Getenv("VERSION")},
		},
	)
)

func Init() {
	// Register custom metrics with the global prometheus registry
	customMetrics.Registry.MustRegister(versionInfo)
}

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package server

import "github.com/prometheus/client_golang/prometheus"

func CreateMetrics() (*prometheus.HistogramVec, *prometheus.CounterVec, prometheus.Gauge) {
	queryTimeMetric := prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "console_query_time_seconds",
		Help:    "The query time in seconds",
		Buckets: prometheus.DefBuckets,
	}, []string{"operationName"})
	queryErrorCountMetric := prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "console_query_error_total",
		Help: "Number of queries that have ended in error",
	}, []string{"operationName"})
	sessionCountMetric := prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "console_active_sessions",
		Help: "Number of active HTTP sessions",
	})
	return queryTimeMetric, queryErrorCountMetric, sessionCountMetric
}


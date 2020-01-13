/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
)

func (r *Resolver) Metric_consoleapi_enmasse_io_v1beta1() Metric_consoleapi_enmasse_io_v1beta1Resolver {
	return &metricK8sResolver{r}
}

type metricK8sResolver struct{ *Resolver }

func (m metricK8sResolver) Type(ctx context.Context, obj *consolegraphql.Metric) (MetricType, error) {
	if obj != nil {
		t := obj.Type
		switch t {
		case "gauge":
			return MetricTypeGauge, nil
		case "counter":
			return MetricTypeCounter, nil
		case "rate":
			return MetricTypeRate, nil
		default:
			panic(fmt.Sprintf("unrecognized metric type : %s for object %+v", t, obj))
		}
	}
	return MetricTypeGauge, nil
}

func (m metricK8sResolver) Units(ctx context.Context, obj *consolegraphql.Metric) (string, error) {
	return obj.Unit, nil
}

func (m metricK8sResolver) Value(ctx context.Context, obj *consolegraphql.Metric) (float64, error) {
	return  obj.Value, nil
}
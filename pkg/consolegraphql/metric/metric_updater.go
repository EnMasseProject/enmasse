/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package metric

import (
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
)

func UpdateAllMetrics(objectCache cache.Cache) (error, int) {

	objs, err := objectCache.Get(cache.PrimaryObjectIndex, "", func(o interface{}) (bool, bool, error) {
		if _, ok := o.(consolegraphql.HasMetrics); ok {
			return true, true, nil
		} else {
			return false, true, nil
		}
	})
	if err != nil {
		return err, 0
	}

	calc := New()
	updatedRecords := 0
	for i := range objs {
		objectWithMetrics := objs[i].(consolegraphql.HasMetrics)
		metrics := objectWithMetrics.GetMetrics()
		updated := make(map[string]*consolegraphql.Metric, 0)
		for _, m := range metrics {
			series := m.GetTimeSeries()
			if series != nil {
				currentValue := m.Value
				newValue, err := calc.Calc(series)
				if err != nil {
					return err, 0
				}
				if currentValue != newValue {
					m.Value = newValue
					updated[m.Name] = m
				}
			}
		}
		if len(updated) > 0 {
			objectCache.Update(func(current interface{}) (interface{}, error) {
				metrics := objectWithMetrics.GetMetrics()
				for _, m := range metrics {
					if update, present := updated[m.Name]; present {
						m.Value = update.Value
					}
				}
				updatedRecords++
				return current, nil
			}, objectWithMetrics)
		}
	}
	return nil, updatedRecords
}
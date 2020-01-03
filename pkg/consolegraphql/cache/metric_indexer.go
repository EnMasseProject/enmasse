/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package cache

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
)

type MetricKeyCreator = func(*consolegraphql.Metric) (bool, []byte, error)

type metricIndex struct {
	IndexFunc MetricKeyCreator
}

func MetricIndex() (*metricIndex) {
	return &metricIndex{
		IndexFunc: func(m *consolegraphql.Metric) (bool, []byte, error) {
			key := fmt.Sprintf("%s/%s/%s/%s/%s/\x00", m.Kind, m.Namespace, m.AddressSpace, m.Name, m.Value.GetName())
			return true, []byte(key), nil
		},
	}
}

func ConnectionLinkMetricIndex()  (*metricIndex) {
	return &metricIndex{
		IndexFunc: func(m *consolegraphql.Metric) (bool, []byte, error) {
			if m.ConnectionName == nil {
				return false, nil, nil
			}
			key := fmt.Sprintf("%s/%s/%s/%s/%s/%s/\x00", m.Kind, m.Namespace, m.AddressSpace, *m.ConnectionName, m.Name, m.Value.GetName())
			return true, []byte(key), nil
		},
	}
}

func (m *metricIndex) FromObject(obj interface{}) (bool, []byte, error) {
	metric, ok := obj.(*consolegraphql.Metric)
	if !ok {
		return false, nil, fmt.Errorf("unexpected type: %T", obj)
	}

	b, val, err := m.IndexFunc(metric)
	if err != nil || !b{
		return b, nil, err
	}
	return true, val, nil
}

func (m *metricIndex) FromArgs(args ...interface{}) ([]byte, error) {
	if len(args) != 1 {
		return nil, fmt.Errorf("must provide only a single argument")
	}
	arg, ok := args[0].(string)
	if !ok {
		return nil, fmt.Errorf("argument must be a string: %#v", args[0])
	}
	// Add the null character as a terminator
	arg += "\x00"
	return []byte(arg), nil
}

func (s *metricIndex) PrefixFromArgs(args ...interface{}) ([]byte, error) {
	if len(args) != 1 {
		return nil, fmt.Errorf("must provide only a single argument")
	}
	arg, ok := args[0].(string)
	if !ok {
		return nil, fmt.Errorf("argument must be a string: %#v", args[0])
	}
	return []byte(arg), nil
}


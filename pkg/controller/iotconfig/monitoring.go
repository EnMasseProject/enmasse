/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	promv1 "github.com/coreos/prometheus-operator/pkg/apis/monitoring/v1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/monitoring"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"k8s.io/apimachinery/pkg/api/meta"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"time"
)

type MonitoringTarget struct {
	Name string
}

var defaultMonitoringTargets = []MonitoringTarget{
	{Name: nameAuthService},
	{Name: nameTenantService},
	{Name: nameDeviceConnection},
}

func (r *ReconcileIoTConfig) processMonitoring(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.Process(func() (reconcile.Result, error) {
		err := r.processPrometheusRule(ctx, "enmasse-iot", config, !monitoring.IsEnabled(), r.reconcilePrometheusRule)

		switch err.(type) {
		case *meta.NoKindMatchError:
			// Prometheus CRDs missing, mark as non-recoverable and re-try in 5 minutes
			return reconcile.Result{RequeueAfter: 5 * time.Minute}, util.WrapAsNonRecoverable(err)
		}

		return reconcile.Result{}, err
	})

	return rc.Result()

}

func allTargets(config *iotv1alpha1.IoTConfig) []MonitoringTarget {

	// start with an empty set

	targets := make([]MonitoringTarget, 0)

	// add default targets

	targets = append(targets, defaultMonitoringTargets...)

	// device registry

	if split, err := isSplitRegistry(config); err == nil {
		// we can ignore the error here, as it will be handled by the reconcile loop anyway
		if split {
			targets = append(targets, []MonitoringTarget{
				{nameDeviceRegistry},
				{nameDeviceRegistryManagement},
			}...)
		} else {
			targets = append(targets, MonitoringTarget{
				Name: nameDeviceRegistry,
			})
		}
	}

	// add adapters

	for _, a := range adapters {
		if !a.IsEnabled(config) {
			continue
		}
		targets = append(targets, MonitoringTarget{
			Name: a.FullName(),
		})
	}

	// return result

	return targets
}

func (r *ReconcileIoTConfig) reconcilePrometheusRule(config *iotv1alpha1.IoTConfig, rule *promv1.PrometheusRule) error {

	install.ApplyDefaultLabels(&rule.ObjectMeta, "iot", rule.Name)
	rule.Labels["monitoring-key"] = "middleware"
	rule.Labels["role"] = "alert-rules"

	// add all extra labels

	for k, v := range config.Spec.Monitoring.Labels {
		rule.Labels[k] = v
	}

	// prepare targets

	targets := allTargets(config)

	// prepare IoT component health group

	componentHealth := promv1.RuleGroup{
		Name:  "IoTComponentHealth",
		Rules: []promv1.Rule{},
	}

	projectHealth := promv1.RuleGroup{
		Name:  "IoTProjectHealth",
		Rules: []promv1.Rule{},
	}

	// add detection of deployments

	for _, t := range targets {
		serviceName := t.Name + "-metrics"
		componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
			Record: "enmasse_component_health",
			Expr:   intstr.FromString(`up{job="` + serviceName + `"} or on(namespace) (1- absent(up{job="` + serviceName + `"}) )`),
		})
	}

	// add check for IoTConfigs in status failed

	componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
		Alert: "IoTConfigHealth",
		Expr:  intstr.FromString("enmasse_iot_configs_failed > 0"),
		For:   "30s",
		Annotations: map[string]string{
			"description": "IoTConfigs have been in a failed state for over 30 seconds",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// add check for IoTConfigs in status configuring

	componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
		Alert: "IoTConfigHealth",
		Expr:  intstr.FromString("enmasse_iot_configs_configuring > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTConfigs have been in a configuring state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// add check for IoTConfigs in status terminating

	componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
		Alert: "IoTConfigHealth",
		Expr:  intstr.FromString("enmasse_iot_configs_terminating > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTConfigs have been in a terminating state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// add check for IoTTenants in status failed

	projectHealth.Rules = append(projectHealth.Rules, promv1.Rule{
		Alert: "IoTProjectHealth",
		Expr:  intstr.FromString("enmasse_iot_tenants_failed > 0"),
		For:   "30s",
		Annotations: map[string]string{
			"description": "IoTTenants that have been in a failed state for over 30 seconds",
			"value":       "{{ $value }}",
			"severity":    "warning",
		},
	})

	// add check for IoTTenants in status configuring

	projectHealth.Rules = append(projectHealth.Rules, promv1.Rule{
		Alert: "IoTProjectHealth",
		Expr:  intstr.FromString("enmasse_iot_tenants_configuring > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTTenants that have been in a configuring state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "warning",
		},
	})

	// add check for IoTTenants in status terminating

	projectHealth.Rules = append(projectHealth.Rules, promv1.Rule{
		Alert: "IoTProjectHealth",
		Expr:  intstr.FromString("enmasse_iot_tenants_terminating > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTTenants that have been in a terminating state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// set groups

	rule.Spec.Groups = []promv1.RuleGroup{componentHealth, projectHealth}

	// done

	return nil
}

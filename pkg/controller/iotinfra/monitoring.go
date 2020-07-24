/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	promv1 "github.com/coreos/prometheus-operator/pkg/apis/monitoring/v1"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
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

func (r *ReconcileIoTInfrastructure) processMonitoring(ctx context.Context, infra *iotv1.IoTInfrastructure) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.Process(func() (reconcile.Result, error) {
		err := r.processPrometheusRule(ctx, "enmasse-iot", infra, !monitoring.IsEnabled(), r.reconcilePrometheusRule)

		switch err.(type) {
		case *meta.NoKindMatchError:
			// Prometheus CRDs missing, mark as non-recoverable and re-try in 5 minutes
			return reconcile.Result{RequeueAfter: 5 * time.Minute}, util.WrapAsNonRecoverable(err)
		}

		return reconcile.Result{}, err
	})

	return rc.Result()

}

func allTargets(infra *iotv1.IoTInfrastructure) []MonitoringTarget {

	// start with an empty set

	targets := make([]MonitoringTarget, 0)

	// add default targets

	targets = append(targets, defaultMonitoringTargets...)

	// device registry

	if split, err := isSplitRegistry(infra); err == nil {
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
		if !a.IsEnabled(infra) {
			continue
		}
		targets = append(targets, MonitoringTarget{
			Name: a.FullName(),
		})
	}

	// return result

	return targets
}

func (r *ReconcileIoTInfrastructure) reconcilePrometheusRule(infra *iotv1.IoTInfrastructure, rule *promv1.PrometheusRule) error {

	install.ApplyDefaultLabels(&rule.ObjectMeta, "iot", rule.Name)
	rule.Labels["monitoring-key"] = "middleware"
	rule.Labels["role"] = "alert-rules"

	// add all extra labels

	for k, v := range infra.Spec.Monitoring.Labels {
		rule.Labels[k] = v
	}

	// prepare targets

	targets := allTargets(infra)

	// prepare IoT component health group

	componentHealth := promv1.RuleGroup{
		Name:  "IoTComponentHealth",
		Rules: []promv1.Rule{},
	}

	tenantHealth := promv1.RuleGroup{
		Name:  "IoTTenantHealth",
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

	// add check for IoTInfrastructures in status failed

	componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
		Alert: "IoTInfrastructureHealth",
		Expr:  intstr.FromString("enmasse_iot_infrastructures_failed > 0"),
		For:   "30s",
		Annotations: map[string]string{
			"description": "IoTInfrastructures that have been in a failed state for over 30 seconds",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// add check for IoTInfrastructures in status configuring

	componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
		Alert: "IoTInfrastructureHealth",
		Expr:  intstr.FromString("enmasse_iot_infrastructures_configuring > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTInfrastructures that have been in a configuring state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// add check for IoTInfrastructures in status terminating

	componentHealth.Rules = append(componentHealth.Rules, promv1.Rule{
		Alert: "IoTInfrastructureHealth",
		Expr:  intstr.FromString("enmasse_iot_infrastructures_terminating > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTInfrastructures that have been in a terminating state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// add check for IoTTenants in status failed

	tenantHealth.Rules = append(tenantHealth.Rules, promv1.Rule{
		Alert: "IoTTenantHealth",
		Expr:  intstr.FromString("enmasse_iot_tenants_failed > 0"),
		For:   "30s",
		Annotations: map[string]string{
			"description": "IoTTenants that have been in a failed state for over 30 seconds",
			"value":       "{{ $value }}",
			"severity":    "warning",
		},
	})

	// add check for IoTTenants in status configuring

	tenantHealth.Rules = append(tenantHealth.Rules, promv1.Rule{
		Alert: "IoTTenantHealth",
		Expr:  intstr.FromString("enmasse_iot_tenants_configuring > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTTenants that have been in a configuring state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "warning",
		},
	})

	// add check for IoTTenants in status terminating

	tenantHealth.Rules = append(tenantHealth.Rules, promv1.Rule{
		Alert: "IoTTenantHealth",
		Expr:  intstr.FromString("enmasse_iot_tenants_terminating > 0"),
		For:   "5m",
		Annotations: map[string]string{
			"description": "IoTTenants that have been in a terminating state for over 5 minutes",
			"value":       "{{ $value }}",
			"severity":    "critical",
		},
	})

	// set groups

	rule.Spec.Groups = []promv1.RuleGroup{componentHealth, tenantHealth}

	// done

	return nil
}

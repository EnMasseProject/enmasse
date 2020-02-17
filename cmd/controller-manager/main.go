/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"context"
	"flag"
	"fmt"
	"time"

	"os"
	"runtime"

	"github.com/enmasseproject/enmasse/pkg/util"

	// Import all Kubernetes client auth plugins (e.g. Azure, GCP, OIDC, etc.)
	"github.com/enmasseproject/enmasse/pkg/cache"
	"github.com/enmasseproject/enmasse/pkg/controller"
	"github.com/enmasseproject/enmasse/version"
	"github.com/operator-framework/operator-sdk/pkg/k8sutil"
	kubemetrics "github.com/operator-framework/operator-sdk/pkg/kube-metrics"
	"github.com/operator-framework/operator-sdk/pkg/leader"
	"github.com/operator-framework/operator-sdk/pkg/log/zap"
	"github.com/operator-framework/operator-sdk/pkg/metrics"
	sdkVersion "github.com/operator-framework/operator-sdk/version"
	"github.com/spf13/pflag"
	v1 "k8s.io/api/core/v1"
	kerrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/util/intstr"
	_ "k8s.io/client-go/plugin/pkg/client/auth"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/config"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/manager/signals"

	monitoringv1 "github.com/coreos/prometheus-operator/pkg/apis/monitoring/v1"
	"github.com/openshift/api"

	enmassescheme "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/scheme"

	"k8s.io/apimachinery/pkg/runtime/schema"

	customMetrics "github.com/enmasseproject/enmasse/pkg/custom-metrics"
)

// Change below variables to serve metrics on different host or port.
var (
	metricsHost               = "0.0.0.0"
	metricsPort         int32 = 8383
	operatorMetricsPort int32 = 8686
)
var log = logf.Log.WithName("cmd")

func printVersion() {
	log.Info(fmt.Sprintf("Operator Version: %s", version.Version))
	log.Info(fmt.Sprintf("Go Version: %s", runtime.Version()))
	log.Info(fmt.Sprintf("Go OS/Arch: %s/%s", runtime.GOOS, runtime.GOARCH))
	log.Info(fmt.Sprintf("Version of operator-sdk: %v", sdkVersion.Version))
}

func main() {
	// Add the zap logger flag set to the CLI. The flag set must
	// be added before calling pflag.Parse().
	pflag.CommandLine.AddFlagSet(zap.FlagSet())
	// Add flags registered by imported packages (e.g. glog and
	// controller-runtime)
	pflag.CommandLine.AddGoFlagSet(flag.CommandLine)
	pflag.Parse()
	// Use a zap logr.Logger implementation. If none of the zap
	// flags are configured (or if the zap flag set is not being
	// used), this defaults to a production zap logger.
	//
	// The logger instantiated here can be changed to any logger
	// implementing the logr.Logger interface. This logger will
	// be propagated through the whole operator, generating
	// uniform and structured logs.
	logf.SetLogger(zap.Logger())

	printVersion()

	namespace := os.Getenv("NAMESPACE")
	log.Info("Watching on namespace", "namespace", namespace)

	// Get a config to talk to the apiserver
	cfg, err := config.GetConfig()
	if err != nil {
		log.Error(err, "Failed to get configuration")
		os.Exit(1)
	}

	ctx := context.TODO()
	// Become the leader before proceeding
	err = leader.Become(ctx, "enmasse-lock")
	if err != nil {
		log.Error(err, "")
		os.Exit(1)
	}

	// Register custom metrics
	customMetrics.Init()

	// Install monitoring resources
	monitoringEnabled := os.Getenv("ENABLE_MONITORING")

	if monitoringEnabled == "true" {
		// Attempt to install the monitoring resources when the operator starts, and every 5 minutes thereafter
		go func() {
			ticker := time.NewTicker(5 * time.Minute)
			for ; true; <-ticker.C {
				serverClient, err := client.New(cfg, client.Options{})
				if err != nil {
					log.Info(fmt.Sprintf("Failed to install monitoring resources: %s", err))
				} else {
					err = installMonitoring(ctx, serverClient)

					if err != nil {
						log.Info(fmt.Sprintf("Failed to install monitoring resources: %s", err))
					} else {
						log.Info("Successfully installed monitoring resources")
						ticker.Stop()
					}
				}
			}
		}()
	}

	// Create a new Cmd to provide shared dependencies and start components
	mgr, err := manager.New(cfg, manager.Options{
		Namespace:          namespace,
		MetricsBindAddress: fmt.Sprintf("%s:%d", metricsHost, metricsPort),
		NewCache: cache.NewDelegateCacheBuilder(namespace,
			schema.GroupVersionKind{
				Group:   "user.enmasse.io",
				Version: "v1beta1",
				Kind:    "MessagingUser",
			},
			schema.GroupVersionKind{
				Group:   "user.enmasse.io",
				Version: "v1beta1",
				Kind:    "MessagingUserList",
			},
			schema.GroupVersionKind{
				Group:   "enmasse.io",
				Version: "v1beta1",
				Kind:    "AddressSpace",
			},
			schema.GroupVersionKind{
				Group:   "enmasse.io",
				Version: "v1beta1",
				Kind:    "AddressSpaceList",
			},
			schema.GroupVersionKind{
				Group:   "enmasse.io",
				Version: "v1beta1",
				Kind:    "Address",
			},
			schema.GroupVersionKind{
				Group:   "enmasse.io",
				Version: "v1beta1",
				Kind:    "AddressList",
			},
			schema.GroupVersionKind{
				Group:   "iot.enmasse.io",
				Version: "v1alpha1",
				Kind:    "IoTProject",
			},
			schema.GroupVersionKind{
				Group:   "iot.enmasse.io",
				Version: "v1alpha1",
				Kind:    "IoTProjectList",
			},
		),
	})
	if err != nil {
		log.Error(err, "Failed to create manager")
		os.Exit(1)
	}

	log.Info("Registering components...")

	// register APIs
	if err := api.Install(mgr.GetScheme()); err != nil {
		log.Error(err, "Failed to register OpenShift schema")
		os.Exit(1)
	}

	if err := enmassescheme.AddToScheme(mgr.GetScheme()); err != nil {
		log.Error(err, "Failed to register EnMasse schema")
		os.Exit(1)
	}

	if err := controller.CheckUpgrade(mgr); err != nil {
		log.Error(err, "Failed to upgrade")
		os.Exit(1)
	}

	// register controller
	if err := controller.AddToManager(mgr); err != nil {
		log.Error(err, "Failed to register controller")
		os.Exit(1)
	}

	// Add the Metrics Service
	addMetrics(ctx, cfg, namespace)

	log.Info("Starting the operator")

	// Start the Cmd
	if err := mgr.Start(signals.SetupSignalHandler()); err != nil {
		log.Error(err, "manager exited non-zero")
		os.Exit(1)
	}
}

// the Prometheus operator
func addMetrics(ctx context.Context, cfg *rest.Config, namespace string) {
	if err := serveCRMetrics(cfg); err != nil {
		log.Info("Could not generate and serve custom resource metrics", "error", err.Error())
	}
	// Add to the below struct any other metrics ports you want to expose.
	servicePorts := []v1.ServicePort{
		{Port: metricsPort, Name: metrics.OperatorPortName, Protocol: v1.ProtocolTCP, TargetPort: intstr.IntOrString{Type: intstr.Int, IntVal: metricsPort}},
		{Port: operatorMetricsPort, Name: metrics.CRPortName, Protocol: v1.ProtocolTCP, TargetPort: intstr.IntOrString{Type: intstr.Int, IntVal: operatorMetricsPort}},
	}
	// Create Service object to expose the metrics port(s).
	service, err := metrics.CreateMetricsService(ctx, cfg, servicePorts)
	if err != nil {
		log.Info("Could not create metrics Service", "error", err.Error())
	}

	// Adding the monitoring-key:middleware to the operator service which will get propagated to the serviceMonitor
	err = addMonitoringKeyLabelToOperatorService(ctx, cfg, service)
	if err != nil {
		log.Error(err, "Could not add monitoring-key label to operator metrics Service")
	}

	// CreateServiceMonitors will automatically create the prometheus-operator ServiceMonitor resources
	// necessary to configure Prometheus to scrape metrics from this operator.
	services := []*v1.Service{service}
	_, err = metrics.CreateServiceMonitors(cfg, namespace, services, func(monitor *monitoringv1.ServiceMonitor) error {
		for i, _ := range monitor.Spec.Endpoints {
			monitor.Spec.Endpoints[i].MetricRelabelConfigs = []*monitoringv1.RelabelConfig{
				&monitoringv1.RelabelConfig{
					SourceLabels: []string{
						"__name__",
					},
					TargetLabel: "__name__",
					Replacement: "enmasse_${1}",
				},
			}
		}
		return nil
	})
	if err != nil {
		log.Info("Could not create ServiceMonitor object", "error", err.Error())
		// If this operator is deployed to a cluster without the prometheus-operator running, it will return
		// ErrServiceMonitorNotPresent, which can be used to safely skip ServiceMonitor creation.
		if err == metrics.ErrServiceMonitorNotPresent {
			log.Info("Install prometheus-operator in your cluster to create ServiceMonitor objects", "error", err.Error())
		}
	}
}

// serveCRMetrics gets the Operator/CustomResource GVKs and generates metrics based on those types.
// It serves those metrics on "http://metricsHost:operatorMetricsPort".
func serveCRMetrics(cfg *rest.Config) error {
	// Below function returns filtered operator/CustomResource specific GVKs.
	// For more control override the below GVK list with your own custom logic.
	filteredGVK, err := k8sutil.GetGVKsFromAddToScheme(enmassescheme.AddToScheme)
	if err != nil {
		return err
	}

	// Get the namespace the operator is currently deployed in.
	operatorNs, err := k8sutil.GetOperatorNamespace()
	if err != nil {
		return err
	}
	// To generate metrics in other namespaces, add the values below.
	ns := []string{operatorNs}
	// Generate and serve custom resource specific metrics.
	err = kubemetrics.GenerateAndServeCRMetrics(cfg, ns, filteredGVK, metricsHost, operatorMetricsPort)
	if err != nil {
		return err
	}

	return nil
}

func addMonitoringKeyLabelToOperatorService(ctx context.Context, cfg *rest.Config, service *v1.Service) error {
	if service == nil {
		return fmt.Errorf("service doesn't exist")
	}

	kclient, err := client.New(cfg, client.Options{})
	if err != nil {
		return err
	}

	updatedLabels := map[string]string{"monitoring-key": "middleware"}
	for k, v := range service.ObjectMeta.Labels {
		updatedLabels[k] = v
	}
	service.ObjectMeta.Labels = updatedLabels

	err = kclient.Update(ctx, service)
	if err != nil {
		return err
	}

	return nil
}

func installMonitoring(ctx context.Context, client client.Client) error {
	log.Info("Installing monitoring resources")
	params := map[string]string{"Namespace": os.Getenv("NAMESPACE")}

	templateHelper := util.NewTemplateHelper(params)

	for _, template := range templateHelper.TemplateList {
		resource, err := templateHelper.CreateResource(template)
		if err != nil {
			return fmt.Errorf("createResource failed: %s", err)
		}
		err = client.Create(ctx, resource)
		if err != nil {
			if !kerrors.IsAlreadyExists(err) {
				return fmt.Errorf("error creating resource: %s", err)
			}
		}
	}

	return nil
}

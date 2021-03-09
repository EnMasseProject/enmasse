/*
 * Copyright 2021, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package ca_bundle

import (
	"context"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"

	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
	controllertypes "k8s.io/apimachinery/pkg/types"
)

var log = logf.Log.WithName("ca_bundle")

const ServiceCaPathOcp311 = "/var/run/secrets/kubernetes.io/serviceaccount"
const ServiceCaPathOcp4 = "/var/run/secrets/enmasse.io/"
const ServiceCaFilename = "service-ca.crt"
const CaBundleConfigmapName = "ca-bundle"
const CaBundleVolumeName = "ca-bundle"
const CaBundleKey = "service-ca.crt"

var _ reconcile.Reconciler = &ReconcileCaBundle{}

type ReconcileCaBundle struct {
	client    client.Client
	reader    client.Reader
	scheme    *runtime.Scheme
	namespace string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	reconciler, err := newReconciler(mgr)
	if err != nil {
		return err
	}
	return add(mgr, reconciler)
}

func newReconciler(mgr manager.Manager) (*ReconcileCaBundle, error) {
	return &ReconcileCaBundle{
		client:    mgr.GetClient(),
		reader:    mgr.GetAPIReader(),
		scheme:    mgr.GetScheme(),
		namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra"),
	}, nil
}

func add(mgr manager.Manager, r *ReconcileCaBundle) error {
	cm := &corev1.ConfigMap{}
	err := r.reader.Get(context.TODO(), controllertypes.NamespacedName{Namespace: r.namespace, Name: CaBundleConfigmapName}, cm)
	if err != nil {
		if k8errors.IsNotFound(err) {
			cm := &corev1.ConfigMap{
				ObjectMeta: metav1.ObjectMeta{Namespace: r.namespace, Name: CaBundleConfigmapName},
			}
			err = ApplyConfigMap(cm, CaBundleConfigmapName)
			if err != nil {
				return err
			}
			err = r.client.Create(context.TODO(), cm)
			if err != nil {
				return err
			}
		} else {
			return err
		}
	}

	// Start reconciler for ca-bundle deployment
	c, err := controller.New("ca-bundle", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	return c.Watch(&source.Kind{Type: &corev1.ConfigMap{}}, &handler.EnqueueRequestForObject{})
}

func (r *ReconcileCaBundle) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	expectedName := controllertypes.NamespacedName{
		Namespace: r.namespace,
		Name:      CaBundleConfigmapName,
	}

	if expectedName == request.NamespacedName {
		reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
		reqLogger.Info("Reconciling ca-bundle")

		ctx := context.TODO()

		_, err := r.reconcileCabundleConfigMap(ctx)
		if err != nil {
			reqLogger.Error(err, "Error reconciling ca-bundle configmap")
			return reconcile.Result{}, err
		}
	}
	return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
}

func (r *ReconcileCaBundle) reconcileCabundleConfigMap(ctx context.Context) (reconcile.Result, error) {

	name := CaBundleConfigmapName
	configMap := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{Namespace: r.namespace, Name: name},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, configMap, func() error {
		return ApplyConfigMap(configMap, name)
	})

	if err != nil {
		log.Error(err, "Failed reconciling cabundle configmap")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

func ApplyConfigMap(configMap *corev1.ConfigMap, name string) error {
	configMap.SetLabels(install.CreateDefaultLabels(configMap.GetLabels(), "operator", name))
	install.ApplyAnnotation(&configMap.ObjectMeta, "service.beta.openshift.io/inject-cabundle", "true")
	return nil
}


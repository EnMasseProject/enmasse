/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package upgrader

import (
	"context"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/predicate"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_authenticationservice_upgrader")

func Add(mgr manager.Manager) error {
	return add(mgr, newUpgrader(mgr))
}

func newUpgrader(mgr manager.Manager) *UpgradeAuthenticationService {
	return &UpgradeAuthenticationService{client: mgr.GetClient(), scheme: mgr.GetScheme(), namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")}
}

func add(mgr manager.Manager, r *UpgradeAuthenticationService) error {

	// Create a new controller
	c, err := controller.New("authenticationservice-upgrade-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for existing unowned keycloak deployment
	isUnownedKeycloakDeployment := func(meta v1.Object) bool {
		return meta.GetName() == "keycloak" &&
			(len(meta.GetOwnerReferences()) == 0 || meta.GetOwnerReferences()[0].Controller == nil || *(meta.GetOwnerReferences()[0].Controller) == false)
	}

	err = c.Watch(&source.Kind{Type: &appsv1.Deployment{}},
		&handler.EnqueueRequestsFromMapFunc{
			ToRequests: handler.ToRequestsFunc(func(a handler.MapObject) []reconcile.Request {
				return []reconcile.Request{
					{
						NamespacedName: types.NamespacedName{Namespace: r.namespace, Name: "authservice"},
					},
				}
			}),
		}, predicate.Funcs{
			CreateFunc: func(e event.CreateEvent) bool {
				return isUnownedKeycloakDeployment(e.Meta)
			},
			UpdateFunc: func(e event.UpdateEvent) bool {
				return isUnownedKeycloakDeployment(e.MetaNew)
			},
			DeleteFunc: func(e event.DeleteEvent) bool {
				return false
			},
			GenericFunc: func(e event.GenericEvent) bool {
				return isUnownedKeycloakDeployment(e.Meta)
			},
		})
	if err != nil {
		return err
	}

	return nil
}

func (r *UpgradeAuthenticationService) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("UpgradingAuthenticationService")

	ctx := context.TODO()

	list := &adminv1beta1.AuthenticationServiceList{}
	opts := &client.ListOptions{}
	err := r.client.List(ctx, opts, list)
	if err != nil {
		return reconcile.Result{}, err
	}

	if len(list.Items) > 0 {
		log.Info("Authentication services CR defined already")
		return reconcile.Result{}, nil
	}

	err = tryUpgradeExistingStandardAuthService(ctx, r)

	return reconcile.Result{}, err
}

var _ reconcile.Reconciler = &UpgradeAuthenticationService{}

type UpgradeAuthenticationService struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client    client.Client
	scheme    *runtime.Scheme
	namespace string
}

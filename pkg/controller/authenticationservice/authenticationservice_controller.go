/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package authenticationservice

import (
	"context"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	routev1 "github.com/openshift/api/route/v1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"reflect"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_authenticationservice")

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileAuthenticationService {
	return &ReconcileAuthenticationService{client: mgr.GetClient(), scheme: mgr.GetScheme(), namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")}
}

func add(mgr manager.Manager, r *ReconcileAuthenticationService) error {

	// Create a new controller
	c, err := controller.New("authenticationservice-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource AuthenticationService
	err = c.Watch(&source.Kind{Type: &adminv1beta1.AuthenticationService{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &appsv1.Deployment{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &adminv1beta1.AuthenticationService{},
	})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &corev1.Service{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &adminv1beta1.AuthenticationService{},
	})
	if err != nil {
		return err
	}

	return nil
}

var _ reconcile.Reconciler = &ReconcileAuthenticationService{}

type ReconcileAuthenticationService struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client    client.Client
	scheme    *runtime.Scheme
	namespace string
}

func updateAuthenticationServiceStatus(authservice *adminv1beta1.AuthenticationService) {
	if authservice.Spec.Type == adminv1beta1.External {

	} else if authservice.Spec.Type == adminv1beta1.None {
		var cert = authservice.Spec.None.CertificateSecret
		if cert == nil {
			cert = &corev1.SecretReference{
				Name: "none-authservice-cert",
			}
		}
		authservice.Status = adminv1beta1.AuthenticationServiceStatus{
			Host:         authservice.Name,
			Port:         5671,
			CaCertSecret: cert,
		}
	} else if authservice.Spec.Type == adminv1beta1.Standard {
		var cert = authservice.Spec.Standard.CertificateSecret
		if cert == nil {
			cert = &corev1.SecretReference{
				Name: "standard-authservice-cert",
			}
		}
		authservice.Status = adminv1beta1.AuthenticationServiceStatus{
			Host:         authservice.Name,
			Port:         5671,
			CaCertSecret: cert,
		}
	}

}

// Reconcile by reading the authentication service spec and making required changes
//
// returning an error will get the request re-queued
func (r *ReconcileAuthenticationService) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling AuthenticationService")

	ctx := context.TODO()
	authservice := &adminv1beta1.AuthenticationService{}
	err := r.client.Get(ctx, request.NamespacedName, authservice)
	if err != nil {
		if k8errors.IsNotFound(err) {
			reqLogger.Info("AuthenticationService resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		// Error reading the object - requeue the request
		reqLogger.Error(err, "Failed to get AuthenticationService")
		return reconcile.Result{}, err
	}

	if authservice.Spec.Type == adminv1beta1.None {
		return r.reconcileNoneAuthService(ctx, authservice)
	} else if authservice.Spec.Type == adminv1beta1.Standard {
		return r.reconcileStandardAuthService(ctx, authservice)
	} else if authservice.Spec.Type == adminv1beta1.External {
		return r.reconcileExternalAuthService(ctx, authservice)
	}
	return reconcile.Result{}, nil
}

func (r *ReconcileAuthenticationService) reconcileNoneAuthService(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {

	err := applyNoneAuthServiceDefaults(ctx, r.client, r.scheme, authservice)
	if err != nil {
		return reconcile.Result{}, err
	}

	var requeue = false
	result, err := r.reconcileDeployment(ctx, authservice, authservice.Name, applyNoneAuthServiceDeployment)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.reconcileService(ctx, authservice, authservice.Name, applyNoneAuthServiceService)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.updateStatus(ctx, authservice, func(status *adminv1beta1.AuthenticationServiceStatus) error {
		status.Host = authservice.Name
		status.Port = 5671
		status.CaCertSecret = authservice.Spec.None.CertificateSecret
		return nil
	})

	if err != nil {
		return reconcile.Result{}, err
	}

	return reconcile.Result{Requeue: requeue}, nil
}

func (r *ReconcileAuthenticationService) reconcileStandardAuthService(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {

	err := applyStandardAuthServiceDefaults(ctx, r.client, r.scheme, authservice)
	if err != nil {
		return reconcile.Result{}, err
	}

	var requeue = false
	result, err := r.reconcileService(ctx, authservice, *authservice.Spec.Standard.ServiceName, applyStandardAuthServiceService)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.reconcileStandardVolume(ctx, *authservice.Spec.Standard.Storage.ClaimName, authservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.reconcileStandardRoute(ctx, *authservice.Spec.Standard.RouteName, authservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.reconcileDeployment(ctx, authservice, *authservice.Spec.Standard.DeploymentName, applyStandardAuthServiceDeployment)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.updateStatus(ctx, authservice, func(status *adminv1beta1.AuthenticationServiceStatus) error {
		status.Host = authservice.Name
		status.Port = 5671
		status.CaCertSecret = authservice.Spec.Standard.CertificateSecret
		return nil
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	result, err = r.removeKeycloakController(ctx, authservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	return reconcile.Result{Requeue: requeue}, nil
}

func (r *ReconcileAuthenticationService) reconcileExternalAuthService(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {
	return r.updateStatus(ctx, authservice, func(status *adminv1beta1.AuthenticationServiceStatus) error {
		status.Host = authservice.Spec.External.Host
		status.Port = authservice.Spec.External.Port
		status.CaCertSecret = authservice.Spec.External.CaCertSecret
		status.ClientCertSecret = authservice.Spec.External.ClientCertSecret
		return nil
	})
}

type UpdateStatusFn func(status *adminv1beta1.AuthenticationServiceStatus) error

func (r *ReconcileAuthenticationService) updateStatus(ctx context.Context, authservice *adminv1beta1.AuthenticationService, updateFn UpdateStatusFn) (reconcile.Result, error) {

	newStatus := adminv1beta1.AuthenticationServiceStatus{}
	updateFn(&newStatus)

	if authservice.Status.Host != newStatus.Host ||
		authservice.Status.Port != newStatus.Port ||
		!reflect.DeepEqual(authservice.Status.CaCertSecret, newStatus.CaCertSecret) ||
		!reflect.DeepEqual(authservice.Status.ClientCertSecret, newStatus.ClientCertSecret) {

		authservice.Status = newStatus
		err := r.client.Update(ctx, authservice)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{Requeue: true}, nil
	}
	return reconcile.Result{}, nil
}

type ApplyDeploymentFn func(authservice *adminv1beta1.AuthenticationService, existingDeployment *appsv1.Deployment) error

func (r *ReconcileAuthenticationService) reconcileDeployment(ctx context.Context, authservice *adminv1beta1.AuthenticationService, name string, fn ApplyDeploymentFn) (reconcile.Result, error) {
	deployment := &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{Namespace: authservice.Namespace, Name: name},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, deployment, func(existing runtime.Object) error {
		existingDeployment := existing.(*appsv1.Deployment)

		if err := controllerutil.SetControllerReference(authservice, existingDeployment, r.scheme); err != nil {
			return err
		}

		return fn(authservice, existingDeployment)
	})

	if err != nil {
		log.Error(err, "Failed reconciling Deployment")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

type ApplyServiceFn func(authservice *adminv1beta1.AuthenticationService, existingService *corev1.Service) error

func (r *ReconcileAuthenticationService) reconcileService(ctx context.Context, authservice *adminv1beta1.AuthenticationService, name string, applyFn ApplyServiceFn) (reconcile.Result, error) {
	service := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: authservice.Namespace, Name: name},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func(existing runtime.Object) error {
		existingService := existing.(*corev1.Service)

		if err := controllerutil.SetControllerReference(authservice, existingService, r.scheme); err != nil {
			return err
		}

		return applyFn(authservice, existingService)
	})

	if err != nil {
		log.Error(err, "Failed reconciling Service")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

func (r *ReconcileAuthenticationService) reconcileStandardVolume(ctx context.Context, name string, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {
	if authservice.Spec.Type == adminv1beta1.Standard &&
		authservice.Spec.Standard.Storage != nil &&
		authservice.Spec.Standard.Storage.Type == adminv1beta1.PersistentClaim {
		pvc := &corev1.PersistentVolumeClaim{
			ObjectMeta: metav1.ObjectMeta{Namespace: authservice.Namespace, Name: name},
		}
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, pvc, func(existing runtime.Object) error {
			existingPvc := existing.(*corev1.PersistentVolumeClaim)

			if *authservice.Spec.Standard.Storage.DeleteClaim {
				if err := controllerutil.SetControllerReference(authservice, existingPvc, r.scheme); err != nil {
					return err
				}
			}
			return applyStandardAuthServiceVolume(authservice, existingPvc)
		})

		if err != nil {
			log.Error(err, "Failed reconciling PersistentVolumeClaim")
			return reconcile.Result{}, err
		}
	}
	return reconcile.Result{}, nil
}

func (r *ReconcileAuthenticationService) reconcileStandardRoute(ctx context.Context, name string, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {
	if authservice.Spec.Type == adminv1beta1.Standard && util.IsOpenshift() {
		route := &routev1.Route{
			ObjectMeta: metav1.ObjectMeta{Namespace: authservice.Namespace, Name: name},
		}
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, route, func(existing runtime.Object) error {
			existingRoute := existing.(*routev1.Route)

			secretName := types.NamespacedName{
				Name:      authservice.Spec.Standard.CertificateSecret.Name,
				Namespace: authservice.Namespace,
			}
			certsecret := &corev1.Secret{}
			err := r.client.Get(ctx, secretName, certsecret)
			if err != nil {
				return err
			}
			cert := certsecret.Data["tls.crt"]
			if err := controllerutil.SetControllerReference(authservice, existingRoute, r.scheme); err != nil {
				return err
			}
			return applyRoute(authservice, existingRoute, string(cert[:]))
		})

		if err != nil {
			log.Error(err, "Failed reconciling Route")
			return reconcile.Result{}, err
		}
	}
	return reconcile.Result{}, nil
}


/*
 * This function removes the keycloak controller if it exists. This process is no longer needed if this
 * operator is running.
 */
func (r *ReconcileAuthenticationService) removeKeycloakController(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {
	dep := &appsv1.Deployment{}
	name := types.NamespacedName{Namespace: authservice.Namespace, Name: "keycloak-controller"}
	err := r.client.Get(ctx, name, dep)
	if err != nil {
		if k8errors.IsNotFound(err) {
			return reconcile.Result{}, nil
		} else {
			return reconcile.Result{Requeue: true}, err
		}
	}
	err = r.client.Delete(ctx, dep)
	if err != nil {
		return reconcile.Result{Requeue: true}, err
	}
	return reconcile.Result{}, nil
}

/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package authenticationservice

import (
	"context"
	"fmt"
	"reflect"

	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/recon"

	routev1 "github.com/openshift/api/route/v1"
	"github.com/prometheus/client_golang/prometheus"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/metrics"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var (
	authinfo = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Name: "authentication_service_ready",
			Help: "EnMasse authentication services in the ready state",
		},
		[]string{"authservice_name", "authservice_type"},
	)
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

	metrics.Registry.MustRegister(authinfo)

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
	currentStatus := authservice.Status
	rc := &recon.ReconcileContext{}
	rc.ProcessSimple(func() error {
		return applyNoneAuthServiceDefaults(ctx, r.client, r.scheme, authservice)
	})

	rc.Process(func() (reconcile.Result, error) {
		return r.reconcileDeployment(ctx, authservice, authservice.Name, applyNoneAuthServiceDeployment)
	})

	rc.Process(func() (reconcile.Result, error) {
		return r.reconcileService(ctx, authservice, authservice.Name, applyNoneAuthServiceService)
	})

	if rc.Error() != nil || rc.NeedRequeue() {
		return rc.Result()
	}

	rc.Process(func() (reconcile.Result, error) {
		return r.updateStatus(ctx, authservice, currentStatus, func(status *adminv1beta1.AuthenticationServiceStatus) error {
			status.Phase = authservice.Status.Phase
			status.Message = authservice.Status.Message
			status.Host = fmt.Sprintf("%s.%s.svc", authservice.Name, authservice.Namespace)
			status.Port = 5671
			status.CaCertSecret = authservice.Spec.None.CertificateSecret
			return nil
		})
	})

	if authservice.Status.Phase == adminv1beta1.AuthenticationServiceActive {
		authinfo.WithLabelValues(authservice.Name, fmt.Sprintf("%v", authservice.Spec.Type)).Set(1)
	} else {
		authinfo.WithLabelValues(authservice.Name, fmt.Sprintf("%v", authservice.Spec.Type)).Set(0)
	}

	return rc.Result()
}

func (r *ReconcileAuthenticationService) reconcileStandardAuthService(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {

	currentStatus := authservice.Status
	rc := &recon.ReconcileContext{}
	rc.ProcessSimple(func() error {
		return applyStandardAuthServiceDefaults(ctx, r.client, r.scheme, authservice)
	})

	rc.Process(func() (reconcile.Result, error) {
		return r.reconcileService(ctx, authservice, *authservice.Spec.Standard.ServiceName, applyStandardAuthServiceService)
	})

	rc.Process(func() (reconcile.Result, error) {
		return r.reconcileStandardVolume(ctx, *authservice.Spec.Standard.Storage.ClaimName, authservice)
	})

	rc.Process(func() (reconcile.Result, error) {
		return r.reconcileStandardRoute(ctx, *authservice.Spec.Standard.RouteName, authservice)
	})

	rc.Process(func() (reconcile.Result, error) {
		return r.reconcileDeployment(ctx, authservice, *authservice.Spec.Standard.DeploymentName, applyStandardAuthServiceDeployment)
	})

	if rc.Error() != nil || rc.NeedRequeue() {
		return rc.Result()
	}

	rc.Process(func() (reconcile.Result, error) {
		return r.updateStatus(ctx, authservice, currentStatus, func(status *adminv1beta1.AuthenticationServiceStatus) error {
			status.Phase = authservice.Status.Phase
			status.Message = authservice.Status.Message
			status.Host = fmt.Sprintf("%s.%s.svc", authservice.Name, authservice.Namespace)
			status.Port = 5671
			status.CaCertSecret = authservice.Spec.Standard.CertificateSecret
			return nil
		})
	})

	if authservice.Status.Phase == adminv1beta1.AuthenticationServiceActive {
		authinfo.WithLabelValues(authservice.Name, fmt.Sprintf("%v", authservice.Spec.Type)).Set(1)
	} else {
		authinfo.WithLabelValues(authservice.Name, fmt.Sprintf("%v", authservice.Spec.Type)).Set(0)
	}

	rc.Process(func() (reconcile.Result, error) {
		return r.removeKeycloakController(ctx, authservice)
	})

	return rc.Result()
}

func (r *ReconcileAuthenticationService) reconcileExternalAuthService(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (reconcile.Result, error) {
	currentStatus := authservice.Status
	authinfo.WithLabelValues(authservice.Name, fmt.Sprintf("%v", authservice.Spec.Type)).Set(1)
	return r.updateStatus(ctx, authservice, currentStatus, func(status *adminv1beta1.AuthenticationServiceStatus) error {
		status.Phase = adminv1beta1.AuthenticationServiceActive
		status.Host = authservice.Spec.External.Host
		status.Port = authservice.Spec.External.Port
		status.CaCertSecret = authservice.Spec.External.CaCertSecret
		status.ClientCertSecret = authservice.Spec.External.ClientCertSecret
		return nil
	})
}

type UpdateStatusFn func(status *adminv1beta1.AuthenticationServiceStatus) error

func (r *ReconcileAuthenticationService) updateStatus(ctx context.Context, authservice *adminv1beta1.AuthenticationService, currentStatus adminv1beta1.AuthenticationServiceStatus, updateFn UpdateStatusFn) (reconcile.Result, error) {

	newStatus := adminv1beta1.AuthenticationServiceStatus{}
	if err := updateFn(&newStatus); err != nil {
		return reconcile.Result{}, err
	}

	if currentStatus.Host != newStatus.Host ||
		currentStatus.Port != newStatus.Port ||
		currentStatus.Phase != newStatus.Phase ||
		currentStatus.Message != newStatus.Message ||
		!reflect.DeepEqual(currentStatus.CaCertSecret, newStatus.CaCertSecret) ||
		!reflect.DeepEqual(currentStatus.ClientCertSecret, newStatus.ClientCertSecret) {

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
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, deployment, func() error {
		if err := controllerutil.SetControllerReference(authservice, deployment, r.scheme); err != nil {
			return err
		}
		return fn(authservice, deployment)
	})

	if deployment.Status.AvailableReplicas < 1 {
		authservice.Status.Phase = adminv1beta1.AuthenticationServiceConfiguring
		authservice.Status.Message = "Waiting for deployment: " + deployment.Name
	} else {
		authservice.Status.Phase = adminv1beta1.AuthenticationServiceActive
		authservice.Status.Message = ""
	}

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
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func() error {
		if err := controllerutil.SetControllerReference(authservice, service, r.scheme); err != nil {
			return err
		}

		return applyFn(authservice, service)
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
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, pvc, func() error {
			if *authservice.Spec.Standard.Storage.DeleteClaim {
				if err := controllerutil.SetControllerReference(authservice, pvc, r.scheme); err != nil {
					return err
				}
			}
			return applyStandardAuthServiceVolume(authservice, pvc)
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
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, route, func() error {
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
			if err := controllerutil.SetControllerReference(authservice, route, r.scheme); err != nil {
				return err
			}
			return applyRoute(authservice, route, string(cert[:]))
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

/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginguser

import (
	"context"
	"fmt"
	"strings"

	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/keycloak"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/enmasseproject/enmasse/pkg/util/recon"

	logr "github.com/go-logr/logr"
	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messaginguser")
var _ reconcile.Reconciler = &ReconcileMessagingUser{}

const (
	ANNOTATION_REALM_NAME = "enmasse.io/realm-name"
	FINALIZER_NAME        = "enmasse.io/standard-authservice"
)

type ReconcileMessagingUser struct {
	client    client.Client
	reader    client.Reader
	scheme    *runtime.Scheme
	namespace string

	newKeycloakClientFunc keycloak.NewKeycloakClientFunc
	keycloakClients       map[string]keycloak.KeycloakClient
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileMessagingUser {
	return &ReconcileMessagingUser{
		client:                mgr.GetClient(),
		reader:                mgr.GetAPIReader(),
		scheme:                mgr.GetScheme(),
		newKeycloakClientFunc: keycloak.NewClient,
		keycloakClients:       make(map[string]keycloak.KeycloakClient),
		namespace:             util.GetEnvOrDefault("NAMESPACE", "enmasse-infra"),
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingUser) error {

	// Create a new controller
	c, err := controller.New("messaginguser-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource MessagingUser
	err = c.Watch(&source.Kind{Type: &userv1beta1.MessagingUser{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return nil
}

func (r *ReconcileMessagingUser) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling MessagingUser")

	ctx := context.TODO()

	user := &userv1beta1.MessagingUser{}
	err := r.client.Get(ctx, request.NamespacedName, user)
	if err != nil {
		if k8errors.IsNotFound(err) {
			reqLogger.Info("MessagingUser resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		reqLogger.Error(err, "Failed to get MessagingUser")
		return reconcile.Result{}, err
	}

	rc := &recon.ReconcileContext{}

	reqLogger.V(8).Info("checkFinalizer", "user", user)
	rc.Process(func() (reconcile.Result, error) {
		return r.checkFinalizer(ctx, reqLogger, user)
	})

	if rc.Error() != nil || rc.NeedRequeue() {
		return rc.Result()
	}

	lastMessage := user.Status.Message
	user.Status.Message = ""
	rc.ProcessSimple(func() error {
		if user.Status.Phase == "" {
			user.Status.Phase = userv1beta1.UserPending
			err := r.client.Status().Update(ctx, user)
			if err != nil {
				return err
			}
		}
		return nil
	})

	reqLogger.V(8).Info("createOrUpdate", "user", user)
	rc.Process(func() (reconcile.Result, error) {
		if user.Status.Phase != userv1beta1.UserTerminating {
			return r.createOrUpdateUser(ctx, reqLogger, user)
		}
		return reconcile.Result{}, nil
	})

	if rc.Error() != nil {
		user.Status.Message = rc.Error().Error()
	}

	reqLogger.V(8).Info("updateGeneration", "user", user)
	rc.ProcessSimple(func() error {
		if user.Status.Message != lastMessage || user.Generation != user.Status.Generation {
			user.Status.Generation = user.Generation
			err := r.client.Status().Update(ctx, user)
			if err != nil {
				return err
			}
		}
		return nil
	})

	return rc.Result()
}

func (r *ReconcileMessagingUser) createOrUpdateUser(ctx context.Context, logger logr.Logger, user *userv1beta1.MessagingUser) (reconcile.Result, error) {

	addressSpace, err := r.lookupAddressSpace(ctx, logger, user)
	if err != nil {
		return reconcile.Result{Requeue: true}, err
	}

	authenticationService, err := r.lookupAuthenticationService(ctx, logger, addressSpace)
	if err != nil {
		return reconcile.Result{Requeue: true}, err
	}

	if authenticationService.Spec.Type == adminv1beta1.Standard {
		if !isAuthserviceAvailable(authenticationService) {
			return reconcile.Result{}, fmt.Errorf("Authentication service %s is not yet available", authenticationService.Name)
		}
		if user.Status.Phase == userv1beta1.UserPending {
			user.Status.Phase = userv1beta1.UserConfiguring
			err = r.client.Status().Update(ctx, user)
			if err != nil {
				return reconcile.Result{}, err
			}
		}
		client, err := r.getKeycloakClient(ctx, authenticationService)
		if err != nil {
			return reconcile.Result{}, err
		}

		realm := addressSpace.Annotations[ANNOTATION_REALM_NAME]

		existingUser, err := client.GetUser(realm, user.Spec.Username)
		if err != nil {
			return reconcile.Result{}, err
		}

		if existingUser == nil {
			err := client.CreateUser(realm, user)
			if err != nil {
				return reconcile.Result{}, err
			}

		} else {
			err := client.UpdateUser(realm, existingUser, user)
			if err != nil {
				return reconcile.Result{}, err
			}
		}
	}

	if user.Status.Phase == userv1beta1.UserConfiguring {
		user.Status.Phase = userv1beta1.UserActive
		err = r.client.Status().Update(ctx, user)
		if err != nil {
			return reconcile.Result{}, err
		}
	}
	return reconcile.Result{}, nil
}

func (r *ReconcileMessagingUser) checkFinalizer(ctx context.Context, logger logr.Logger, user *userv1beta1.MessagingUser) (reconcile.Result, error) {
	return finalizer.ProcessFinalizers(ctx, r.client, r.reader, user, []finalizer.Finalizer{
		finalizer.Finalizer{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				user, ok := c.Object.(*userv1beta1.MessagingUser)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingUser")
				}
				user.Status.Phase = userv1beta1.UserTerminating
				err := r.client.Status().Update(ctx, user)
				if err != nil {
					return reconcile.Result{}, err
				}

				addressSpace, err := r.lookupAddressSpace(ctx, logger, user)
				if err != nil {
					// Do not block finalizer if address space is gone
					if k8errors.IsNotFound(err) {
						return reconcile.Result{}, nil
					}
					return reconcile.Result{}, err
				}

				authenticationService, err := r.lookupAuthenticationService(ctx, logger, addressSpace)
				if err != nil {
					// Do not block finalizer if authentication service is gone
					if k8errors.IsNotFound(err) {
						return reconcile.Result{}, nil
					}
					return reconcile.Result{}, err
				}
				if authenticationService.Spec.Type == adminv1beta1.Standard && isAuthserviceAvailable(authenticationService) {
					client, err := r.getKeycloakClient(ctx, authenticationService)
					if err != nil {
						return reconcile.Result{}, nil
					}

					realm := addressSpace.Annotations[ANNOTATION_REALM_NAME]

					user, err := client.GetUser(realm, user.Spec.Username)
					if err != nil {
						return reconcile.Result{}, nil
					}
					if user != nil {
						err = client.DeleteUser(realm, user)
						return reconcile.Result{}, err
					}
					return reconcile.Result{}, nil
				} else {
					logger.Info("Unable to finalize MessagingUser: authentication service not ready")
					return reconcile.Result{Requeue: true}, nil
				}
			},
		},
	})
}

func (r *ReconcileMessagingUser) lookupAddressSpace(ctx context.Context, logger logr.Logger, user *userv1beta1.MessagingUser) (*enmassev1beta1.AddressSpace, error) {
	addressSpace := &enmassev1beta1.AddressSpace{}
	addressSpaceName := types.NamespacedName{
		Name:      strings.Split(user.Name, ".")[0],
		Namespace: user.Namespace,
	}
	err := r.client.Get(ctx, addressSpaceName, addressSpace)
	if err != nil {
		logger.Error(err, "Failed to get AddressSpace for MessagingUser")
		return nil, err
	}
	return addressSpace, nil
}

func (r *ReconcileMessagingUser) lookupAuthenticationService(ctx context.Context, logger logr.Logger, addressSpace *enmassev1beta1.AddressSpace) (*adminv1beta1.AuthenticationService, error) {
	if addressSpace.Spec.AuthenticationService == nil {
		return nil, fmt.Errorf("Authentication service not (yet) set for address space %s/%s", addressSpace.Namespace, addressSpace.Name)
	}
	authNameRef := addressSpace.Spec.AuthenticationService.Name
	if authNameRef == "" {
		authNameRef = addressSpace.Spec.AuthenticationService.Type
	}
	authenticationService := &adminv1beta1.AuthenticationService{}
	authServiceName := types.NamespacedName{
		Name:      authNameRef,
		Namespace: r.namespace,
	}
	if authServiceName.Name == "" {
		authServiceName.Name = addressSpace.Spec.AuthenticationService.Type
	}
	err := r.client.Get(ctx, authServiceName, authenticationService)
	if err != nil {
		logger.Error(err, "Failed to get AuthenticationService for MessagingUser")
		return nil, err
	}
	return authenticationService, nil
}

func (r *ReconcileMessagingUser) getKeycloakClient(ctx context.Context, authservice *adminv1beta1.AuthenticationService) (keycloak.KeycloakClient, error) {
	existing, ok := r.keycloakClients[authservice.Name]
	if ok {
		log.Info("Found existing keycloak client", "authservice", authservice.Name)
		return existing, nil
	} else {
		var ca []byte
		if authservice.Status.CaCertSecret != nil {
			caCertSecret := &corev1.Secret{}
			err := r.client.Get(ctx, types.NamespacedName{
				Name:      authservice.Status.CaCertSecret.Name,
				Namespace: r.namespace,
			}, caCertSecret)
			if err != nil {
				return nil, err
			}

			ca = caCertSecret.Data["tls.crt"]
		}

		credentials := &corev1.Secret{}
		err := r.client.Get(ctx, types.NamespacedName{
			Name:      authservice.Spec.Standard.CredentialsSecret.Name,
			Namespace: r.namespace,
		}, credentials)
		if err != nil {
			return nil, err
		}

		adminUser := credentials.Data["admin.username"]
		adminPassword := credentials.Data["admin.password"]
		client, err := r.newKeycloakClientFunc(authservice.Status.Host, 8443, string(adminUser), string(adminPassword), ca)
		if err != nil {
			return nil, err
		}
		r.keycloakClients[authservice.Name] = client
		return client, nil
	}
}

func isAuthserviceAvailable(authservice *adminv1beta1.AuthenticationService) bool {
	return authservice.Status.Host != ""
}

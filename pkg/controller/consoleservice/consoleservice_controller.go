/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consoleservice

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	oauthv1 "github.com/openshift/api/oauth/v1"
	routev1 "github.com/openshift/api/route/v1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/intstr"
	"net/url"
	"reflect"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
	"strings"
)


/*
TODO TLS for the HTTPD side car
TODO Tidy up (minimise) Apache HTTPD conf
TODO Tidy up this code - extract utility methods
TODO unit tests - having prblem with client when interacting with openshift API endpoints?
 */
const CONSOLE_NAME = "console"

var log = logf.Log.WithName("controller_consoleservice")

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {

	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileConsoleService {
	return &ReconcileConsoleService{client: mgr.GetClient(), scheme: mgr.GetScheme(), namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")}
}

func add(mgr manager.Manager, r *ReconcileConsoleService) error {

	// Create a new controller
	c, err := controller.New("consoleservice-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource ConsoleService
	err = c.Watch(&source.Kind{Type: &v1beta1.ConsoleService{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &appsv1.Deployment{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &v1beta1.ConsoleService{},
	})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &corev1.Service{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &v1beta1.ConsoleService{},
	})
	if err != nil {
		return err
	}

	if (util.IsOpenshift()) {
		// Changes to the secret or routes potentially need to be written to the oauthclient
		err = c.Watch(&source.Kind{Type: &corev1.Secret{}}, &handler.EnqueueRequestForOwner{
			IsController: true,
			OwnerType:    &v1beta1.ConsoleService{},
		})
		if err != nil {
			return err
		}

		// Watch for route changes.
		mapFn := handler.ToRequestsFunc(
			func(a handler.MapObject) []reconcile.Request {

				reqs := make([]reconcile.Request, 0)

				if strings.HasPrefix(a.Meta.GetName(), "console-") {
					list := &v1beta1.ConsoleServiceList{}
					err = r.client.List(context.TODO(),  &client.ListOptions{}, list)
					if err == nil {
						for _, item := range list.Items {
							request := reconcile.Request{
								NamespacedName: types.NamespacedName{
									Name:      item.ObjectMeta.Name,
									Namespace: item.ObjectMeta.Namespace,
								},
							}
							reqs = append(reqs, request)
						}
					}
				}
				return reqs
			})
		err = c.Watch(&source.Kind{Type: &routev1.Route{}}, &handler.EnqueueRequestsFromMapFunc{
			ToRequests: mapFn,
		})
		if err != nil {
			return err
		}
	}

	// Currently we need a single instance of console called "console", ensure that it exists.
	err = ensureSingletonConsoleService(context.TODO(), metav1.ObjectMeta{Namespace: r.namespace, Name: CONSOLE_NAME}, r.client)
	if err != nil {
		log.Error(err, "Failed create singleton ConsoleService instance")
	}

	return nil
}

var _ reconcile.Reconciler = &ReconcileConsoleService{}

type ReconcileConsoleService struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client    client.Client
	scheme    *runtime.Scheme
	namespace string
}

// Reconcile by reading the console service spec and making required changes
//
// returning an error will get the request re-queued
func (r *ReconcileConsoleService) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling ConsoleService")

	ctx := context.TODO()
	consoleservice := &v1beta1.ConsoleService{}
	err := r.client.Get(ctx, request.NamespacedName, consoleservice)
	if err != nil {
		if k8errors.IsNotFound(err) {
			if (CONSOLE_NAME == request.NamespacedName.Name) {
				err = ensureSingletonConsoleService(ctx, metav1.ObjectMeta{Namespace: request.NamespacedName.Namespace,
					Name: request.NamespacedName.Name}, r.client)
				return reconcile.Result{}, err
			} else {
				reqLogger.Info("ConsoleService resource not found. Ignoring since object must be deleted")
				return reconcile.Result{}, nil
			}
		}
		// Error reading the object - requeue the request
		reqLogger.Error(err, "Failed to get ConsoleService")
		return reconcile.Result{}, err
	}

	err = applyConsoleServiceDefaults(ctx, r.client, r.scheme, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}

	// service
	result, err := r.reconcileService(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue := result.Requeue

	// route
	result, route, err := r.reconcileRoute(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	// oauthclient
	result, err = r.reconcileOauthClient(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	// deployment
	result, err = r.reconcileDeployment(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	result, err = r.updateStatus(ctx, consoleservice, func(status *v1beta1.ConsoleServiceStatus) error {

		if route != nil && len(route.Status.Ingress) > 0 {
			status.Host = route.Status.Ingress[0].Host
			status.Port = 443
			var cert = consoleservice.Spec.CertificateSecret
			status.CaCertSecret = cert
		}
		return nil
	})

	return reconcile.Result{Requeue: requeue}, nil
}

type UpdateStatusFn func(status *v1beta1.ConsoleServiceStatus) error

func (r *ReconcileConsoleService) updateStatus(ctx context.Context, consoleservice *v1beta1.ConsoleService, updateFn UpdateStatusFn) (reconcile.Result, error) {

	newStatus := v1beta1.ConsoleServiceStatus{}
	updateFn(&newStatus)

	if consoleservice.Status.Host != newStatus.Host ||
		consoleservice.Status.Port != newStatus.Port ||
		!reflect.DeepEqual(consoleservice.Status.CaCertSecret, newStatus.CaCertSecret) {

		consoleservice.Status = newStatus
		err := r.client.Update(ctx, consoleservice)
		if err != nil {
			return reconcile.Result{}, err
		}
		return reconcile.Result{Requeue: true}, nil
	}
	return reconcile.Result{}, nil
}

func applyConsoleServiceDefaults(ctx context.Context, client client.Client, scheme *runtime.Scheme, consoleservice *v1beta1.ConsoleService) error {
	var dirty = false;

	if consoleservice.Spec.CertificateSecret == nil {
		dirty = true
		secretName := consoleservice.Name + "-cert"
		consoleservice.Spec.CertificateSecret = &corev1.SecretReference{
			Name: secretName,
		}

		if !util.IsOpenshift() {
			err := util.CreateSecret(ctx, client, scheme, consoleservice.Namespace, secretName, consoleservice, func(secret *corev1.Secret) error {
				install.ApplyDefaultLabels(&secret.ObjectMeta, "consoleservice", secretName)

				cn := util.ServiceToCommonName(consoleservice.Namespace, consoleservice.Name)
				return util.GenerateSelfSignedCertSecret(cn, nil, nil, secret)
			})
			if err != nil {
				return err
			}
		}
	}

	if util.IsOpenshift() {
		if consoleservice.Spec.Scope == nil {
			dirty = true
			scope := "user:full"
			consoleservice.Spec.Scope = &scope
		}

		if consoleservice.Spec.OauthClientSecret == nil {
			dirty = true
			secretName := consoleservice.Name + "-oauth"
			consoleservice.Spec.OauthClientSecret = &corev1.SecretReference{Name: secretName}
		}

		if consoleservice.Spec.DiscoveryMetadataURL == nil {
			dirty = true
			discoveryURL := "https://openshift.default.svc/.well-known/oauth-authorization-server"

			openshiftUri, rewritten, err := util.OpenshiftUri()
			if err != nil {
				return err
			}

			if rewritten  {
				// The well known metadata will be unusable
				metadata, err := util.WellKnownOauthMetadata()
				if err != nil {
					return err
				}

				keys := []string{
					"issuer",
					"authorization_endpoint",
					"token_endpoint",}

				for _, k := range keys {
					if u, ok := metadata[k]; ok {
						metadata_url, err := url.Parse(u.(string))
						if err == nil {
							metadata_url.Host = openshiftUri.Host
							metadata_url.Scheme = openshiftUri.Scheme
							metadata[k] = metadata_url.String()
						}
					}
				}

				metadata_bytes, err := json.Marshal(metadata)
				if err != nil {
					return err
				}

				discoveryURL = "data:application/json;base64," + base64.StdEncoding.EncodeToString(metadata_bytes)
			}

			consoleservice.Spec.DiscoveryMetadataURL = &discoveryURL
		}
	} else {
		if consoleservice.Spec.Scope == nil {
			dirty = true
			scope := "openid"
			consoleservice.Spec.Scope = &scope
		}
	}

	if dirty {
		// address-space-controller needs to know the default values, so we rewrite the object.
		err := client.Update(ctx, consoleservice);
		return err
	}

	return nil
}

func (r *ReconcileConsoleService) reconcileService(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	service := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: consoleservice.Name},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func(existing runtime.Object) error {
		existingService := existing.(*corev1.Service)

		if err := controllerutil.SetControllerReference(consoleservice, existingService, r.scheme); err != nil {
			return err
		}

		return applyService(consoleservice, existingService)
	})

	if err != nil {
		log.Error(err, "Failed reconciling Service")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

func applyService(consoleService *v1beta1.ConsoleService, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "consoleservice", consoleService.Name)
	service.Spec.Selector = install.CreateDefaultLabels(nil, "consoleservice", consoleService.Name)

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}
	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = consoleService.Spec.CertificateSecret.Name
	service.Spec.Ports = []corev1.ServicePort{
		{
			Port:       8443,
			Protocol:   corev1.ProtocolTCP,
			TargetPort: intstr.FromString("https"),
			Name:       "https",
		},
	}
	return nil
}


func (r *ReconcileConsoleService) reconcileRoute(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, *routev1.Route, error) {
	if util.IsOpenshift() {
		route := &routev1.Route{
			ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: consoleservice.Name},
		}
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, route, func(existing runtime.Object) error {
			route = existing.(*routev1.Route)

			secretName := types.NamespacedName{
				Name:      consoleservice.Spec.CertificateSecret.Name,
				Namespace: consoleservice.Namespace,
			}
			certsecret := &corev1.Secret{}
			err := r.client.Get(ctx, secretName, certsecret)
			if err != nil {
				return err
			}
			cert := certsecret.Data["tls.crt"]
			if err := controllerutil.SetControllerReference(consoleservice, route, r.scheme); err != nil {
				return err
			}
			return applyRoute(consoleservice, route, string(cert[:]))
		})

		if err != nil {
			log.Error(err, "Failed reconciling Route")
			return reconcile.Result{}, nil, err
		}
		return reconcile.Result{}, route, nil
	} else {
		return reconcile.Result{}, nil, nil
	}
}

func applyRoute(consoleservice *v1beta1.ConsoleService, route *routev1.Route, caCertificate string) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "consoleservice", consoleservice.Name)

	route.Spec = routev1.RouteSpec{
		To: routev1.RouteTargetReference{
			Kind: "Service",
			Name: consoleservice.Name,
		},
		TLS: &routev1.TLSConfig{
			Termination:   routev1.TLSTerminationReencrypt,
			CACertificate: caCertificate,
		},
		Port: &routev1.RoutePort{
			TargetPort: intstr.FromString("https"),
		},
	}

	if consoleservice.Spec.Host != nil {
		route.Spec.Host = *consoleservice.Spec.Host
	}
	return nil
}

func (r *ReconcileConsoleService) reconcileDeployment(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	deployment := &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: consoleservice.Name},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, deployment, func(existing runtime.Object) error {
		existingDeployment := existing.(*appsv1.Deployment)

		if err := controllerutil.SetControllerReference(consoleservice, existingDeployment, r.scheme); err != nil {
			return err
		}

		return applyDeployment(consoleservice, existingDeployment)
	})

	if err != nil {
		log.Error(err, "Failed reconciling Deployment")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

func applyDeployment(consoleservice *v1beta1.ConsoleService, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "consoleservice", consoleservice.Name)

	install.ApplyEmptyDirVolume(deployment, "apps")
	install.ApplySecretVolume(deployment, "console-tls", consoleservice.Spec.CertificateSecret.Name)

	install.ApplyInitContainer(deployment, "console-init", func(container *corev1.Container) {
		install.ApplyContainerImage(container, "console-init", nil)

		if consoleservice.Spec.Scope != nil {
			install.ApplyEnv(container, "OAUTH2_SCOPE", func(envvar *corev1.EnvVar) {
				envvar.Value = *consoleservice.Spec.Scope;
			})
		}

		if consoleservice.Spec.DiscoveryMetadataURL != nil {
			install.ApplyEnv(container, "DISCOVERY_METADATA_URL", func(envvar *corev1.EnvVar) {
				envvar.Value = *consoleservice.Spec.DiscoveryMetadataURL;
			})
		}

		container.Command = []string{"/oauth-proxy/bin/init.sh", "/apps/"}
		install.ApplyVolumeMountSimple(container, "apps", "/apps", false);
	})


	if util.IsOpenshift() {
		install.ApplyContainer(deployment, "console-proxy", func(container *corev1.Container) {
			install.ApplyContainerImage(container, "console-proxy-openshift", nil)

			container.Args = []string{"-config=/apps/cfg/oauth-proxy-openshift.cfg"}

			install.ApplyVolumeMountSimple(container, "apps", "/apps", false);
			install.ApplyVolumeMountSimple(container, "console-tls", "/etc/tls/private", true);

			if consoleservice.Spec.OauthClientSecret != nil {
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_ID", "client-id", consoleservice.Spec.OauthClientSecret.Name)
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_SECRET", "client-secret", consoleservice.Spec.OauthClientSecret.Name)
			}

			container.Ports = []corev1.ContainerPort{{
				ContainerPort: 8443,
				Name:          "https",
			}}

			container.ReadinessProbe = &corev1.Probe{
				InitialDelaySeconds: 60,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
			container.LivenessProbe = &corev1.Probe{
				InitialDelaySeconds: 120,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
		})

		install.ApplyContainer(deployment, "console-httpd", func(container *corev1.Container) {
			install.ApplyContainerImage(container, "console-httpd", nil)

			container.Ports = []corev1.ContainerPort{{
				ContainerPort: 8080,
				Name:          "http",
			}}

			// Can't define a probe as HTTPD bound to loopback.  Perhaps have oauth-proxy's probes reach through
			// HTTP and hit whoami?

		})
	} else {
		install.ApplyContainer(deployment, "console-proxy", func(container *corev1.Container) {
			install.ApplyContainerImage(container, "console-proxy-kubernetes", nil)

			container.Args = []string{"-config=/apps/cfg/oauth-proxy-kubernetes.cfg"}

			install.ApplyVolumeMountSimple(container, "apps", "/apps", false);
			install.ApplyVolumeMountSimple(container, "console-tls", "/etc/tls/private", true);

			if consoleservice.Spec.OauthClientSecret != nil {
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_ID", "client-id", consoleservice.Spec.OauthClientSecret.Name)
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_SECRET", "client-secret", consoleservice.Spec.OauthClientSecret.Name)
			}

			if consoleservice.Spec.Scope != nil {
				install.ApplyEnv(container, "SSL_CERT_DIR", func(envvar *corev1.EnvVar) {
					envvar.Value = "/var/run/secrets/kubernetes.io/serviceaccount/";
				})
			}

			container.Ports = []corev1.ContainerPort{{
				ContainerPort: 8443,
				Name:          "https",
			}}

			container.ReadinessProbe = &corev1.Probe{
				InitialDelaySeconds: 60,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
			container.LivenessProbe = &corev1.Probe{
				InitialDelaySeconds: 120,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
		})
	}

	deployment.Spec.Strategy = appsv1.DeploymentStrategy{
		Type: appsv1.RecreateDeploymentStrategyType,
	}
	return nil
}

func (r *ReconcileConsoleService) reconcileOauthClient(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	if util.IsOpenshift() {

		secretref := consoleservice.Spec.OauthClientSecret

		secret := &corev1.Secret{
			ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: secretref.Name},
		}
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, secret, func(existing runtime.Object) error {
			secret = existing.(*corev1.Secret)
			if err := controllerutil.SetControllerReference(consoleservice, secret, r.scheme); err != nil {
				return err
			}
			return applyOauthSecret(secret)
		})

		if err != nil {
			log.Error(err, "Failed reconciling OAuth Secret")
			return reconcile.Result{}, err
		}

		key := client.ObjectKey{Namespace: consoleservice.Namespace, Name: consoleservice.Name}
		route := &routev1.Route{}
		err = r.client.Get(ctx, key, route)
		if err != nil {
			return reconcile.Result{}, err
		}

		if len(route.Status.Ingress) == 0 {
			return reconcile.Result{}, nil
		}

		redirects := make([]string, 0)
		redirects = buildRedirectsFor(*route, redirects)

		list := &routev1.RouteList{}

		opts := &client.ListOptions{}
		//_ = opts.SetLabelSelector("app=enmasse")
		err = r.client.List(context.TODO(), opts, list)
		if err != nil {
			return reconcile.Result{}, nil
		}

		for _, item := range list.Items {
			// TODO it would be better if we could use a label allowed us to identify the routes belonging
			// to address space console instances.
			if strings.HasPrefix(item.Name, "console-")  {
				redirects = buildRedirectsFor(item, redirects)
			}
		}
		oauth := &oauthv1.OAuthClient{
			ObjectMeta: metav1.ObjectMeta{Name: secret.Name},
		}

		_, err = controllerutil.CreateOrUpdate(ctx, r.client, oauth, func(existing runtime.Object) error {
			existingOauth := existing.(*oauthv1.OAuthClient)

			err = applyOauthClient(existingOauth, secret, redirects)
			if err != nil {
				return err
			}
			return nil
		})

		if err != nil {
			log.Error(err, "Failed reconciling OAuth")
			return reconcile.Result{}, err
		}

		return reconcile.Result{}, nil
	}
	return reconcile.Result{}, nil
}

func buildRedirectsFor(route routev1.Route, redirects []string) []string {
	scheme := "http"
	if route.Spec.TLS != nil {
		scheme = "https"
	}
	for _, ingress := range route.Status.Ingress {
		redirect := fmt.Sprintf("%s://%s", scheme, ingress.Host)
		redirects = append(redirects, redirect)
	}
	return redirects
}


func applyOauthClient(oauth *oauthv1.OAuthClient, secret *corev1.Secret, redirects []string) error {
	install.ApplyDefaultLabels(&oauth.ObjectMeta, "oauthclient", oauth.Name)
	bytes := secret.Data["client-secret"]
	oauth.Secret = string(bytes[:])

	oauth.GrantMethod = oauthv1.GrantHandlerAuto
	oauth.RedirectURIs = redirects;
	return nil
}

func applyOauthSecret(secret *corev1.Secret) error {

	if secret.Data == nil {
		secret.Data = make(map[string][]byte)
	}

	if _, hassecret := secret.Data["client-secret"];  !hassecret {
		password, err := util.GeneratePassword(32)
		if err != nil {
			return err
		}

		secret.Data["client-secret"] = []byte(password)
	}

	if _, hasid := secret.Data["client-id"];  !hasid {
		secret.Data["client-id"] = []byte(secret.Name)
	}

	return nil
}

func ensureSingletonConsoleService(ctx context.Context, objectMeta metav1.ObjectMeta, c client.Client) error {

	consoleservice := &v1beta1.ConsoleService{
		ObjectMeta: objectMeta,
	}
	_, err := controllerutil.CreateOrUpdate(ctx, c, consoleservice, func(existing runtime.Object) error {
		return nil
	})

	list := &v1beta1.ConsoleServiceList{}
	opts := &client.ListOptions{}
	err = c.List(ctx, opts, list)
	if err != nil {
		return err
	}

	for _, item := range list.Items {
		if "console" != item.Name  {
			err = c.Delete(ctx, &item, nil);
			break;
		}
	}

	return err
}


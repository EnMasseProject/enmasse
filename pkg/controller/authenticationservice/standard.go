/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package authenticationservice

import (
	"context"
	"fmt"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	oauthv1 "github.com/openshift/api/oauth/v1"
	routev1 "github.com/openshift/api/route/v1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	resource "k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/runtime"
	intstr "k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func applyStandardAuthServiceDefaults(ctx context.Context, client client.Client, scheme *runtime.Scheme, authservice *adminv1beta1.AuthenticationService) error {
	if authservice.Spec.Standard == nil {
		authservice.Spec.Standard = &adminv1beta1.AuthenticationServiceSpecStandard{}
	}
	if authservice.Spec.Standard.Storage == nil {
		authservice.Spec.Standard.Storage = &adminv1beta1.AuthenticationServiceSpecStandardStorage{
			Type: adminv1beta1.Ephemeral,
		}
	}
	if authservice.Spec.Standard.DeploymentName == nil {
		authservice.Spec.Standard.DeploymentName = &authservice.Name
	}
	if authservice.Spec.Standard.ServiceName == nil {
		authservice.Spec.Standard.ServiceName = &authservice.Name
	}
	if authservice.Spec.Standard.RouteName == nil {
		authservice.Spec.Standard.RouteName = &authservice.Name
	}
	if authservice.Spec.Standard.Storage.ClaimName == nil {
		authservice.Spec.Standard.Storage.ClaimName = &authservice.Name
	}
	if authservice.Spec.Standard.Storage.DeleteClaim == nil {
		authservice.Spec.Standard.Storage.DeleteClaim = new(bool)
		*authservice.Spec.Standard.Storage.DeleteClaim = true
	}
	if authservice.Spec.Standard.Resources == nil {
		authservice.Spec.Standard.Resources = &corev1.ResourceRequirements{
			Requests: map[corev1.ResourceName]resource.Quantity{"memory": *resource.NewScaledQuantity(2, resource.Giga)},
			Limits:   map[corev1.ResourceName]resource.Quantity{"memory": *resource.NewScaledQuantity(2, resource.Giga)},
		}
	}
	if authservice.Spec.Standard.Datasource == nil {
		authservice.Spec.Standard.Datasource = &adminv1beta1.AuthenticationServiceSpecStandardDatasource{
			Type: adminv1beta1.H2Datasource,
		}
	}
	if authservice.Spec.Standard.CredentialsSecret == nil {
		secretName := "keycloak-credentials"
		authservice.Spec.Standard.CredentialsSecret = &corev1.SecretReference{
			Name: secretName,
		}
		err := util.CreateSecret(ctx, client, scheme, authservice.Namespace, secretName, authservice, func(secret *corev1.Secret) error {
			install.ApplyDefaultLabels(&secret.ObjectMeta, "standard-authservice", secretName)

			secret.StringData = make(map[string]string)
			secret.StringData["admin.username"] = "admin"
			adminPassword, err := util.GeneratePassword(32)
			if err != nil {
				return err
			}
			secret.StringData["admin.password"] = adminPassword
			return nil
		})
		if err != nil {
			return err
		}
	}
	if authservice.Spec.Standard.CertificateSecret == nil {
		secretName := "standard-authservice-cert"
		authservice.Spec.Standard.CertificateSecret = &corev1.SecretReference{
			Name: secretName,
		}

		if !util.IsOpenshift() {
			err := util.CreateSecret(ctx, client, scheme, authservice.Namespace, secretName, authservice, func(secret *corev1.Secret) error {
				cn := util.ServiceToCommonName(authservice.Namespace, *authservice.Spec.Standard.ServiceName)
				return util.GenerateSelfSignedCertSecret(cn, nil, nil, secret)
			})
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func applyStandardAuthServiceDeployment(authservice *adminv1beta1.AuthenticationService, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "standard-authservice", *authservice.Spec.Standard.DeploymentName)

	install.ApplyInitContainer(deployment, "keycloak-plugin", func(container *corev1.Container) {
		install.ApplyContainerImage(container, "keycloak-plugin", authservice.Spec.Standard.InitImage)
		install.ApplyEnvSimple(container, "KEYCLOAK_DIR", "/opt/jboss/keycloak")
		install.ApplyVolumeMountSimple(container, "keycloak-providers", "/opt/jboss/keycloak/providers", false)
		install.ApplyVolumeMountSimple(container, "keycloak-configuration", "/opt/jboss/keycloak/standalone/configuration", false)
		install.ApplyVolumeMountSimple(container, "standard-authservice-cert", "/opt/enmasse/cert", false)
		install.ApplyEnvSimple(container, "KEYCLOAK_CONFIG_FILE", "standalone-"+string(authservice.Spec.Standard.Datasource.Type)+".xml")
	})
	install.ApplyContainer(deployment, "keycloak", func(container *corev1.Container) {
		install.ApplyContainerImage(container, "keycloak", authservice.Spec.Standard.Image)
		jvmOptions := "-Dvertx.cacheDirBase=/tmp -Djboss.bind.address=0.0.0.0 -Djava.net.preferIPv4Stack=true -Duser.timezone=UTC"
		if authservice.Spec.Standard.JvmOptions != nil {
			jvmOptions += " " + *authservice.Spec.Standard.JvmOptions
		} else if qty, ok := authservice.Spec.Standard.Resources.Requests["memory"]; ok {
			containerMemoryRequest := qty.ScaledValue(resource.Mega)
			jvmOptions += fmt.Sprintf(" -Xms%dm -Xmx%dm", containerMemoryRequest/2, containerMemoryRequest/2)
		}
		install.ApplyEnvSimple(container, "JAVA_OPTS", jvmOptions)
		install.ApplyEnvSecret(container, "KEYCLOAK_USER", "admin.username", authservice.Spec.Standard.CredentialsSecret.Name)
		install.ApplyEnvSecret(container, "KEYCLOAK_PASSWORD", "admin.password", authservice.Spec.Standard.CredentialsSecret.Name)

		if authservice.Spec.Standard.Datasource.Type == adminv1beta1.PostgresqlDatasource {
			install.ApplyEnvSimple(container, "DB_HOST", authservice.Spec.Standard.Datasource.Host)
			install.ApplyEnvSimple(container, "DB_PORT", fmt.Sprintf("%d", authservice.Spec.Standard.Datasource.Port))
			install.ApplyEnvSimple(container, "DB_DATABASE", authservice.Spec.Standard.Datasource.Database)
			install.ApplyEnvSecret(container, "DB_USERNAME", "database-user", authservice.Spec.Standard.Datasource.CredentialsSecret.Name)
			install.ApplyEnvSecret(container, "DB_PASSWORD", "database-password", authservice.Spec.Standard.Datasource.CredentialsSecret.Name)
		}

		container.Args = []string{"start-keycloak.sh", "-b", "0.0.0.0", "-c", "standalone-openshift.xml"}

		container.Ports = []corev1.ContainerPort{{
			ContainerPort: 5671,
			Name:          "amqps",
		}, {
			ContainerPort: 8443,
			Name:          "https",
		}}
		container.ReadinessProbe = &corev1.Probe{
			InitialDelaySeconds: 60,
			Handler: corev1.Handler{
				HTTPGet: &corev1.HTTPGetAction{
					Port:   intstr.FromString("https"),
					Path:   "/auth",
					Scheme: "HTTPS",
				},
			},
		}
		container.LivenessProbe = &corev1.Probe{
			InitialDelaySeconds: 120,
			Handler: corev1.Handler{
				HTTPGet: &corev1.HTTPGetAction{
					Port:   intstr.FromString("https"),
					Path:   "/auth",
					Scheme: "HTTPS",
				},
			},
		}
		if authservice.Spec.Standard.Resources != nil {
			container.Resources = *authservice.Spec.Standard.Resources
		}
		install.ApplyVolumeMountSimple(container, "keycloak-providers", "/opt/jboss/keycloak/providers", false)
		install.ApplyVolumeMountSimple(container, "keycloak-configuration", "/opt/jboss/keycloak/standalone/configuration", false)
		install.ApplyVolumeMountSimple(container, "keycloak-persistence", "/opt/jboss/keycloak/standalone/data", false)
		install.ApplyVolumeMountSimple(container, "standard-authservice-cert", "/opt/enmasse/cert", true)
	})

	install.ApplySecretVolume(deployment, "standard-authservice-cert", authservice.Spec.Standard.CertificateSecret.Name)
	install.ApplyEmptyDirVolume(deployment, "keycloak-providers")
	install.ApplyEmptyDirVolume(deployment, "keycloak-configuration")
	install.ApplyEmptyDirVolume(deployment, "keycloak-configuration")

	// Only allow setting volume on initial creation
	if util.IsNewObject(deployment) {
		if authservice.Spec.Standard.Storage.Type == adminv1beta1.PersistentClaim {
			install.ApplyPersistentVolume(deployment, "keycloak-persistence", *authservice.Spec.Standard.Storage.ClaimName)
		} else {
			install.ApplyEmptyDirVolume(deployment, "keycloak-persistence")
		}
	}

	if authservice.Spec.Standard.ServiceAccountName != nil {
		deployment.Spec.Template.Spec.ServiceAccountName = *authservice.Spec.Standard.ServiceAccountName
	} else {
		deployment.Spec.Template.Spec.ServiceAccountName = "standard-authservice"
	}
	deployment.Spec.Strategy = appsv1.DeploymentStrategy{
		Type: appsv1.RecreateDeploymentStrategyType,
	}
	return nil
}

func applyStandardAuthServiceService(authservice *adminv1beta1.AuthenticationService, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "standard-authservice", *authservice.Spec.Standard.ServiceName)
	service.Spec.Selector = install.CreateDefaultLabels(nil, "standard-authservice", *authservice.Spec.Standard.DeploymentName)

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}
	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = "standard-authservice-cert"
	service.Spec.Ports = []corev1.ServicePort{
		{
			Port:       8443,
			Protocol:   corev1.ProtocolTCP,
			TargetPort: intstr.FromString("https"),
			Name:       "https",
		},
		{
			Port:       5671,
			Protocol:   corev1.ProtocolTCP,
			TargetPort: intstr.FromString("amqps"),
			Name:       "amqps",
		},
	}
	return nil
}

func applyStandardAuthServiceVolume(authservice *adminv1beta1.AuthenticationService, pvc *corev1.PersistentVolumeClaim) error {

	install.ApplyDefaultLabels(&pvc.ObjectMeta, "standard-authservice", *authservice.Spec.Standard.Storage.ClaimName)

	// Only allow setting volume on initial creation
	if util.IsNewObject(pvc) {
		selector := authservice.Spec.Standard.Storage.Selector
		storageClassName := authservice.Spec.Standard.Storage.Class
		resources := corev1.ResourceRequirements{
			Requests: map[corev1.ResourceName]resource.Quantity{"storage": authservice.Spec.Standard.Storage.Size},
		}

		pvc.Spec.AccessModes = []corev1.PersistentVolumeAccessMode{
			corev1.ReadWriteOnce,
		}
		pvc.Spec.Selector = selector
		pvc.Spec.StorageClassName = storageClassName
		pvc.Spec.Resources = resources
	}
	return nil
}

func applyRoute(authservice *adminv1beta1.AuthenticationService, route *routev1.Route, caCertificate string) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "standard-authservice", *authservice.Spec.Standard.RouteName)

	route.Spec = routev1.RouteSpec{
		To: routev1.RouteTargetReference{
			Kind: "Service",
			Name: authservice.Name,
		},
		TLS: &routev1.TLSConfig{
			Termination:   routev1.TLSTerminationReencrypt,
			CACertificate: caCertificate,
		},
		Port: &routev1.RoutePort{
			TargetPort: intstr.FromString("https"),
		},
	}
	return nil
}

func applyOauthClient(authservice *adminv1beta1.AuthenticationService, oauth *oauthv1.OAuthClient, redirectUri string) error {
	install.ApplyDefaultLabels(&oauth.ObjectMeta, "oauthclient", oauth.Name)
	if oauth.Secret == "" {
		password, err := util.GeneratePassword(32)
		if err != nil {
			return err
		}
		oauth.Secret = password
	}
	oauth.GrantMethod = oauthv1.GrantHandlerAuto
	oauth.RedirectURIs = []string{
		redirectUri,
	}
	return nil
}

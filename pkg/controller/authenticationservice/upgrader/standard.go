/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package upgrader

import (
	"context"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	v12 "github.com/openshift/api/apps/v1"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

func tryUpgradeExistingStandardAuthService(ctx context.Context, r *UpgradeAuthenticationService) error {

	deployment := &appsv1.Deployment{}
	deploymentName := types.NamespacedName{Name: "keycloak", Namespace: r.namespace}
	err := r.client.Get(ctx, deploymentName, deployment)

	if err != nil {
		if errors.IsNotFound(err) {
			log.Error(err, "No existing keycloak deployment found", "NamespacedName", deploymentName.String())
			return nil
		} else {
			return err
		}
	}

	deploymentTemplateSpec := deployment.Spec.Template.Spec
	pvcClaimName := findFirstPVCClaimName(deploymentTemplateSpec)

	pvc := &corev1.PersistentVolumeClaim{}
	if pvcClaimName != nil {
		pvcName := types.NamespacedName{Name: *pvcClaimName, Namespace: r.namespace}
		err = r.client.Get(ctx, pvcName, pvc)
		if err != nil {
			if errors.IsNotFound(err) {
				log.Info("No existing keycloak pvc found", "NamespacedName", pvcName.String())
				pvc = nil
			} else {
				return err
			}
		} else {
			log.Info("Found existing keycloak pvc", "NamespacedName", pvcName.String())
		}

	} else {
		pvc = nil
	}

	standardauthService := &corev1.Service{}
	standardauthServiceName := types.NamespacedName{Name: "standard-authservice", Namespace: r.namespace}
	err = r.client.Get(ctx, standardauthServiceName, standardauthService)

	if err != nil {
		if errors.IsNotFound(err) {
			log.Info("No existing standardauth service found", "NamespacedName", standardauthServiceName.String())
			standardauthService = nil
		} else {
			return err
		}
	} else {
		log.Info("Found existing standardauth service", "NamespacedName", standardauthServiceName.String())
	}

	postgresqlSecret := &corev1.Secret{}
	postgresService := &corev1.Service{}
	postgresDeploymentConfig := &v12.DeploymentConfig{}
	postgresqlContainer := &corev1.Container{}
	if util.IsOpenshift() {
		postgresqlSecretName := types.NamespacedName{Name: "postgresql", Namespace: r.namespace}
		err = r.client.Get(ctx, postgresqlSecretName, postgresqlSecret)
		if err != nil {
			if errors.IsNotFound(err) {
				log.Info("No existing postgresql secret found", "NamespacedName", postgresqlSecretName.String())
				postgresqlSecret = nil
			} else {
				return err
			}
		} else {
			log.Info("Found existing postgresql secret", "NamespacedName", postgresqlSecretName.String())
		}

		postgresServiceName := types.NamespacedName{Name: "postgresql", Namespace: r.namespace}
		err = r.client.Get(ctx, postgresServiceName, postgresService)

		if err != nil {
			if errors.IsNotFound(err) {
				log.Info("No existing postgresql service found", "NamespacedName", postgresServiceName.String())
				postgresService = nil
			} else {
				return err
			}
		} else {
			log.Info("Found existing postgresql service", "NamespacedName", postgresServiceName.String())
		}

		postgresDeploymentConfigName := types.NamespacedName{Name: "postgresql", Namespace: r.namespace}
		err = r.client.Get(ctx, postgresDeploymentConfigName, postgresDeploymentConfig)

		if err != nil {
			if errors.IsNotFound(err) {
				log.Info("No existing postgresql deploymentconfig found", "NamespacedName", postgresServiceName.String())
				postgresDeploymentConfig = nil
			} else {
				return err
			}
		} else {
			log.Info("Found existing postgresql deploymentconfig", "NamespacedName", postgresServiceName.String())
			if len(postgresDeploymentConfig.Spec.Template.Spec.Containers) > 0 {
				postgresqlContainer = &postgresDeploymentConfig.Spec.Template.Spec.Containers[0]
			}
		}

	} else {
		postgresqlSecret = nil
		postgresService = nil
		postgresDeploymentConfig = nil
		postgresqlContainer = nil
	}

	var keycloakContainer *corev1.Container
	if len(deploymentTemplateSpec.Containers) > 0 {
		for _, container := range deploymentTemplateSpec.Containers {
			if container.Name == "keycloak" {
				keycloakContainer = &container
				break
			}
		}
	}

	if keycloakContainer == nil {
		log.Info("Couldn't find keycloak container within existing deployment", "deployment", deployment)
		return nil
	}

	meta := v1.ObjectMeta{Namespace: r.namespace, Name: "standard-authservice"}
	authservice := &adminv1beta1.AuthenticationService{
		ObjectMeta: meta,
	}

	meta.Labels = make(map[string]string)
	meta.Labels["app"] = "enmasse"

	authservice.Spec.Type = adminv1beta1.Standard
	authservice.Spec.Standard = &adminv1beta1.AuthenticationServiceSpecStandard{}

	authservice.Spec.Standard.DeploymentName = &deployment.Name
	authservice.Spec.Standard.CredentialsSecret = &corev1.SecretReference{Name: "keycloak-credentials"}

	if standardauthService != nil {
		authservice.Spec.Standard.ServiceName = &standardauthService.Name
	}

	if pvc != nil {
		if claimQuantity, ok := pvc.Spec.Resources.Requests["storage"]; ok {
			authservice.Spec.Standard.Storage = &adminv1beta1.AuthenticationServiceSpecStandardStorage{
				Type:      adminv1beta1.PersistentClaim,
				ClaimName: &pvc.Name,
				Size:      claimQuantity,
			}
		}
	}

	if keycloakContainer.Resources.Requests != nil || keycloakContainer.Resources.Limits != nil {
		authservice.Spec.Standard.Resources = &keycloakContainer.Resources
	}

	authservice.Spec.Standard.Datasource = &adminv1beta1.AuthenticationServiceSpecStandardDatasource{
		Type: adminv1beta1.H2Datasource,
	}

	if postgresService != nil && postgresqlContainer != nil {
		configurePostgresqlDatasource(ctx, postgresService, authservice.Spec.Standard.Datasource, keycloakContainer, postgresqlContainer, r)
	}

	_, err = controllerutil.CreateOrUpdate(ctx, r.client, authservice, func(existing runtime.Object) error {
		return nil
	})

	if err != nil {
		log.Error(err, "Failed to create authenticationservice record", "authenticationservice", authservice)
	} else {
		log.Info("Successfully upgraded existing authentication service", "authenticationservice", authservice)
	}

	return nil
}

func configurePostgresqlDatasource(ctx context.Context, postgresService *corev1.Service, datasource *adminv1beta1.AuthenticationServiceSpecStandardDatasource, keycloakContainer *corev1.Container, postgresqlContainer *corev1.Container, r *UpgradeAuthenticationService) error {

	isSecretKey := func(s corev1.EnvVar) bool {
		return s.ValueFrom != nil && s.ValueFrom.SecretKeyRef != nil
	}
	all := func(s corev1.EnvVar) bool { return true }

	dbUserName := findEnvVar(keycloakContainer, "DB_USERNAME", isSecretKey)
	dbPassword := findEnvVar(keycloakContainer, "DB_PASSWORD", isSecretKey)
	dbDatabase := findEnvVar(keycloakContainer, "DB_DATABASE", all)

	if dbUserName == nil || dbPassword == nil || dbDatabase == nil {
		log.Info("Could not extract existing database details from keycloak deployment")
		return nil
	}

	pdbUserName := findEnvVar(postgresqlContainer, "POSTGRESQL_USER", isSecretKey)
	pdbPassword := findEnvVar(postgresqlContainer, "POSTGRESQL_PASSWORD", isSecretKey)
	pdbDatabase := findEnvVar(postgresqlContainer, "POSTGRESQL_DATABASE", all)

	if pdbUserName == nil || pdbPassword == nil || pdbDatabase == nil {
		log.Info("Could not extract existing database details from postgresql deploymentconfig")
		return nil
	}

	if dbUserName.ValueFrom.SecretKeyRef.Name == pdbUserName.ValueFrom.SecretKeyRef.Name &&
		dbPassword.ValueFrom.SecretKeyRef.Name == pdbPassword.ValueFrom.SecretKeyRef.Name &&
		dbUserName.ValueFrom.SecretKeyRef.Name == dbPassword.ValueFrom.SecretKeyRef.Name {
		datasource.CredentialsSecret = corev1.SecretReference{
			Name: dbUserName.ValueFrom.SecretKeyRef.Name,
		}
	} else {
		// TODO could handle the cases where the environment variables refer to different secrets (or don't refer to secrets at all) by creating a new secret.
		// probably not going to need these cases.
		log.Info("Existing database username/password details on postgresql deploymentconfig and keycloak deployment refer to different secrets, " +
			"can't set datasource.CredentialsSecret automatically")
	}

	if dbDatabase.Value != "" && dbDatabase.Value == pdbDatabase.Value {
		datasource.Database = dbDatabase.Value
	} else if dbDatabase.ValueFrom.SecretKeyRef != nil &&
		dbDatabase.ValueFrom.SecretKeyRef.Name == pdbDatabase.ValueFrom.SecretKeyRef.Name {
		secretKeyRef := dbDatabase.ValueFrom.SecretKeyRef
		key := client.ObjectKey{Namespace: r.namespace, Name: secretKeyRef.Name}
		secret := &corev1.Secret{}
		err := r.client.Get(ctx, key, secret)
		if err != nil {
			if errors.IsNotFound(err) {
				log.Info("Existing secret carrying database-name not found,"+
					"can't set datasource.Database automatically", "NamespacedName", secretKeyRef.String())
			} else {
				return err
			}
		} else {
			if db, ok := secret.Data["database-name"]; ok {
				datasource.Database = string(db)
			} else {
				log.Info("Can't find key 'database-name' within secret, "+
					"can't set datasource.Database automatically", "NamespacedName", secretKeyRef.String())
			}
		}
	} else {
		log.Info("Existing database database-name details on postgresql deploymentconfig and keycloak deployment refer to different secrets, " +
			"can't set datasource.Database automatically")
	}

	datasource.Host = postgresService.Name
	if len(postgresService.Spec.Ports) > 0 {
		datasource.Port = int(postgresService.Spec.Ports[0].Port)
	}

	datasource.Type = adminv1beta1.PostgresqlDatasource
	return nil
}

func findFirstPVCClaimName(deploymentTemplateSpec corev1.PodSpec) *string {
	for _, item := range deploymentTemplateSpec.Volumes {
		if item.PersistentVolumeClaim != nil && item.PersistentVolumeClaim.ClaimName != "" {
			return &item.PersistentVolumeClaim.ClaimName
		}
	}
	return nil
}

type envPredicate func(endVar corev1.EnvVar) bool

func findEnvVar(c *corev1.Container, name string, pred envPredicate) *corev1.EnvVar {

	for _, env := range c.Env {
		if env.Name == name && pred(env) {
			return &env
		}
	}

	return nil
}

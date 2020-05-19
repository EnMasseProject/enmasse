/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	enmasseapi "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"log"
	"reflect"
	"strings"
	"time"

	"os"

	"github.com/fabric8io/kubernetes-client/kubernetes-model/pkg/schemagen"
)

type Schema struct {
	MessagingInfra        enmasseapi.MessagingInfrastructure
	MessagingInfraList    enmasseapi.MessagingInfrastructureList
	MessagingTenant       enmasseapi.MessagingTenant
	MessagingTenantList   enmasseapi.MessagingTenantList
	MessagingAddress      enmasseapi.MessagingAddress
	MessagingAddressList  enmasseapi.MessagingAddressList
	MessagingEndpoint     enmasseapi.MessagingEndpoint
	MessagingEndpointList enmasseapi.MessagingEndpointList
}

func main() {
	customTypeNames := map[string]string{
		"K8sSubjectAccessReview":       "SubjectAccessReview",
		"K8sLocalSubjectAccessReview":  "LocalSubjectAccessReview",
		"JSONSchemaPropsorStringArray": "JSONSchemaPropsOrStringArray",
	}
	packages := []schemagen.PackageDescriptor{
		{"k8s.io/api/core/v1", "", "io.fabric8.kubernetes.api.model", "kubernetes_core_"},
		{"k8s.io/apimachinery/pkg/api/resource", "", "io.fabric8.kubernetes.api.model", "kubernetes_resource_"},
		{"k8s.io/apimachinery/pkg/util/intstr", "", "io.fabric8.kubernetes.api.model", "kubernetes_apimachinery_pkg_util_intstr_"},
		{"k8s.io/apimachinery/pkg/runtime", "", "io.fabric8.kubernetes.api.model.runtime", "kubernetes_apimachinery_pkg_runtime_"},
		{"k8s.io/apimachinery/pkg/version", "", "io.fabric8.kubernetes.api.model.version", "kubernetes_apimachinery_pkg_version_"},
		{"k8s.io/apimachinery/pkg/apis/meta/v1", "", "io.fabric8.kubernetes.api.model.apis", "kubernetes_apimachinery_pkg_apis_"},
		{"k8s.io/kubernetes/pkg/util", "", "io.fabric8.kubernetes.api.model", "kubernetes_util_"},
		{"k8s.io/kubernetes/pkg/watch/json", "", "io.fabric8.kubernetes.api.model", "kubernetes_watch_"},
		{"k8s.io/kubernetes/pkg/api/errors", "", "io.fabric8.kubernetes.api.model", "kubernetes_errors_"},
		{"k8s.io/client-go/tools/clientcmd/api/v1", "", "io.fabric8.kubernetes.api.model", "kubernetes_config_"},
		{"k8s.io/client-go/tools/leaderelection", "", "io.fabric8.kubernetes.api.model.leaderelection", "kubernetes_leaderelection_"},
		{"k8s.io/client-go/tools/leaderelection/resourcelock", "", "io.fabric8.kubernetes.api.model.leaderelection.resourcelock", "kubernetes_leaderelection_resourcelock_"},
		{"github.com/openshift/api/build/v1", "", "io.fabric8.openshift.api.model", "os_build_"},
		{"github.com/openshift/api/apps/v1", "", "io.fabric8.openshift.api.model", "os_deploy_"},
		{"github.com/openshift/api/image/v1", "", "io.fabric8.openshift.api.model", "os_image_"},
		{"github.com/openshift/api/oauth/v1", "", "io.fabric8.openshift.api.model", "os_oauth_"},
		{"github.com/openshift/api/route/v1", "", "io.fabric8.openshift.api.model", "os_route_"},
		{"github.com/openshift/api/template/v1", "", "io.fabric8.openshift.api.model", "os_template_"},
		{"github.com/openshift/api/user/v1", "", "io.fabric8.openshift.api.model", "os_user_"},
		{"github.com/openshift/api/authorization/v1", "", "io.fabric8.openshift.api.model", "os_authorization_"},
		{"github.com/openshift/api/project/v1", "", "io.fabric8.openshift.api.model", "os_project_"},
		{"github.com/openshift/api/security/v1", "", "io.fabric8.openshift.api.model", "os_security_"},
		{"github.com/openshift/api/network/v1", "", "io.fabric8.openshift.api.model", "os_network_"},
		{"k8s.io/kubernetes/pkg/api/unversioned", "", "io.fabric8.kubernetes.api.model", "api_"},
		{"k8s.io/api/discovery/v1beta1", "", "io.fabric8.kubernetes.api.model.discovery", "kubernetes_discovery_"},
		{"k8s.io/api/extensions/v1beta1", "", "io.fabric8.kubernetes.api.model.extensions", "kubernetes_extensions_"},
		{"k8s.io/api/policy/v1beta1", "", "io.fabric8.kubernetes.api.model.policy", "kubernetes_policy_"},
		{"k8s.io/api/authentication/v1", "authentication.k8s.io", "io.fabric8.kubernetes.api.model.authentication", "kubernetes_authentication_"},
		{"k8s.io/api/authorization/v1", "authorization.k8s.io", "io.fabric8.kubernetes.api.model.authorization", "kubernetes_authorization_"},
		{"k8s.io/api/apps/v1", "", "io.fabric8.kubernetes.api.model.apps", "kubernetes_apps_"},
		{"k8s.io/api/apps/v1beta1", "", "io.fabric8.kubernetes.api.model.apps.v1beta1", "kubernetes_apps_v1beta1_"},
		{"k8s.io/api/batch/v1beta1", "", "io.fabric8.kubernetes.api.model.batch", "kubernetes_batch_"},
		{"k8s.io/api/batch/v1", "", "io.fabric8.kubernetes.api.model.batch", "kubernetes_batch_"},
		{"k8s.io/api/autoscaling/v2beta2", "autoscaling", "io.fabric8.kubernetes.api.model", "kubernetes_autoscaling_"},
		{"k8s.io/api/autoscaling/v1", "autoscaling", "io.fabric8.kubernetes.api.model.v1", "kubernetes_autoscaling_v1_"},
		{"k8s.io/apiextensions-apiserver/pkg/apis/apiextensions/v1beta1", "", "io.fabric8.kubernetes.api.model.apiextensions", "kubernetes_apiextensions_"},
		{"k8s.io/apimachinery/pkg/apis/meta/v1", "", "io.fabric8.kubernetes.api.model", "kubernetes_apimachinery_"},
		{"k8s.io/api/networking/v1", "networking.k8s.io", "io.fabric8.kubernetes.api.model.networking", "kubernetes_networking_"},
		{"k8s.io/api/storage/v1", "storage.k8s.io", "io.fabric8.kubernetes.api.model.storage", "kubernetes_storageclass_"},
		{"k8s.io/api/storage/v1beta1", "storage.k8s.io", "io.fabric8.kubernetes.api.model.storage.v1beta1", "kubernetes_storageclass_v1beta1_"},
		{"k8s.io/api/rbac/v1", "rbac.authorization.k8s.io", "io.fabric8.kubernetes.api.model.rbac", "kubernetes_rbac_"},
		{"k8s.io/api/settings/v1alpha1", "settings.k8s.io", "io.fabric8.kubernetes.api.model.settings", "kubernetes_settings_"},
		{"k8s.io/api/scheduling/v1beta1", "scheduling.k8s.io", "io.fabric8.kubernetes.api.model.scheduling", "kubernetes_scheduling_"},
		{"k8s.io/api/events/v1beta1", "events.k8s.io", "io.fabric8.kubernetes.api.model.events", "kubernetes_events_"},
		{"k8s.io/api/admission/v1beta1", "admission.k8s.io", "io.fabric8.kubernetes.api.model.admission", "kubernetes_admission_"},
		{"k8s.io/api/admissionregistration/v1beta1", "admissionregistration.k8s.io", "io.fabric8.kubernetes.api.model.admissionregistration", "kubernetes_admissionregistration_"},
		{"k8s.io/api/certificates/v1beta1", "certificates.k8s.io", "io.fabric8.kubernetes.api.model.certificates", "kubernetes_certificates_"},
		{"k8s.io/api/coordination/v1", "coordination.k8s.io", "io.fabric8.kubernetes.api.model.coordination.v1", "kubernetes_coordination_"},
		{"k8s.io/metrics/pkg/apis/metrics/v1beta1", "metrics.k8s.io", "io.fabric8.kubernetes.api.model.metrics.v1beta1", "kubernetes_metrics_v1beta1_"},
		// EnMasse model
		{"github.com/enmasseproject/pkg/apis/enmasse/v1beta1", "", "io.enmasse.api.model", "enmasse_model_"},
		{"github.com/enmasseproject/pkg/apis/enmasse/v1beta2", "", "io.enmasse.api.model", "enmasse_model_"},
	}

	typeMap := map[reflect.Type]reflect.Type{
		reflect.TypeOf(time.Time{}): reflect.TypeOf(""),
		reflect.TypeOf(struct{}{}):  reflect.TypeOf(""),
	}
	schema, err := schemagen.GenerateSchema(reflect.TypeOf(Schema{}), packages, typeMap, customTypeNames)
	if err != nil {
		fmt.Fprintf(os.Stderr, "An error occurred: %v", err)
		return
	}

	args := os.Args[1:]
	if len(args) < 1 || args[0] != "validation" {
		schema.Resources = nil
	}

	b, err := json.Marshal(&schema)
	if err != nil {
		log.Fatal(err)
	}
	result := string(b)
	result = strings.Replace(result, "\"additionalProperty\":", "\"additionalProperties\":", -1)

	/**
	 * Hack to fix https://github.com/fabric8io/kubernetes-client/issues/1565
	 *
	 * Right now enums are having body as array of jsons rather than being array of strings.
	 * (See https://user-images.githubusercontent.com/13834498/59852204-00d25680-938c-11e9-91b6-74f6bc3ae65b.png)
	 *
	 * I could not find any other way of fixing this since I'm not sure where it's coming from.
	 * So doing this search and replace of whole enum json object block hence converting it to an array of plain
	 * strings rather than of json objects.
	 */
	result = strings.Replace(result, "\"enum\":{\"type\":\"array\",\"description\":\"\",\"javaOmitEmpty\":true,\"items\":{\"$ref\":\"#/definitions/kubernetes_apiextensions_JSON\",\"javaType\":\"io.fabric8.kubernetes.api.model.apiextensions.JSON\"}},",
		"\"enum\":{\"type\":\"array\",\"description\":\"\",\"javaOmitEmpty\":true,\"items\":{\"type\": \"string\"}},", -1)

	// This replacements are needed to avoid generating code for existing types
	result = strings.Replace(result, "\"javaType\":\"io.fabric8", "\"existingJavaType\":\"io.fabric8", -1)
	result = strings.Replace(result, "\"javaType\":\"java.util", "\"existingJavaType\":\"java.util", -1)
	result = strings.Replace(result, "\"javaType\":\"java.lang", "\"existingJavaType\":\"java.lang", -1)
	result = strings.Replace(result, "\"javaType\":\"String", "\"existingJavaType\":\"java.lang.String", -1)

	// Replace api version for now
	result = strings.Replace(result, "enmasse/v1beta2", "enmasse.io/v1beta2", -1)

	var out bytes.Buffer
	err = json.Indent(&out, []byte(result), "", "  ")
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println(out.String())
}

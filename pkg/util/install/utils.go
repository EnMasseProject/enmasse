/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"os"
	"sort"
	"strings"

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/apiutil"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/images"

	appsv1 "k8s.io/api/apps/v1"
	batchv1 "k8s.io/api/batch/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)


const ServingCertSecretNameAlpha = "service.alpha.openshift.io/serving-cert-secret-name"
const ServingCertSecretNameBeta = "service.beta.openshift.io/serving-cert-secret-name"

func CreateDefaultLabels(labels map[string]string, component string, name string) map[string]string {

	if labels == nil {
		labels = make(map[string]string)
	}

	labels["component"] = component
	labels["app"] = "enmasse"
	labels["name"] = name
	labels["app.kubernetes.io/part-of"] = component

	return labels
}

// Apply a custom label
func ApplyCustomLabel(meta *v1.ObjectMeta, key string, value string) {
	meta.Labels[key] = value
}

// Apply standard set of labels
func ApplyDefaultLabels(meta *v1.ObjectMeta, component string, name string) {
	meta.Labels = CreateDefaultLabels(meta.Labels, component, name)
}

// Apply some default service values
func ApplyServiceDefaults(service *corev1.Service, component string, name string) {

	ApplyDefaultLabels(&service.ObjectMeta, component, name)
	service.Spec.Selector = CreateDefaultLabels(nil, component, name)

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

}

func ApplyMetricsServiceDefaults(service *corev1.Service, component string, name string) {

	ApplyDefaultLabels(&service.ObjectMeta, component, name+"-metrics")
	service.Spec.Selector = CreateDefaultLabels(nil, component, name)

}

func ApplyOpenShiftServingCertAnnotation(annotations map[string]string, certName string, isOpenShift func() bool, isOpenShift4 func() bool) {
	if isOpenShift() {
		keys := make([]string, 0)
		if isOpenShift4() {
			keys = append(keys, ServingCertSecretNameBeta)
		}
		keys = append(keys, ServingCertSecretNameAlpha)

		for _, k := range keys {
			if v, okay := annotations[k]; okay && v == certName {
				return;
			}
		}

		annotations[keys[0]] = certName
		for _, k := range keys[1:] {
			delete(annotations,k)
		}
	}
}

func ApplyAnnotation(meta *v1.ObjectMeta, key string, value string) {

	if meta.Annotations == nil {
		meta.Annotations = make(map[string]string)
	}
	meta.Annotations[key] = value
}


func CreateDefaultAnnotations(annotations map[string]string) map[string]string {

	// currently we only ensure the annotations map is not null

	if annotations == nil {
		annotations = make(map[string]string)
	}

	if version, ok := os.LookupEnv("VERSION"); ok {
		annotations["enmasse.io/version"] = version
	}

	if revision, ok := os.LookupEnv("REVISION"); ok {
		annotations["enmasse.io/revision"] = revision
	}

	return annotations

}

// Apply some default deployment values
func ApplyDeploymentDefaults(deployment *appsv1.Deployment, component string, name string) {

	ApplyDefaultLabels(&deployment.ObjectMeta, component, name)

	if deployment.CreationTimestamp.IsZero() {
		deployment.Spec.Selector = &v1.LabelSelector{
			MatchLabels: CreateDefaultLabels(nil, component, name),
		}
	}

	if deployment.Annotations == nil {
		deployment.Annotations = map[string]string{}
	}

	replicas := int32(1)
	deployment.Spec.Replicas = &replicas

	deployment.Spec.Template.Annotations = CreateDefaultAnnotations(deployment.Spec.Template.Annotations)
	deployment.Spec.Template.Labels = CreateDefaultLabels(deployment.Spec.Template.Labels, component, name)

}

// Apply some default deployment values
func ApplyStatefulSetDefaults(statefulset *appsv1.StatefulSet, component string, name string) {

	ApplyDefaultLabels(&statefulset.ObjectMeta, component, name)

	if statefulset.CreationTimestamp.IsZero() {
		statefulset.Spec.Selector = &v1.LabelSelector{
			MatchLabels: CreateDefaultLabels(nil, component, name),
		}
	}

	if statefulset.Annotations == nil {
		statefulset.Annotations = map[string]string{}
	}

	replicas := int32(1)
	statefulset.Spec.Replicas = &replicas

	statefulset.Spec.Template.Annotations = CreateDefaultAnnotations(statefulset.Spec.Template.Annotations)
	statefulset.Spec.Template.Labels = CreateDefaultLabels(statefulset.Spec.Template.Labels, component, name)
}

func DropContainer(deployment *appsv1.Deployment, name string) {
	if deployment.Spec.Template.Spec.Containers == nil {
		return
	}

	// removing an entry from an array in go is tricky ...

	for i := len(deployment.Spec.Template.Spec.Containers) - 1; i >= 0; i-- {
		c := deployment.Spec.Template.Spec.Containers[i]
		if c.Name == name {
			deployment.Spec.Template.Spec.Containers = append(
				deployment.Spec.Template.Spec.Containers[:i],
				deployment.Spec.Template.Spec.Containers[i+1:]...,
			)
		}
	}
}

// Delete all containers which are not expected
func DeleteOtherContainers(containers []corev1.Container, prefix string, expectedNames []string) []corev1.Container {

	sort.Strings(expectedNames)

	result := make([]corev1.Container, 0, len(containers))

	for _, c := range containers {
		if len(prefix) > 0 && !strings.HasPrefix(c.Name, prefix) {
			result = append(result, c)
			continue
		}
		if !util.ContainsString(expectedNames, c.Name) {
			continue
		}

		result = append(result, c)
	}

	return result

}

func ApplyContainerWithError(containers []corev1.Container, name string, mutator func(*corev1.Container) error) ([]corev1.Container, error) {

	if containers == nil {
		containers = make([]corev1.Container, 0)
	}

	for i, c := range containers {
		if c.Name == name {
			err := mutator(&containers[i])
			return containers, err
		}
	}

	c := &corev1.Container{
		Name: name,
	}

	err := mutator(c)
	if err == nil {
		containers = append(containers, *c)
	}

	return containers, err
}

func ApplyDeploymentContainerWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container) error) error {
	containers, err := ApplyContainerWithError(deployment.Spec.Template.Spec.Containers, name, mutator)

	if err == nil {
		deployment.Spec.Template.Spec.Containers = containers
	}

	return err
}

func ApplyStatefulSetContainerWithError(statefulSet *appsv1.StatefulSet, name string, mutator func(*corev1.Container) error) error {
	containers, err := ApplyContainerWithError(statefulSet.Spec.Template.Spec.Containers, name, mutator)

	if err == nil {
		statefulSet.Spec.Template.Spec.Containers = containers
	}

	return err
}

func ApplyInitContainerWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container) error) error {
	containers, err := ApplyContainerWithError(deployment.Spec.Template.Spec.InitContainers, name, mutator)

	if err == nil {
		deployment.Spec.Template.Spec.InitContainers = containers
	}

	return err
}

func ApplyJobContainerWithError(job *batchv1.Job, name string, mutator func(*corev1.Container) error) error {
	containers, err := ApplyContainerWithError(job.Spec.Template.Spec.Containers, name, mutator)

	if err == nil {
		job.Spec.Template.Spec.Containers = containers
	}

	return err
}

func ApplyPersistentVolume(pod *corev1.PodSpec, name string, claimName string) {
	ApplyVolume(pod, name, func(volume *corev1.Volume) {
		if volume.PersistentVolumeClaim == nil {
			volume.PersistentVolumeClaim = &corev1.PersistentVolumeClaimVolumeSource{}
		}
		volume.PersistentVolumeClaim.ClaimName = claimName
	})
}

func ApplyConfigMapVolume(pod *corev1.PodSpec, name string, configMapName string) {
	ApplyConfigMapVolumeItems(pod, name, configMapName, nil)
}

func ApplyConfigMapVolumeItems(pod *corev1.PodSpec, name string, configMapName string, items []corev1.KeyToPath) {
	ApplyVolume(pod, name, func(volume *corev1.Volume) {
		if volume.ConfigMap == nil {
			volume.ConfigMap = &corev1.ConfigMapVolumeSource{}
		}
		volume.ConfigMap.Name = configMapName
		volume.ConfigMap.Items = items
	})
}

func ApplySecretVolume(pod *corev1.PodSpec, name string, secretName string) {
	ApplyVolume(pod, name, func(volume *corev1.Volume) {
		if volume.Secret == nil {
			volume.Secret = &corev1.SecretVolumeSource{}
		}
		volume.Secret.SecretName = secretName
	})
}

func ApplyEmptyDirVolume(pod *corev1.PodSpec, name string) {
	ApplyVolume(pod, name, func(volume *corev1.Volume) {
		if volume.EmptyDir == nil {
			volume.EmptyDir = &corev1.EmptyDirVolumeSource{}
		}
	})
}

func ApplyVolume(pod *corev1.PodSpec, name string, mutator func(*corev1.Volume)) {
	// call "with error", and eat up the error
	_ = ApplyVolumeWithError(pod, name, func(volume *corev1.Volume) error {
		mutator(volume)
		return nil
	})
}

func ApplyVolumeWithError(pod *corev1.PodSpec, name string, mutator func(*corev1.Volume) error) error {

	if pod.Volumes == nil {
		pod.Volumes = make([]corev1.Volume, 0)
	}

	for i, c := range pod.Volumes {
		if c.Name == name {
			return mutator(&pod.Volumes[i])
		}
	}

	v := &corev1.Volume{
		Name: name,
	}

	err := mutator(v)
	if err == nil {
		pod.Volumes = append(pod.Volumes, *v)
	}

	return err
}

func DropVolume(pod *corev1.PodSpec, name string) {
	if pod.Volumes == nil {
		return
	}

	// removing an entry from an array in go is tricky ...

	for i := len(pod.Volumes) - 1; i >= 0; i-- {
		v := pod.Volumes[i]
		if v.Name == name {
			pod.Volumes = append(
				pod.Volumes[:i],
				pod.Volumes[i+1:]...,
			)
		}
	}
}

func ApplyContainerImage(container *corev1.Container, imageName string, override *v1beta1.ImageOverride) error {

	resolved, err := images.GetImage(imageName)
	if err != nil {
		return err
	}

	var pullPolicy corev1.PullPolicy

	if override != nil {
		if override.Name != "" {
			resolved = override.Name
		}
		if override.PullPolicy != "" {
			pullPolicy = override.PullPolicy
		}
	}

	container.Image = resolved
	if pullPolicy != "" {
		container.ImagePullPolicy = pullPolicy
	} else {
		container.ImagePullPolicy = images.PullPolicyFromImageName(resolved)
	}

	return nil
}

func SetContainerImage(container *corev1.Container, imageName string, overrides v1beta1.ImageOverridesProvider) error {

	resolved, err := images.GetImage(imageName)
	if err != nil {
		return err
	}

	var pullPolicy corev1.PullPolicy

	overrideMap := overrides.GetImageOverrides()
	if overrideMap != nil {
		val, ok := overrideMap[imageName]
		if ok {
			if val.Name != "" {
				resolved = val.Name
			}
			if val.PullPolicy != "" {
				pullPolicy = val.PullPolicy
			}
		}
	}

	container.Image = resolved
	if pullPolicy != "" {
		container.ImagePullPolicy = pullPolicy
	} else {
		container.ImagePullPolicy = images.PullPolicyFromImageName(resolved)
	}

	return nil
}

// Apply a simple HTTP probe
func ApplyHttpProbe(probe *corev1.Probe, initialDelaySeconds int32, path string, port uint16) *corev1.Probe {
	if probe == nil {
		probe = &corev1.Probe{}
	}

	probe.InitialDelaySeconds = initialDelaySeconds
	probe.Exec = nil
	probe.TCPSocket = nil
	probe.HTTPGet = &corev1.HTTPGetAction{
		Path:   path,
		Port:   intstr.FromInt(int(port)),
		Scheme: corev1.URISchemeHTTP,
	}

	return probe
}

func ApplyVolumeMountWithError(container *corev1.Container, name string, mutator func(mount *corev1.VolumeMount) error) error {

	if container.VolumeMounts == nil {
		container.VolumeMounts = make([]corev1.VolumeMount, 0)
	}

	for i, v := range container.VolumeMounts {
		if v.Name == name {
			return mutator(&container.VolumeMounts[i])
		}
	}

	v := &corev1.VolumeMount{
		Name: name,
	}

	err := mutator(v)
	if err == nil {
		container.VolumeMounts = append(container.VolumeMounts, *v)
	}

	return err
}

func ApplyVolumeMount(container *corev1.Container, name string, mutator func(mount *corev1.VolumeMount)) {
	// call "with error", and eat up the error
	_ = ApplyVolumeMountWithError(container, name, func(mount *corev1.VolumeMount) error {
		mutator(mount)
		return nil
	})
}

func ApplyVolumeMountSimple(container *corev1.Container, name string, path string, readOnly bool) {
	ApplyVolumeMount(container, name, func(mount *corev1.VolumeMount) {
		mount.MountPath = path
		mount.ReadOnly = readOnly
	})
}

func DropVolumeMount(container *corev1.Container, name string) {
	if container.VolumeMounts == nil {
		return
	}

	// removing an entry from an array in go is tricky ...

	for i := len(container.VolumeMounts) - 1; i >= 0; i-- {
		v := container.VolumeMounts[i]
		if v.Name == name {
			container.VolumeMounts = append(
				container.VolumeMounts[:i], container.VolumeMounts[i+1:]...,
			)
		}
	}
}

func ApplyEnvWithError(container *corev1.Container, name string, mutator func(envvar *corev1.EnvVar) error) error {

	if container.Env == nil {
		container.Env = make([]corev1.EnvVar, 0)
	}

	for i, e := range container.Env {
		if e.Name == name {
			return mutator(&container.Env[i])
		}
	}

	e := &corev1.EnvVar{
		Name: name,
	}

	err := mutator(e)
	if err == nil {
		container.Env = append(container.Env, *e)
	}

	return err
}

func ApplyEnv(container *corev1.Container, name string, mutator func(envvar *corev1.EnvVar)) {
	_ = ApplyEnvWithError(container, name, func(envvar *corev1.EnvVar) error {
		mutator(envvar)
		return nil
	})
}

func ApplyEnvSimple(container *corev1.Container, name string, value string) {
	ApplyEnv(container, name, func(envvar *corev1.EnvVar) {
		envvar.ValueFrom = nil
		envvar.Value = value
	})
}

func ApplyOrRemoveEnvSimple(container *corev1.Container, name string, value string) {
	if value != "" {
		ApplyEnvSimple(container, name, value)
	} else {
		RemoveEnv(container, name)
	}
}

func FromFieldNamespace() *corev1.EnvVarSource {
	return FromField("metadata.namespace")
}

func FromField(fieldPath string) *corev1.EnvVarSource {
	return &corev1.EnvVarSource{
		FieldRef: &corev1.ObjectFieldSelector{
			FieldPath: fieldPath,
		},
	}
}

func FromSecret(secretName string, secretKey string) *corev1.EnvVarSource {
	return FromOptionalSecret(secretName, secretKey, nil)
}

func FromOptionalSecret(secretName string, secretKey string, optional *bool) *corev1.EnvVarSource {
	return &corev1.EnvVarSource{
		SecretKeyRef: &corev1.SecretKeySelector{
			Key: secretKey,
			LocalObjectReference: corev1.LocalObjectReference{
				Name: secretName,
			},
			Optional: optional,
		},
	}
}

func ApplyEnvSecret(container *corev1.Container, name string, secretKey string, secretName string) {
	ApplyEnvOptionalSecret(container, name, secretKey, secretName, nil)
}

func ApplyEnvOptionalSecret(container *corev1.Container, name string, secretKey string, secretName string, optional *bool) {
	ApplyEnv(container, name, func(envvar *corev1.EnvVar) {
		envvar.Value = ""
		envvar.ValueFrom = FromOptionalSecret(secretName, secretKey, optional)
	})
}

func FromConfigMap(configMapName string, configMapKey string) *corev1.EnvVarSource {
	return FromOptionalConfigMap(configMapName, configMapKey, nil)
}

func FromOptionalConfigMap(configMapName string, configMapKey string, optional *bool) *corev1.EnvVarSource {
	return &corev1.EnvVarSource{
		ConfigMapKeyRef: &corev1.ConfigMapKeySelector{
			Key: configMapKey,
			LocalObjectReference: corev1.LocalObjectReference{
				Name: configMapName,
			},
			Optional: optional,
		},
	}
}

func ApplyEnvConfigMap(container *corev1.Container, name string, configMapKey string, configMapName string, optional *bool) {
	ApplyEnv(container, name, func(envvar *corev1.EnvVar) {
		envvar.Value = ""
		envvar.ValueFrom = FromOptionalConfigMap(configMapName, configMapKey, optional)
	})
}

// Append a string to the value of an env-var. If the env-var doesn't exist, it will be created with the provided value.
// A whitespace is added between the existing value and the new value.
func AppendEnvVarValue(container *corev1.Container, name string, value string) {
	if container.Env == nil {
		container.Env = make([]corev1.EnvVar, 0)
	}

	for i, env := range container.Env {
		if env.Name == name {
			opts := env.Value

			if len(opts) > 0 {
				opts += " "
			}

			opts += value

			env.Value = opts
			container.Env[i] = env

			return
		}
	}

	container.Env = append(container.Env, corev1.EnvVar{
		Name:  name,
		Value: value,
	})
}

func ApplyTlsSecret(secret *corev1.Secret, key []byte, certificate []byte) {
	secret.Type = corev1.SecretTypeTLS
	secret.Data = map[string][]byte{
		"tls.key": key,
		"tls.crt": certificate,
	}
}

func RemoveEnv(container *corev1.Container, name string) {
	if container.Env == nil {
		return
	}

	for i, e := range container.Env {
		if e.Name == name {
			container.Env = append(container.Env[:i], container.Env[i+1:]...)
			break
		}
	}
}

func ApplyNodeAffinity(template *corev1.PodTemplateSpec, matchKey string) {
	template.Spec.Affinity = &corev1.Affinity{
		NodeAffinity: &corev1.NodeAffinity{
			PreferredDuringSchedulingIgnoredDuringExecution: []corev1.PreferredSchedulingTerm{
				{
					Weight: 1,
					Preference: corev1.NodeSelectorTerm{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      matchKey,
								Operator: "In",
								Values: []string{
									"true",
								},
							},
						},
					},
				},
			},
		},
	}
}

func OverrideSecurityContextFsGroup(componentName string, securityContext *corev1.PodSecurityContext, target *appsv1.Deployment) {
	override := util.GetFsGroupOverride(componentName)
	if securityContext == nil {
		target.Spec.Template.Spec.SecurityContext = &corev1.PodSecurityContext{}
	} else {
		target.Spec.Template.Spec.SecurityContext = securityContext
	}
	if target.Spec.Template.Spec.SecurityContext.FSGroup == nil {
		target.Spec.Template.Spec.SecurityContext.FSGroup = override
	}
}

func OverrideProbe(override *corev1.Probe, target *corev1.Probe) {
	if override.InitialDelaySeconds > 0 {
		target.InitialDelaySeconds = override.InitialDelaySeconds
	}
	if override.PeriodSeconds > 0 {
		target.PeriodSeconds = override.PeriodSeconds
	}
	if override.TimeoutSeconds > 0 {
		target.TimeoutSeconds = override.TimeoutSeconds
	}
	if override.FailureThreshold > 0 {
		target.FailureThreshold = override.FailureThreshold
	}
	if override.SuccessThreshold > 0 {
		target.SuccessThreshold = override.SuccessThreshold
	}
}

// Ensure that an owner is set
func AddOwnerReference(owner v1.Object, object v1.Object, scheme *runtime.Scheme) error {

	ro, ok := owner.(runtime.Object)
	if !ok {
		return fmt.Errorf("is not a %T a runtime.Object, cannot call ensureOwnerIsSet", owner)
	}

	gvk, err := apiutil.GVKForObject(ro, scheme)
	if err != nil {
		return err
	}

	// create our ref
	newref := *util.NewOwnerRef(owner, gvk)

	// get existing refs
	refs := object.GetOwnerReferences()

	found := false
	for _, ref := range refs {
		if util.IsSameRef(ref, newref) {
			found = true
		}
	}

	// did we find it?
	if !found {
		// no! so append
		refs = append(refs, newref)
	}

	// set the new result
	object.SetOwnerReferences(refs)

	return nil
}

func ComputeConfigMapHash(cm corev1.ConfigMap) (hash string, err error) {
	d := sha256.New()
	_, err = d.Write([]byte(cm.Namespace))
	if err != nil {
		return
	}
	_, err = d.Write([]byte(cm.Name))
	if err != nil {
		return
	}
	_, err = d.Write([]byte(cm.ResourceVersion))
	if err != nil {
		return
	}
	keys := make([]string, 0)
	for key, _ := range cm.Data {
		keys = append(keys, key)
	}
	sort.Strings(keys)

	for _, key := range keys {
		_, err = d.Write([]byte(key))
		if err != nil {
			return
		}
		_, err = d.Write([]byte(cm.Data[key]))
		if err != nil {
			return
		}
	}

	hash = base64.StdEncoding.EncodeToString(d.Sum(nil)[:])
	return
}

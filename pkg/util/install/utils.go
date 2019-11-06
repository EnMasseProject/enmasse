/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package install

import (
	"fmt"
	"strconv"

	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/apiutil"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/images"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

func CreateDefaultLabels(labels map[string]string, component string, name string) map[string]string {

	if labels == nil {
		labels = make(map[string]string)
	}

	labels["component"] = component
	labels["app"] = "enmasse"
	labels["name"] = name

	return labels
}

// Apply standard set of labels
func ApplyDefaultLabels(meta *v1.ObjectMeta, component string, name string) {
	meta.Labels = CreateDefaultLabels(meta.Labels, component, name)
}

// Apply some default service values
func ApplyServiceDefaults(service *corev1.Service, component string, name string) {

	ApplyDefaultLabels(&service.ObjectMeta, component, name)
	if service.CreationTimestamp.IsZero() {
		service.Spec.Selector = CreateDefaultLabels(nil, component, name)
	}

}

func CreateDefaultAnnotations(annotations map[string]string) map[string]string {

	// currently we only ensure the annotations map is not null

	if annotations == nil {
		annotations = make(map[string]string)
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

func ApplyContainer(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container)) {
	// call "with error", and eat up the error
	_ = ApplyContainerWithError(deployment, name, func(container *corev1.Container) error {
		mutator(container)
		return nil
	})
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

func ApplyContainerWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container) error) error {

	if deployment.Spec.Template.Spec.Containers == nil {
		deployment.Spec.Template.Spec.Containers = make([]corev1.Container, 0)
	}

	for i, c := range deployment.Spec.Template.Spec.Containers {
		if c.Name == name {
			return mutator(&deployment.Spec.Template.Spec.Containers[i])
		}
	}

	c := &corev1.Container{
		Name: name,
	}

	err := mutator(c)
	if err == nil {
		deployment.Spec.Template.Spec.Containers = append(deployment.Spec.Template.Spec.Containers, *c)
	}

	return err
}

func ApplyInitContainer(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container)) {
	// call "with error", and eat up the error
	_ = ApplyInitContainerWithError(deployment, name, func(container *corev1.Container) error {
		mutator(container)
		return nil
	})
}

func ApplyInitContainerWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Container) error) error {

	if deployment.Spec.Template.Spec.InitContainers == nil {
		deployment.Spec.Template.Spec.InitContainers = make([]corev1.Container, 0)
	}

	for i, c := range deployment.Spec.Template.Spec.InitContainers {
		if c.Name == name {
			return mutator(&deployment.Spec.Template.Spec.InitContainers[i])
		}
	}

	c := &corev1.Container{
		Name: name,
	}

	err := mutator(c)
	if err == nil {
		deployment.Spec.Template.Spec.InitContainers = append(deployment.Spec.Template.Spec.InitContainers, *c)
	}

	return err
}

func ApplyPersistentVolume(deployment *appsv1.Deployment, name string, claimName string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.PersistentVolumeClaim == nil {
			volume.PersistentVolumeClaim = &corev1.PersistentVolumeClaimVolumeSource{}
		}
		volume.PersistentVolumeClaim.ClaimName = claimName
	})
}

func ApplyConfigMapVolume(deployment *appsv1.Deployment, name string, configMapName string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.ConfigMap == nil {
			volume.ConfigMap = &corev1.ConfigMapVolumeSource{}
		}
		volume.ConfigMap.Name = configMapName
	})
}

func ApplySecretVolume(deployment *appsv1.Deployment, name string, secretName string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.Secret == nil {
			volume.Secret = &corev1.SecretVolumeSource{}
		}
		volume.Secret.SecretName = secretName
	})
}

func ApplyEmptyDirVolume(deployment *appsv1.Deployment, name string) {
	ApplyVolume(deployment, name, func(volume *corev1.Volume) {
		if volume.EmptyDir == nil {
			volume.EmptyDir = &corev1.EmptyDirVolumeSource{}
		}
	})
}

func ApplyVolume(deployment *appsv1.Deployment, name string, mutator func(*corev1.Volume)) {
	// call "with error", and eat up the error
	_ = ApplyVolumeWithError(deployment, name, func(volume *corev1.Volume) error {
		mutator(volume)
		return nil
	})
}

func ApplyVolumeWithError(deployment *appsv1.Deployment, name string, mutator func(*corev1.Volume) error) error {

	if deployment.Spec.Template.Spec.Volumes == nil {
		deployment.Spec.Template.Spec.Volumes = make([]corev1.Volume, 0)
	}

	for i, c := range deployment.Spec.Template.Spec.Volumes {
		if c.Name == name {
			return mutator(&deployment.Spec.Template.Spec.Volumes[i])
		}
	}

	v := &corev1.Volume{
		Name: name,
	}

	err := mutator(v)
	if err == nil {
		deployment.Spec.Template.Spec.Volumes = append(deployment.Spec.Template.Spec.Volumes, *v)
	}

	return err
}

func DropVolume(deployment *appsv1.Deployment, name string) {
	if deployment.Spec.Template.Spec.Volumes == nil {
		return
	}

	// removing an entry from an array in go is tricky ...

	for i := len(deployment.Spec.Template.Spec.Volumes) - 1; i >= 0; i-- {
		v := deployment.Spec.Template.Spec.Volumes[i]
		if v.Name == name {
			deployment.Spec.Template.Spec.Volumes = append(
				deployment.Spec.Template.Spec.Volumes[:i],
				deployment.Spec.Template.Spec.Volumes[i+1:]...,
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

func ApplyEnvSecret(container *corev1.Container, name string, secretKey string, secretName string) {
	ApplyEnvOptionalSecret(container, name, secretKey, secretName, nil)
}

func ApplyEnvOptionalSecret(container *corev1.Container, name string, secretKey string, secretName string, optional *bool) {
	ApplyEnv(container, name, func(envvar *corev1.EnvVar) {
		envvar.Value = ""
		envvar.ValueFrom = &corev1.EnvVarSource{
			SecretKeyRef: &corev1.SecretKeySelector{
				Key: secretKey,
				LocalObjectReference: corev1.LocalObjectReference{
					Name: secretName,
				},
				Optional: optional,
			},
		}
	})
}

func ApplyEnvConfigMap(container *corev1.Container, name string, configMapKey string, configMapName string, optional *bool) {
	ApplyEnv(container, name, func(envvar *corev1.EnvVar) {
		envvar.Value = ""
		envvar.ValueFrom = &corev1.EnvVarSource{
			ConfigMapKeyRef: &corev1.ConfigMapKeySelector{
				Key: configMapKey,
				LocalObjectReference: corev1.LocalObjectReference{
					Name: configMapName,
				},
				Optional: optional,
			},
		}
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

// This is a workaround for a problem that may manifest when EnMasse is deployed into OLM cluster-wide
// under certain configurations. If https://github.com/operator-framework/operator-lifecycle-manager/issues/927
// is resolved, this workaround can be removed.
func ApplyFsGroupOverride(deployment *appsv1.Deployment) error {
	err := util.ApplyEnv("FS_GROUP_OVERRIDE", func(name string, value string, ok bool) error {
		if ok {
			fsGroupOverride, err := strconv.ParseInt(value, 10, 0)
			if deployment.Spec.Template.Spec.SecurityContext == nil {
				deployment.Spec.Template.Spec.SecurityContext = &corev1.PodSecurityContext{}
			}
			deployment.Spec.Template.Spec.SecurityContext.FSGroup = &fsGroupOverride
			return err
		}
		return nil
	})
	return err
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

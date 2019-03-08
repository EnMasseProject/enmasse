package iotconfig

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/images"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

func (r *ReconcileIoTConfig) addQpidProxySetup(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	err := install.ApplyContainerWithError(deployment, "qdr-cfg", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, images.ImageRequest{"enmasseproject", "qdr-proxy-configurator"}, config); err != nil {
			return err
		}

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(64*1024*1024 /* 64Mi */, resource.BinarySI),
			},
		}

		if len(container.VolumeMounts) != 1 {
			container.VolumeMounts = make([]corev1.VolumeMount, 1)
		}

		container.VolumeMounts[0].Name = "qdr-tmp-certs"
		container.VolumeMounts[0].MountPath = "/var/qdr-certs"
		container.VolumeMounts[0].ReadOnly = true

		return nil
	})

	if err != nil {
		return err
	}

	err = install.ApplyContainerWithError(deployment, "qdr-proxy", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, images.ImageRequest{"enmasseproject", "qdrouterd-base"}, config); err != nil {
			return err
		}

		container.Args = []string{"/sbin/qdrouterd", "-c", "/etc/qdr/config/qdrouterd.conf"}

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		if len(container.VolumeMounts) != 2 {
			container.VolumeMounts = make([]corev1.VolumeMount, 2)
		}

		container.VolumeMounts[0].Name = "qdr-tmp-certs"
		container.VolumeMounts[0].MountPath = "/var/qdr-certs"
		container.VolumeMounts[0].ReadOnly = true

		container.VolumeMounts[1].Name = "qdr-proxy-config"
		container.VolumeMounts[1].MountPath = "/etc/qdr/config"
		container.VolumeMounts[1].ReadOnly = true

		return nil
	})

	if err != nil {
		return err
	}

	install.ApplyConfigMapVolume(deployment, "qdr-proxy-config", "qdr-proxy-configurator")
	install.ApplyEmptyDirVolume(deployment, "qdr-tmp-certs")

	return nil
}

func AppendHonoAdapterEnvs(container *corev1.Container, username string, password string) {
	container.Env = append(container.Env, []corev1.EnvVar{
		{Name: "HONO_MESSAGING_HOST", Value: "localhost"},
		{Name: "HONO_MESSAGING_PORT", Value: "5672"},
		{Name: "HONO_COMMAND_HOST", Value: "localhost"},
		{Name: "HONO_COMMAND_PORT", Value: "5672"},

		{Name: "HONO_REGISTRATION_HOST", Value: "iot-device-registry.$(KUBERNETES_NAMESPACE).svc"},
		{Name: "HONO_REGISTRATION_USERNAME", Value: username},
		{Name: "HONO_REGISTRATION_PASSWORD", Value: password},
		{Name: "HONO_CREDENTIALS_HOST", Value: "iot-device-registry.$(KUBERNETES_NAMESPACE).svc"},
		{Name: "HONO_CREDENTIALS_USERNAME", Value: username},
		{Name: "HONO_CREDENTIALS_PASSWORD", Value: password},
		{Name: "HONO_TENANT_HOST", Value: "iot-tenant-service.$(KUBERNETES_NAMESPACE).svc"},
		{Name: "HONO_TENANT_USERNAME", Value: username},
		{Name: "HONO_TENANT_PASSWORD", Value: password},
	}...)
}

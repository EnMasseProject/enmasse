/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.KubeUtil;
import io.enmasse.admin.model.v1.ConsoleService;
import io.enmasse.admin.model.v1.ConsoleServiceSpec;
import io.enmasse.admin.model.v1.ConsoleServiceStatus;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.enmasse.controller.common.TemplateParameter.*;

public class ServiceHelper {
   public static Map<String, Integer> getServicePorts(Service service) {
       Map<String, Integer> servicePorts = new HashMap<>();
       Map<String, String> serviceAnnotations = service.getMetadata().getAnnotations();
       for (Map.Entry<String, String> annotationEntry : serviceAnnotations.entrySet()) {
           String annotationKey = annotationEntry.getKey();
           String annotationValue = annotationEntry.getValue();
           if (annotationKey.startsWith(AnnotationKeys.SERVICE_PORT_PREFIX)) {
               String portName = annotationKey.substring(AnnotationKeys.SERVICE_PORT_PREFIX.length());
               int portValue = Integer.parseInt(annotationValue);
               servicePorts.put(portName, portValue);
           }
       }
       return servicePorts;
   }

   public static Map<String, Integer> fromServicePortList(List<ServicePort> servicePorts) {
       Map<String, Integer> servicePortMap = new HashMap<>();
       for (ServicePort servicePort : servicePorts) {
           servicePortMap.put(servicePort.getName(), servicePort.getPort());
       }
       return servicePortMap;
   }

    public static List<ServicePort> toServicePortList(Map<String, Integer> servicePorts) {
       List<ServicePort> servicePortList = new ArrayList<>();
       for (Map.Entry<String, Integer> entry : servicePorts.entrySet()) {
           servicePortList.add(new ServicePortBuilder()
                   .withName(entry.getKey())
                   .withPort(entry.getValue())
                   .build());
       }
       return servicePortList;
    }

    public static boolean applyConsoleServiceToContainer(Container container, ConsoleService consoleService) {
        ConsoleServiceSpec spec = consoleService.getSpec();
        ConsoleServiceStatus status = consoleService.getStatus();

        int updates = 0;

        if (spec.getDiscoveryMetadataURL() != null) {
            EnvVar env = new EnvVarBuilder().withName(CONSOLE_OAUTH_DISCOVERY_URL).withValue(spec.getDiscoveryMetadataURL()).build();
            if (KubeUtil.applyEnvVar(container, env)) {
                updates++;
            }
        } else {
            if (KubeUtil.removeEnvVar(container, CONSOLE_OAUTH_DISCOVERY_URL)) {
                updates++;
            }
        }

        if (spec.getScope() != null) {
            EnvVar env = new EnvVarBuilder().withName(CONSOLE_OAUTH_SCOPE).withValue(spec.getScope()).build();
            if (KubeUtil.applyEnvVar(container, env)) {
                updates++;
            }
        } else {
            if (KubeUtil.removeEnvVar(container, CONSOLE_OAUTH_SCOPE)) {
                updates++;
            }
        }

        if (status != null && status.getUrl() != null) {
            EnvVar env1 = new EnvVarBuilder().withName(CONSOLE_LINK).withValue(status.getUrl()).build();
            if (KubeUtil.applyEnvVar(container, env1)) {
                updates++;
            }
        } else {
            if (KubeUtil.removeEnvVar(container, CONSOLE_LINK)) {
                updates++;
            }
        }

        if (spec.getOauthClientSecret() != null) {
            String oauthClientSecretName = spec.getOauthClientSecret().getName();
            EnvVar clientIdEnv = new EnvVarBuilder().withName("CONSOLE_OAUTH_CLIENT_ID")
                    .withValueFrom(new EnvVarSourceBuilder()
                            .withNewSecretKeyRef("client-id", oauthClientSecretName, true)
                            .build())
                    .build();
            if (KubeUtil.applyEnvVar(container, clientIdEnv)) {
                updates++;
            }
            EnvVar clientSecretEnv = new EnvVarBuilder().withName("CONSOLE_OAUTH_CLIENT_SECRET")
                    .withValueFrom(new EnvVarSourceBuilder()
                            .withNewSecretKeyRef("client-secret", oauthClientSecretName, true)
                            .build())
                    .build();
            if (KubeUtil.applyEnvVar(container, clientSecretEnv)) {
                updates++;
            }
        } else {
            if (KubeUtil.removeEnvVar(container, "CONSOLE_OAUTH_CLIENT_ID")) {
                updates++;
            }

            if (KubeUtil.removeEnvVar(container, "CONSOLE_OAUTH_CLIENT_SECRET")) {
                updates++;
            }
        }

        if (spec.getSsoCookieSecret() != null) {
            EnvVar clientIdEnv = new EnvVarBuilder().withName("SSO_COOKIE_SECRET")
                    .withValueFrom(new EnvVarSourceBuilder()
                            .withNewSecretKeyRef("cookie-secret", spec.getSsoCookieSecret().getName(), true)
                            .build())
                    .build();
            if (KubeUtil.applyEnvVar(container, clientIdEnv)) {
                updates++;
            }
        } else {
            if (KubeUtil.removeEnvVar(container, "SSO_COOKIE_SECRET")) {
                updates++;
            }
        }

        return updates > 0;
    }
}

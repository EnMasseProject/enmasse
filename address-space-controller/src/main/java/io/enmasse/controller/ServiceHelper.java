/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Service;

import java.util.HashMap;
import java.util.Map;

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
}

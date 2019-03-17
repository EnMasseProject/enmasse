/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"fmt"
	corev1 "k8s.io/api/core/v1"
	cert "k8s.io/client-go/util/cert"
	"net"
)

func ServiceToCommonName(serviceNamespace, serviceName string) string {
	return fmt.Sprintf("%s.%s.svc", serviceName, serviceNamespace)
}

func GenerateSelfSignedCertSecret(host string, alternateIPs []net.IP, alternateDNS []string, secret *corev1.Secret) error {
	secret.StringData = make(map[string]string)

	crt, key, err := cert.GenerateSelfSignedCertKey(host, alternateIPs, alternateDNS)
	if err != nil {
		return err
	}

	secret.StringData = make(map[string]string)
	secret.StringData["tls.key"] = string(key)
	secret.StringData["tls.crt"] = string(crt)
	return nil
}

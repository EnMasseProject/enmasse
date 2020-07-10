/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package router

import (
	"encoding/json"
	"fmt"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
)

type routerConfig struct {
	entities [][]interface{}
}

func generateConfig(i *v1.MessagingInfrastructure, router *v1.MessagingInfrastructureSpecRouter) routerConfig {
	globalAuthHost := fmt.Sprintf("access-control-%s.%s.svc.cluster.local", i.Name, i.Namespace)
	authServicePort := 5671

	return routerConfig{
		entities: [][]interface{}{
			[]interface{}{
				// Generic configuration settings
				"router",
				map[string]interface{}{
					"workerThreads":           4, // TODO: Set from variable
					"timestampsInUTC":         true,
					"defaultDistribution":     "unavailable",
					"mode":                    "interior",
					"id":                      "${HOSTNAME}",
					"allowResumableLinkRoute": false,
				},
			},
			[]interface{}{
				// Internal TLS profile used by all internal components
				"sslProfile",
				map[string]interface{}{
					"name":           "infra_tls",
					"privateKeyFile": "/etc/enmasse-certs/tls.key",
					"certFile":       "/etc/enmasse-certs/tls.crt",
					"caCertFile":     "/etc/enmasse-certs/ca.crt",
				},
			},
			[]interface{}{
				// Listener for inter-router traffic. Should not be used by other services.
				"listener",
				map[string]interface{}{
					"name":             "interrouter",
					"host":             "0.0.0.0",
					"port":             55672,
					"requireSsl":       true,
					"role":             "inter-router",
					"saslMechanisms":   "EXTERNAL",
					"sslProfile":       "infra_tls",
					"authenticatePeer": true,
				},
			},
			[]interface{}{
				// Listener for internal management commands.
				"listener",
				map[string]interface{}{
					"name":             "operator-management",
					"host":             "0.0.0.0",
					"port":             55671,
					"requireSsl":       true,
					"saslMechanisms":   "EXTERNAL",
					"sslProfile":       "infra_tls",
					"authenticatePeer": true,
				},
			},
			[]interface{}{
				// Localhost listener for admin access
				"listener",
				map[string]interface{}{
					"name":             "local-management",
					"host":             "127.0.0.1",
					"port":             7777,
					"authenticatePeer": false,
				},
			},
			[]interface{}{
				// Localhost listener for liveness probe and metrics
				"listener",
				map[string]interface{}{
					"name":             "liveness",
					"host":             "127.0.0.1",
					"port":             7778,
					"authenticatePeer": false,
					"http":             true,
					"metrics":          true,
					"healthz":          true,
					"websockets":       false,
					"httpRootDir":      "invalid",
				},
			},
			[]interface{}{
				// Listener for cluster-internal components
				"listener",
				map[string]interface{}{
					"name":             "cluster-internal",
					"host":             "0.0.0.0",
					"port":             55667,
					"requireSsl":       true,
					"saslMechanisms":   "EXTERNAL",
					"sslProfile":       "infra_tls",
					"authenticatePeer": true,
				},
			},

			// TODO this global authService config is temporary.  Will be replaced by per endpoint config.
			[]interface{}{
				// Listener for cluster-internal components
				"authServicePlugin",
				map[string]interface{}{
					"name":       fmt.Sprintf("%s:%d", globalAuthHost, authServicePort),
					"host":       globalAuthHost,
					"port":       authServicePort,
					"realm":      globalAuthHost,
					"sslProfile": "infra_tls",
				},
			},
		},
	}
}

func serializeConfig(config *routerConfig) ([]byte, error) {
	return json.Marshal(config.entities)
}

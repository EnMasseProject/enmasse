/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export interface IIoTProjectsResponse {
  allProjects: {
    total: number;
    iotProjects: Array<{
      metadata: {
        name: string;
        namespace: string;
        creationTimestamp: string;
      };
      enabled: boolean;
      spec: {
        downstreamStrategyType: string;
        configuration: string;
        downstreamStrategy: {
          addressSpace: {
            name: string;
          };
          addresses: {
            Telemetry: {
              name: string;
            };
            Event: {
              name: string;
            };
            Command: {
              name: string;
            };
          };
        };
      };
      status: {
        phase: string;
        phaseReason: string;
        tenantName: string;
        downstreamEndpoint: {
          host: string;
          port: number;
          credentials: {
            username: string;
            password: string;
          };
          tls: boolean;
          certificate: string;
        };
      };
      endpoints: Array<{
        name: string;
        url: string;
        host: string;
      }>;
    }>;
  };
}

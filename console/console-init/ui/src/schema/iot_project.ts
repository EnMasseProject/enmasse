/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
interface IAddressSpaceType {
  kind: string;
  metadata: {
    name: string;
    namespace: string;
    creationTimestamp: string;
  };
  messagingStatus?: {
    isReady: boolean;
    phase: string;
    messages: Array<string>;
  };
}

export interface IIotProjectType {
  kind: string;
  metadata: {
    name: string;
    namespace: string;
    creationTimestamp: string;
  };
  iotStatus?: {
    phase: string;
    phaseReason: string;
  };
  spec?: {
    tenantId?: string;
    addresses?: {
      Telemetry?: {
        name?: string;
        plan?: string;
        type?: string;
      };
      Event?: {
        name?: string;
        plan?: string;
        type?: string;
      };
      Command?: Array<{
        name?: string;
        plan?: string;
        type?: string;
      }>;
    };
    configuration?: string;
  };
  endpoints?: Array<{
    name?: string;
    url?: string;
    host?: string;
    port?: number;
    tls?: boolean;
  }>;
  enabled: boolean;
}
export interface IIoTProjectsResponse {
  allProjects: {
    totoal: number;
    objects: Array<IIotProjectType>;
  };
}
export interface IAllProjectsResponse {
  allProjects: {
    total: number;
    objects: Array<IAddressSpaceType | IIotProjectType>;
    // iotProjects?: Array<{
    //   metadata?: {
    //     name?: string;
    //     namespace?: string;
    //     creationTimestamp?: string;
    //   };
    //   enabled?: boolean;
    //   spec?: {
    //     tenantId?: string;
    //     configuration?: string;
    //     addresses?: {
    //       Telemetry?: {
    //         name?: string;
    //       };
    //       Event?: {
    //         name?: string;
    //       };
    //       Command?: Array<{
    //         name?: string;
    //       }>;
    //     };
    //   };
    //   status?: {
    //     phase?: string;
    //     phaseReason?: string;
    //   };
    //   endpoints?: Array<{
    //     name?: string;
    //     url?: string;
    //     host?: string;
    //     tls?: boolean;
    //   }>;
    // }>;
    // total?: number;
    // iotProjects?: Array<{
    //   metadata?: {
    //     name?: string;
    //     namespace?: string;
    //     creationTimestamp?: string;
    //   };
    //   enabled?: boolean;
    //   spec?: {
    //     downstreamStrategyType?: string;
    //     configuration?: string;
    //     downstreamStrategy?: {
    //       addressSpace?: {
    //         name?: string;
    //       };
    //       addresses?: {
    //         Telemetry?: {
    //           name?: string;
    //         };
    //         Event?: {
    //           name?: string;
    //         };
    //         Command: Array<{
    //           name: string;
    //         }>;
    //       };
    //     };
    //   };
    //   status?: {
    //     phase?: string;
    //     phaseReason?: string;
    //     tenantName?: string;
    //     downstreamEndpoint?: {
    //       host?: string;
    //       port?: number;
    //       credentials?: {
    //         username?: string;
    //         password?: string;
    //       };
    //       tls?: boolean;
    //       certificate?: string;
    //     };
    //   };
    //   endpoints?: Array<{
    //     name?: string;
    //     url?: string;
    //     host?: string;
    //     tls?: boolean;
    //   }>;
    // }>;
  };
}

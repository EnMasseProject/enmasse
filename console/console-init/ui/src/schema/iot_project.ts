/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
interface IObject_Metadata {
  name?: string;
  namespace?: string;
  creationTimestamp?: string;
}
interface IAddressSpaceType {
  kind: string;
  metadata: IObject_Metadata;
  messagingStatus?: {
    isReady: boolean;
    phase: string;
    messages: Array<string>;
  };
  spec?: {
    plan: {
      spec: {
        displayName?: string;
      };
      metadata: {
        name?: string;
      };
    };
    type?: string;
    authenticationService?: {
      name?: string;
    };
  };
}

export interface IIotProjectType {
  kind: string;
  metadata: IObject_Metadata;
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

export interface ISearchNameOrNameSpaceProjectListResponse {
  allProjects: {
    total: number;
    objects: Array<{
      kind?: string;
      metadata: IObject_Metadata;
    }>;
  };
}
export interface IAllProjectsResponse {
  allProjects: {
    total: number;
    objects: Array<IAddressSpaceType | IIotProjectType>;
  };
}

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export interface IMetrics {
  name: string;
  type: string;
  value: number;
  units: string;
}

export interface IConnectionDetailResponse {
  connections: {
    total: number;
    connections: Array<{
      metadata: {
        name: string;
        namespace: string;
        creationTimestamp: string;
      };
      spec: {
        hostname: string;
        containerId: string;
        protocol: string;
        encrypted: boolean;
        properties: Array<{
          key: string;
          value: string;
        }>;
      };
      metrics: Array<IMetrics>;
    }>;
  };
}

export interface IAddressResponse {
  addresses: {
    total: number;
    addresses: Array<{
      metadata: {
        namespace: string;
        name: string;
        creationTimestamp: string;
      };
      spec: {
        address: string;
        type: string;
        plan: {
          spec: {
            displayName: string;
          };
          metadata: {
            name: string;
          };
        };
      };
      status: {
        planStatus: {
          partitions: number;
        };
        isReady: boolean;
        messages: Array<string>;
        phase: string;
      };
      metrics: Array<IMetrics>;
    }>;
  };
}
export interface IConnectionListResponse {
  connections: {
    total: number;
    connections: Array<{
      metadata: {
        name: string;
        creationTimestamp: string;
      };
      spec: {
        hostname: string;
        containerId: string;
        protocol: string;
        encrypted: boolean;
      };
      metrics: Array<IMetrics>;
    }>;
  };
}

export interface IAddressDetailResponse {
  addresses: {
    total: number;
    addresses: Array<{
      metadata: {
        name: string;
        namespace: string;
        creationTimestamp: string;
      };
      spec: {
        address: string;
        type: string;
        topic: string | null;
        deadLetterAddress: string | null;
        expiryAddress: string | null;
        plan: {
          spec: {
            displayName: string;
            addressType: string;
          };
          metadata: {
            name: string;
          };
        };
      };
      status: {
        isReady: boolean;
        messages: string;
        phase: string;
        planStatus: {
          partitions: number;
        };
      };
      metrics: Array<IMetrics>;
    }>;
  };
}
export interface IAddressLinksResponse {
  addresses: {
    total: number;
    addresses: Array<{
      metadata: {
        name: string;
      };
      spec: {
        addressSpace: string;
      };
      links: {
        total: number;
        links: Array<{
          metadata: {
            name: string;
          };
          spec: {
            role: string;
            connection: {
              metadata: {
                name: string;
                namespace: string;
              };
              spec: {
                containerId: string;
              };
            };
          };
          metrics: IMetrics[];
        }>;
      };
    }>;
  };
}

export interface IAddressSpacesResponse {
  addressSpaces: {
    total: number;
    addressSpaces: Array<{
      metadata: {
        name: string;
        namespace: string;
        creationTimestamp: string;
      };
      spec: {
        type: string;
        plan: {
          metadata: {
            name: string;
          };
          spec: {
            displayName: string;
          };
        };
        authenticationService: {
          name: string;
        };
      };
      status: {
        isReady: boolean;
        phase: string;
        messages: Array<string>;
      };
    }>;
  };
}

export interface IConnectionLinksResponse {
  connections: {
    total: number;
    connections: Array<{
      metadata: {
        name: string;
        namespace: string;
      };
      links: {
        total: number;
        links: Array<{
          metadata: {
            name: string;
          };
          spec: {
            role: string;
            address: string;
          };
          metrics: Array<IMetrics>;
        }>;
      };
    }>;
  };
}

export interface IConnectionLinksNameSearchResponse {
  connections: {
    total: number;
    connections: Array<{
      links: {
        total: number;
        links: Array<{
          metadata: {
            name: string;
          };
        }>;
      };
    }>;
  };
}
export interface IConnectionLinksAddressSearchResponse {
  connections: {
    total: number;
    connections: Array<{
      links: {
        total: number;
        links: Array<{
          spec: {
            address: string;
          };
        }>;
      };
    }>;
  };
}

export interface ISearchAddressLinkNameResponse {
  addresses: {
    total: number;
    addresses: Array<{
      links: {
        total: number;
        links: Array<{
          metadata: {
            name: string;
          };
        }>;
      };
    }>;
  };
}

export interface ISearchAddressLinkContainerResponse {
  addresses: {
    total: number;
    addresses: Array<{
      links: {
        total: number;
        links: Array<{
          spec: {
            connection: {
              spec: {
                containerId: string;
              };
            };
          };
        }>;
      };
    }>;
  };
}

export interface ISearchNameOrNameSpaceAddressSpaceListResponse {
  addressSpaces: {
    total: number;
    addressSpaces: Array<{
      metadata: {
        name: string;
        namespace: string;
      };
    }>;
  };
}

export interface IAddressListNameSearchResponse {
  addresses: {
    total: number;
    addresses: Array<{
      spec: {
        address: string;
      };
    }>;
  };
}

export interface IConnectionListNameSearchResponse {
  connections: {
    total: number;
    connections: Array<{
      spec: {
        hostname: string;
        containerId: string;
      };
    }>;
  };
}

export interface IUserDetail {
  whoami: {
    metadata: {
      name: string;
    };
    fullName: string;
  };
}

export interface IAddressSpaceDetailResponse {
  addressSpaces: {
    addressSpaces: Array<{
      metadata: {
        namespace: string;
        name: string;
        creationTimestamp: string;
      };
      spec: {
        type: string;
        plan: {
          metadata: {
            name: string;
          };
          spec: {
            displayName: string;
          };
        };
        authenticationService: {
          name: string;
        };
      };
      status: {
        isReady: boolean;
        phase: string;
        messages: string[];
      };
    }>;
  };
}

export enum MessagingEndpointProtocol {
  amqp,
  amqps,
  amqp_ws,
  amqp_wss
}

export enum MessagingEndpointType {
  cluster,
  nodePort,
  loadBalancer,
  route,
  ingress
}
export interface IAddressSpaceSchema {
  addressSpaceSchema: Array<{
    metadata: {
      name?: string;
    };
    spec: {
      description?: string;
      authenticationServices?: string[];
      certificateProviderTypes: Array<{
        name?: string;
        displayName?: string;
        description?: string;
      }>;
      routeServicePorts: Array<{
        name?: string;
        displayName?: string;
        routeTlsTerminations?: string[];
      }>;
      endpointExposeTypes: Array<{
        name?: string;
        displayName?: string;
        description?: string;
      }>;
    };
  }>;
}

export interface IEndpointProtocol {
  name?: string;
  protocol?: string;
  port?: number;
}
export interface IEndpointResponse {
  metadata: {
    name: string;
    namespace: string;
    creationTimestamp: string;
  };
  spec: {
    protocols: string[];
  };
  status: {
    phase: string;
    type: string;
    message: string;
    host: string;
    ports: IEndpointProtocol[];
  };
}
export interface IEndpointListResponse {
  total: number;
  messagingEndpoints: {
    messagingEndpoints: IEndpointResponse[];
  };
}

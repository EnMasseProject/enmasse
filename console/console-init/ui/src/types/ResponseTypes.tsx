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
    Total: number;
    Connections: Array<{
      objectMeta: {
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
    Total: number;
    Addresses: Array<{
      objectMeta: {
        namespace: string;
        name: string;
      };
      spec: {
        address: string;
        type: string;
        plan: {
          spec: {
            displayName: string;
          };
          objectMeta: {
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
    Total: number;
    Connections: Array<{
      objectMeta: {
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

enum eLinkRole {
  "sender",
  "receiver"
}
export interface IAddressDetailResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      objectMeta: {
        name: string;
        namespace: String;
        creationTimestamp: string;
      };
      spec: {
        address: string;
        topic: string | null;
        plan: {
          spec: {
            displayName: string;
            addressType: string;
          };
        };
      };
      status: {
        isReady: string;
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
    Total: number;
    Addresses: Array<{
      objectMeta: {
        name: string;
      };
      spec: {
        addressSpace: string;
      };
      links: {
        Total: number;
        Links: Array<{
          objectMeta: {
            name: string;
          };
          spec: {
            role: string;
            connection: {
              objectMeta: {
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
    Total: number;
    AddressSpaces: Array<{
      objectMeta: {
        name: string;
        namespace: string;
        creationTimestamp: string;
      };
      spec: {
        type: string;
        plan: {
          objectMeta: {
            name: string;
          };
          spec: {
            displayName: string;
          };
        };
        AuthenticationService: {
          Name: string;
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
    Total: number;
    Connections: Array<{
      objectMeta: {
        name: string;
        namespace: string;
      };
      links: {
        Total: number;
        Links: Array<{
          objectMeta: {
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
    Total: number;
    Connections: Array<{
      links: {
        Total: number;
        Links: Array<{
          objectMeta: {
            name: string;
          };
        }>;
      };
    }>;
  };
}
export interface IConnectionLinksAddressSearchResponse {
  connections: {
    Total: number;
    Connections: Array<{
      links: {
        Total: number;
        Links: Array<{
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
    Total: number;
    Addresses: Array<{
      links: {
        Total: number;
        Links: Array<{
          objectMeta: {
            name: string;
          };
        }>;
      };
    }>;
  };
}

export interface ISearchAddressLinkContainerResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      links: {
        Total: number;
        Links: Array<{
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
    Total: number;
    AddressSpaces: Array<{
      objectMeta: {
        name: string;
        namespace: string;
      };
    }>;
  };
}

export interface IAddressListNameSearchResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      spec: {
        address: string;
      };
    }>;
  };
}

export interface IConnectionListNameSearchResponse {
  connections: {
    Total: number;
    Connections: Array<{
      spec: {
        hostname: string;
        containerId: string;
      };
    }>;
  };
}

export interface IUserDetail {
  whoami: {
    objectMeta: {
      name: string;
    };
    fullName: string;
  };
}

export interface IMetrics {
  Name: string;
  Type: string;
  Value: number;
  Units: string;
}

export interface IConnectionDetailResponse {
  connections: {
    Total: number;
    Connections: Array<{
      ObjectMeta: {
        Name: string;
        Namespace: string;
        CreationTimeStamp: string;
        ResourceVersion: string;
      };
      Spec: {
        Hostname: string;
        ContainerId: string;
        Protocol: string;
        Encrypted: boolean;
        Properties: Array<{
          Key: string;
          Value: string;
        }>;
      };
      Metrics: Array<IMetrics>;
      Links: {
        Total: number;
        Links: Array<{
          ObjectMeta: {
            Name: string;
            Namespace: string;
          };
          Spec: {
            Role: string;
          };
          Metrics: Array<IMetrics>;
        }>;
      };
    }>;
  };
}

export interface IAddressResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      ObjectMeta: {
        Namespace: string;
        Name: string;
      };
      Spec: {
        Address: string;
        Type: string;
        Plan: {
          Spec: {
            DisplayName: string;
          };
        };
      };
      Status: {
        PlanStatus: {
          Partitions: number;
        };
        IsReady: boolean;
        Messages: Array<string>;
        Phase: string;
      };
      Metrics: Array<IMetrics>;
    }>;
  };
}
export interface IConnectionListResponse {
  connections: {
    Total: number;
    Connections: Array<{
      ObjectMeta: {
        Name: string;
      };
      Spec: {
        Hostname: string;
        ContainerId: string;
        Protocol: string;
        Encrypted: boolean;
      };
      Metrics: Array<IMetrics>;
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
      ObjectMeta: {
        Name: string;
        Namespace: String;
        CreationTimestamp: string;
      };
      Spec: {
        Address: string;
        Plan: {
          Spec: {
            DisplayName: string;
            AddressType: string;
          };
        };
      };
      Status: {
        IsReady: string;
        Messages: string;
        Phase: string;
        PlanStatus: {
          Partitions: number;
        };
      };
      Metrics: Array<IMetrics>;
    }>;
  };
}
export interface IAddressLinksResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      ObjectMeta: {
        Name: string;
      };
      Spec: {
        AddressSpace: string;
      };
      Links: {
        Total: number;
        Links: Array<{
          ObjectMeta: {
            Name: string;
            Namespace: string;
          };
          Spec: {
            Role: string;
            Connection: {
              ObjectMeta: {
                Name: string;
                Namespace: string;
              };
              Spec: {
                ContainerId: string;
              };
            };
          };
          Metrics: IMetrics[];
        }>;
      };
    }>;
  };
}

export interface IAddressSpacesResponse {
  addressSpaces: {
    Total: number;
    AddressSpaces: Array<{
      ObjectMeta: {
        Name: string;
        Namespace: string;
        CreationTimestamp: string;
      };
      Spec: {
        Type: string;
        Plan: {
          Spec: {
            DisplayName: string;
          };
        };
      };
      Status: {
        IsReady: boolean;
      };
    }>;
  };
}

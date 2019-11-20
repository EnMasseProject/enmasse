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
      };
      Metrics: Array<IMetrics>;
    }>;
  };
}

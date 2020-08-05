/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export interface IDeviceDetailResponse {
  devices: {
    total: number;
    devices: Array<{
      deviceId?: string;
      enabled?: boolean;
      jsonData?: string;
      via?: string[];
      ext?: string;
      defaults?: string;
      credentials?: string;
      viaGroups?: string[];
      memberOf?: string[];
      status?: {
        created?: string;
        updated?: string;
        lastSeen?: string;
      };
    }>;
  };
}

export interface ICredentialsReponse {
  credentials: {
    total?: number;
    credentials?: string;
  };
}

export interface IIoTDevicesResponse {
  devices: {
    total: number;
    devices: Array<{
      deviceId: string;
      status: {
        lastSeen: string;
        updated: string;
        created: string;
      };
      enabled: boolean;
      via: string[];
      viaGroups: string[];
      credentials: string;
    }>;
  };
}

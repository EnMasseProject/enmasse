/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

interface IRegistrationResponse {
  enabled?: boolean;
  via?: string[];
  ext?: string;
  viaGroups?: string[];
  memberOf?: string[];
  defaults?: string;
}
export interface IDeviceResponse {
  deviceId?: string;
  registration: IRegistrationResponse;
  jsonData?: string;
  credentials?: string;
  status?: {
    created?: string;
    updated?: string;
    lastSeen?: string;
  };
}
export interface IDeviceDetailResponse {
  devices: {
    total: number;
    devices: Array<IDeviceResponse>;
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
      registration: {
        enabled: boolean;
        via: string[];
        viaGroups: string[];
      };
      status: {
        lastSeen: Date;
        updated: Date;
        created: Date;
      };
      credentials: string;
    }>;
  };
}

export interface ICreateDeviceResponse {
  deviceId?: string;
  registration: {
    enabled: boolean;
    via?: string[];
    viaGroups?: string[];
    memberOf?: string[];
    defaults?: string;
    ext?: string;
  };
  credentials?: string; //A Json array with the devices credentials.
}

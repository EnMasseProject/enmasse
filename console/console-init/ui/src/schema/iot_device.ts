/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export interface IDeviceDetailResponse {
  devices: {
    total?: number;
    devices: Array<{
      deviceId?: string;
      enabled?: boolean;
      viaGateway?: boolean;
      jsonData?: string;
    }>;
  };
}

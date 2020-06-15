/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery } from "@apollo/react-hooks";
import { POLL_INTERVAL, FetchPolicy } from "constant";
import { IIoTDevicesResponse } from "schema/iot_device";
import {
  RETURN_ALL_DEVICES_FOR_IOT_PROJECT,
  DELETE_IOT_DEVICE,
  UPDATE_IOT_DEVICE
} from "graphql-module/queries/iot_device";
import {
  DeviceList,
  IDevice,
  IDeviceFilter
} from "modules/iot-device/components";
import { IRowData, SortByDirection, ISortBy } from "@patternfly/react-table";
import { getTableCells } from "modules/iot-device";
import { compareObject } from "utils";
import { useParams } from "react-router";
import { useMutationQuery } from "hooks";
import { Loading } from "use-patternfly";

export interface IDeviceListContainerProps {
  setTotalDevices: (val: number) => void;
  onSelectDevice: (
    data: IDevice,
    isSelected: boolean,
    isAllSelected?: boolean
  ) => void;
  selectedDevices: IDevice[];
  areAllDevicesSelected: boolean;
  selectAllDevices: (devices: IDevice[]) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  appliedFilter: IDeviceFilter;
}

export const DeviceListContainer: React.FC<IDeviceListContainerProps> = ({
  setTotalDevices,
  onSelectDevice,
  selectedDevices,
  areAllDevicesSelected,
  selectAllDevices,
  sortValue,
  setSortValue,
  appliedFilter
}) => {
  const { projectname } = useParams();

  const [sortBy, setSortBy] = useState<ISortBy>();

  const { loading, data } = useQuery<IIoTDevicesResponse>(
    RETURN_ALL_DEVICES_FOR_IOT_PROJECT(projectname, sortBy, appliedFilter),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const [setDeleteDeviceQueryVariables] = useMutationQuery(DELETE_IOT_DEVICE, [
    "devices_for_iot_project"
  ]);

  const [setUpdateDeviceQueryVariables] = useMutationQuery(UPDATE_IOT_DEVICE, [
    "devices_for_iot_project"
  ]);

  const { total, devices } = data?.devices || {};
  setTotalDevices(total || 0);

  const deleteDevice = (row: any) => {
    const { deviceId } = row.originalData;
    setDeleteDeviceQueryVariables({
      deviceId,
      iotproject: projectname
    });
  };

  const disableDevice = (row: any) => {
    const { deviceId, jsonData, viaGateway } = row.originalData;
    setUpdateDeviceQueryVariables({
      project: projectname,
      device: {
        deviceId,
        jsonData,
        viaGateway,
        enabled: false
      }
    });
  };

  const enableDevice = (row: any) => {
    // TODO: to be changed according to the backend query of mock
    const { deviceId, jsonData } = row.originalData;
    setUpdateDeviceQueryVariables({
      project: projectname,
      device: {
        deviceId,
        jsonData,
        viaGateway: true,
        enabled: true
      }
    });
  };

  const actionResolver = (rowData: IRowData) => {
    const enabled = rowData?.originalData?.enabled;
    return [
      {
        title: "Delete",
        onClick: () => deleteDevice(rowData)
      },
      {
        title: enabled ? "Disable" : "Enable",
        onClick: () =>
          enabled ? disableDevice(rowData) : enableDevice(rowData)
      }
    ];
  };

  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }

  if (loading) {
    return <Loading />;
  }

  const onSort = (_event: any, index: number, direction: SortByDirection) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  const rows =
    devices?.map(({ deviceId, enabled, jsonData, viaGateway }) => {
      return {
        deviceId,
        enabled,
        jsonData,
        viaGateway,
        ...JSON.parse(jsonData)?.timestamps,
        selected:
          selectedDevices.filter(device =>
            compareObject({ deviceId }, { deviceId: device.deviceId })
          ).length === 1
      };
    }) || [];

  if (areAllDevicesSelected && selectedDevices.length !== devices?.length) {
    selectAllDevices(rows || []);
  }

  return (
    <DeviceList
      deviceRows={rows.map(getTableCells)}
      onSelectDevice={onSelectDevice}
      actionResolver={actionResolver}
      onSort={onSort}
      sortBy={sortBy}
    />
  );
};

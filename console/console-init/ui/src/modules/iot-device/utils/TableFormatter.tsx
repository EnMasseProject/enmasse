import React from "react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon
} from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { FormatDistance } from "use-patternfly";

import { IRowData, ICell } from "@patternfly/react-table";
import { IDevice } from "modules/iot-device/components";
import { Label } from "@patternfly/react-core";
import { DeviceConnectionType } from "constant";

const renderDeviceType = (type: string) => {
  return type === "N/A" ? (
    <Label color="orange" icon={<ExclamationTriangleIcon />}>
      {type}
    </Label>
  ) : (
    <Label variant="outline" color="green">
      {type}
    </Label>
  );
};

export const getTableCells = (row: IDevice, selectedColumns: string[]) => {
  const {
    enabled,
    via,
    viaGroups,
    memberOf,
    credentials,
    deviceId,
    updated,
    created,
    lastSeen
  } = row;

  let deviceType: DeviceConnectionType;

  if (via?.length || viaGroups?.length) {
    deviceType = DeviceConnectionType.VIA_GATEWAYS;
  } else {
    deviceType = DeviceConnectionType.CONNECTED_DIRECTLY;
  }

  const cells: ICell[] = [];
  selectedColumns.forEach(column => {
    switch (column) {
      case "deviceId":
        cells.push({
          header: "id",
          title: (
            <span>
              <Link to={`devices/${deviceId}/device-info`}>{deviceId}</Link>
            </span>
          )
        });
        break;
      case "connectionType":
        cells.push({
          header: "type",
          title: renderDeviceType(deviceType)
        });
        break;
      case "status":
        cells.push({
          header: "status",
          title: enabled ? (
            <span>
              <CheckCircleIcon color="green" />
              &nbsp;Enabled
            </span>
          ) : (
            <span>
              <ExclamationCircleIcon color="grey" />
              &nbsp;Disabled
            </span>
          )
        });
        break;
      case "lastUpdated":
        cells.push({
          header: "lastUpdated",
          title: updated && (
            <>
              <FormatDistance date={updated} /> ago
            </>
          )
        });
        break;
      case "lastSeen":
        cells.push({
          header: "lastSeen",
          title: lastSeen && (
            <>
              <FormatDistance date={lastSeen} /> ago
            </>
          )
        });
        break;
      case "addedDate":
        cells.push({
          header: "addedDate",
          title: created && (
            <>
              <FormatDistance date={created} /> ago
            </>
          )
        });
        break;
      case "memberOf":
        cells.push({
          header: "memberOf",
          title: memberOf && memberOf.length > 0 ? memberOf.join(", ") : "-"
        });
        break;
      case "viaGateways":
        cells.push({
          header: "memberOf",
          title: via && via.length > 0 ? via.join(", ") : "-"
        });
        break;
      default:
        break;
    }
  });

  const tableRow: IRowData = {
    selected: row.selected || false,
    cells: cells,
    originalData: row
  };

  return tableRow;
};

import React from "react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon
} from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { FormatDistance } from "use-patternfly";

import { IRowData } from "@patternfly/react-table";
import { IDevice } from "modules/iot-device/components";
import { Label } from "@patternfly/react-core";

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

export const getTableCells = (row: IDevice) => {
  const {
    enabled,
    viaGateway,
    credentials,
    deviceId,
    updated,
    created,
    lastSeen
  } = row;

  let deviceType: string;

  if (!viaGateway === !credentials?.length) {
    deviceType = "N/A";
  } else if (viaGateway) {
    deviceType = "Via gateways";
  } else {
    deviceType = "Connected directly";
  }

  const tableRow: IRowData = {
    selected: row.selected || false,
    cells: [
      {
        header: "id",
        title: (
          <span>
            <Link to={`devices/${deviceId}/device-info`}>{deviceId}</Link>
          </span>
        )
      },
      {
        header: "type",
        title: renderDeviceType(deviceType)
      },
      {
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
      },
      // TODO: The lastseen value is not being parsed from server yet
      {
        title: lastSeen && (
          <>
            <FormatDistance date={lastSeen} /> ago
          </>
        )
      },
      {
        title: updated && (
          <>
            <FormatDistance date={updated} /> ago
          </>
        )
      },
      {
        title: created && (
          <>
            <FormatDistance date={created} /> ago
          </>
        )
      }
    ],
    originalData: row
  };

  return tableRow;
};

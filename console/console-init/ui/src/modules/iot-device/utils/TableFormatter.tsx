import React from "react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon
} from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { FormatDistance } from "use-patternfly";

import { IRowData } from "@patternfly/react-table";
import { IDevice } from "modules/iot-device/components";

export const getTableCells = (row: IDevice) => {
  const { jsonData = "{}", enabled } = row;

  const { gateways, credentials } = JSON.parse(jsonData);

  let deviceType: string;

  if (!gateways?.length === !credentials?.length) {
    deviceType = "N/A";
  } else if (gateways) {
    deviceType = "Using gateways";
  } else {
    deviceType = "Using credentials";
  }

  const tableRow: IRowData = {
    selected: row.selected || false,
    cells: [
      {
        header: "id",
        title: (
          <span>
            <Link to={`devices/${row.deviceId}/device-info`}>
              {row.deviceId}
            </Link>
          </span>
        )
      },
      {
        header: "type",
        title: <span>{deviceType}</span>
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
      // TODO: The timestamps are subjected to change, the following lines of code will be changed accordingly
      {
        title: row.lastSeen && (
          <>
            <FormatDistance date={row.lastSeen} /> ago
          </>
        )
      },
      {
        title: row.updated && (
          <>
            <FormatDistance date={row.updated} /> ago
          </>
        )
      },
      {
        title: row.created && (
          <>
            <FormatDistance date={row.created} /> ago
          </>
        )
      }
    ],
    originalData: row
  };

  return tableRow;
};

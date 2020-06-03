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
  const tableRow: IRowData = {
    selected: row.selected || false,
    cells: [
      {
        header: "id",
        title: (
          <span>
            <Link to={"/"}>{row.id}</Link>
          </span>
        )
      },
      {
        header: "type",
        title: row.type && <span>{row.type}</span>
      },
      {
        title: row.status ? (
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
      {
        title: row.lastSeen && (
          <>
            <FormatDistance date={row.lastSeen} /> ago
          </>
        )
      },
      {
        title: row.lastUpdated && (
          <>
            <FormatDistance date={row.lastUpdated} /> ago
          </>
        )
      },
      {
        title: row.creationTimeStamp && (
          <>
            <FormatDistance date={row.creationTimeStamp} /> ago
          </>
        )
      }
    ],
    originalData: row
  };

  return tableRow;
};

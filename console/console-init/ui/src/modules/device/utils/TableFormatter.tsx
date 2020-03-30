import React from "react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon
} from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { FormatDistance } from "use-patternfly";

import { IRowData } from "@patternfly/react-table";
import { IDevice } from "modules/device/components";

export const getTableCells = (row: IDevice) => {
  const tableRow: IRowData = {
    selected: row.selected,
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
        title: <span>{row.type}</span>
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
        title: (
          <>
            <FormatDistance date={row.lastSeen} /> ago
          </>
        )
      },
      {
        title: (
          <>
            <FormatDistance date={row.lastUpdated} /> ago
          </>
        )
      },
      {
        title: (
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

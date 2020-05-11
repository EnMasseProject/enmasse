/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListToggle,
  DataListContent,
  DataListItemCells
} from "@patternfly/react-core";
import classNames from "classnames";
import { StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  data_list_cell: {
    marginRight: 0
  },
  key_cell_toogle: {
    marginLeft: -25
  },
  key_cell: {
    marginLeft: 50
  },
  type_cell_toggle: {
    textAlign: "center",
    textTransform: "capitalize"
  },
  type_cell: {
    textAlign: "center",
    marginLeft: 55,
    textTransform: "capitalize"
  },
  value_cell: {
    textAlign: "center"
  }
});

export type ITableRowProps = {
  rowData: any;
};

export enum DataType {
  ARRAY = "array",
  OBJECT = "object"
}

export const TableRow: React.FC<ITableRowProps> = ({ rowData }) => {
  const [expanded, setExpanded] = useState<string[]>([]);

  const toggle = (event: any) => {
    const expandedId = event.target.id;
    const index = expanded && expanded.indexOf(expandedId);
    const newExpanded =
      index >= 0
        ? [
            ...expanded.slice(0, index),
            ...expanded.slice(index + 1, expanded.length)
          ]
        : [...expanded, expandedId];
    setExpanded(newExpanded);
  };

  const shouldDisplayDataListToggle = (
    type: DataType.ARRAY | DataType.OBJECT
  ) => {
    /**
     * check parent data type is Array or Object.
     * Toggle will add for parent node only
     */
    if (type === DataType.OBJECT || type === DataType.ARRAY) {
      return true;
    }
    return false;
  };

  const { key, type, value } = rowData || {};

  const cssClassKey = classNames({
    [styles.key_cell_toogle]: shouldDisplayDataListToggle(type),
    [styles.key_cell]: !shouldDisplayDataListToggle(type)
  });

  const cssClassType = classNames({
    [styles.type_cell_toggle]: shouldDisplayDataListToggle(type),
    [styles.type_cell]: !shouldDisplayDataListToggle(type)
  });

  const cssClassValue = classNames([styles.value_cell]);

  return (
    <>
      <DataListItem
        aria-labelledby={key + " data list item"}
        isExpanded={shouldDisplayDataListToggle(type) && expanded.includes(key)}
      >
        <DataListItemRow>
          {shouldDisplayDataListToggle(type) && (
            <DataListToggle
              onClick={toggle}
              isExpanded={expanded.includes(key)}
              id={key}
              aria-controls={key + " data list toggle"}
            />
          )}
          <DataListItemCells
            dataListCells={[
              <DataListCell key="key-content">
                <div className={cssClassKey}>{key}</div>
              </DataListCell>,
              <DataListCell key="type-content">
                <div className={cssClassType}>{type}</div>
              </DataListCell>,
              <DataListCell key="value-content">
                <div className={cssClassValue}>
                  {type === DataType.OBJECT || type === DataType.ARRAY
                    ? "- -"
                    : value}
                </div>
              </DataListCell>
            ]}
          ></DataListItemCells>
        </DataListItemRow>
        {Array.isArray(value) &&
          value.map((data: any) => (
            <DataListContent
              aria-label={key + " data list content"}
              id={key}
              isHidden={!expanded.includes(key)}
              noPadding
              key={key}
            >
              <TableRow rowData={data} />
            </DataListContent>
          ))}
      </DataListItem>
    </>
  );
};

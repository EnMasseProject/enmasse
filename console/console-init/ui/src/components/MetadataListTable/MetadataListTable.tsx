/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import classNames from "classnames";
import {
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  Title
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { convertJsonToMetadataOptions } from "utils";
import { TableRow, IRowOption } from "./TableRow";
import { NoMetadataFound } from "./NoMetadataFound";

const styles = StyleSheet.create({
  header_magin_left: {
    marginLeft: 45
  },
  header_text_align_center: {
    textAlign: "center"
  }
});

interface IHeaderProps {
  headers?: string[];
  ["aria-labelledby"]: string;
}

interface IDataList {
  headers: string[];
  data: any;
}

export interface IMetadataListTablePorps {
  id: string;
  dataList: IDataList[];
  ["aria-label"]: string;
  ["aria-labelledby"]: string;
}

const TableHeader: React.FC<IHeaderProps> = ({
  headers,
  "aria-labelledby": ariaLabelledby
}) => {
  const dataListCells = () =>
    headers &&
    headers.map((header: string, index: number) => {
      const cssClass = classNames({
        [css(styles.header_magin_left)]: index === 0,
        [css(styles.header_text_align_center)]: index === 1 || index === 2
      });

      return (
        <DataListCell key={index} className={cssClass}>
          <Title headingLevel="h6" size="md">
            {header}
          </Title>
        </DataListCell>
      );
    });

  return (
    <DataListItem aria-labelledby={ariaLabelledby}>
      <DataListItemRow>
        <DataListItemCells dataListCells={dataListCells()}></DataListItemCells>
      </DataListItemRow>
    </DataListItem>
  );
};

export const MetadataListTable: React.FC<IMetadataListTablePorps> = ({
  id,
  dataList,
  "aria-label": ariaLabel,
  "aria-labelledby": ariaLabelledby
}) => {
  const addMetadata = () => {
    /**
     * TODO: redirect on add metadata page
     */
  };

  return (
    <div id={id}>
      {dataList?.length > 0 && (
        <DataList aria-label={ariaLabel}>
          {dataList.map((list: IDataList) => {
            const { headers, data } = list;
            const metadataOptions = data && convertJsonToMetadataOptions(data);
            return (
              <>
                <TableHeader
                  headers={headers}
                  aria-labelledby={ariaLabelledby}
                />
                {metadataOptions &&
                  metadataOptions.map((row: IRowOption) => (
                    <TableRow
                      id={"ml-table-row-" + row.key}
                      rowData={row}
                      key={row.key}
                    />
                  ))}
              </>
            );
          })}
        </DataList>
      )}
      {dataList?.length <= 0 && (
        <NoMetadataFound id="nometadata-found" addMetadata={addMetadata} />
      )}
    </div>
  );
};

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
import { StyleSheet } from "@patternfly/react-styles";
import { getJsonForMetadata } from "utils";
import { TableRow } from "./TableRow";

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
  ["aria-labelledby-header"]: string;
}

const TableHeader: React.FC<IHeaderProps> = ({
  headers,
  "aria-labelledby": ariaLabelledby
}) => {
  const dataListCells = () =>
    headers &&
    headers.map((header: string, index: number) => {
      const cssClass = classNames({
        [styles.header_magin_left]: index === 0,
        [styles.header_text_align_center]: index === 1 || index === 2
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
  "aria-labelledby-header": ariaLabelledby
}) => {
  return (
    <DataList aria-label={ariaLabel} id={id}>
      {dataList &&
        dataList.map((list: IDataList) => {
          const { headers, data } = list;
          const metadataOptions = getJsonForMetadata(data);
          return (
            <>
              <TableHeader headers={headers} aria-labelledby={ariaLabelledby} />
              {metadataOptions &&
                metadataOptions.map((row: any) => <TableRow rowData={row} />)}
            </>
          );
        })}
    </DataList>
  );
};

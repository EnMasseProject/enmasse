/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Card, CardBody, CardHeader, Title } from "@patternfly/react-core";
import {
  MetadataListTable,
  IMetadataListTablePorps
} from "components/MetadataListTable";

export interface IDeviceInfoMetadataProps
  extends Pick<IMetadataListTablePorps, "dataList"> {
  id: string;
}

export const DeviceInfoMetadata: React.FC<IDeviceInfoMetadataProps> = ({
  id,
  dataList
}) => {
  return (
    <Card id={id}>
      <CardHeader>
        <Title headingLevel="h1" size="2xl">
          Device metadata
        </Title>
      </CardHeader>
      <CardBody>
        <MetadataListTable
          dataList={dataList}
          id={"divice-info-metadata-table"}
          aria-label={"device info metadata"}
          aria-labelledby-header={"device info metadata header"}
        />
      </CardBody>
    </Card>
  );
};

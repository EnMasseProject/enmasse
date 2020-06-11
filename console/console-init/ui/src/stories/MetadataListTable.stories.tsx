/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MetadataListTable } from "components";

export default {
  title: "MetadataTable"
};

const metadataList = [
  {
    headers: ["Message info parameter", "Type", "Value"],
    data: {
      "content-type-1": "text/plain",
      "content-type-2": "text/plain",
      "content-type-3": "text/plain",
      long: 12.3544
    }
  },
  {
    headers: ["Basic info parameter", "Type", "Value"],
    data: {
      custom: {
        level: 0,
        serial_id: "0000",
        location: {
          long: 1.234,
          lat: 5.678
        },
        features: ["foo", "bar", "baz"]
      }
    }
  }
];

export const MetadataTable = () => (
  <div style={{ overflow: "auto" }}>
    <MetadataListTable
      id={"metadata-table"}
      dataList={metadataList}
      aria-label={"metadata table"}
      aria-labelledby={"medata data header"}
    />
  </div>
);

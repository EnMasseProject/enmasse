/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Title, Grid, GridItem } from "@patternfly/react-core";
import { Table, TableHeader, TableBody } from "@patternfly/react-table";
import { StyleSheet } from "@patternfly/react-styles";
import { getJsonForMetadata } from "utils";

const styles = StyleSheet.create({
  type: {
    textTransform: "capitalize"
  },
  row_margin: {
    marginBottom: 5
  },
  section_margin: {
    marginTop: 20,
    marginBottom: 10
  }
});

export interface IExtensionsViewProps {
  id: string;
  ext: any;
  heading: string;
}

export const ExtensionsView: React.FC<IExtensionsViewProps> = ({
  id,
  ext,
  heading
}) => {
  const columns = [
    {
      title: (
        <Title headingLevel="h1" size="md">
          <b>Parameter</b>
        </Title>
      )
    },
    {
      title: (
        <Title headingLevel="h1" size="md">
          <b>Type</b>
        </Title>
      )
    },
    {
      title: (
        <Title headingLevel="h1" size="md">
          <b>Value</b>
        </Title>
      )
    }
  ];

  const extOptions = getJsonForMetadata(ext);

  const rows = extOptions.map((ext: any) => {
    const { key, value, typeLabel } = ext || {};
    const cells = [
      { header: "parameter", title: key },
      {
        header: "type",
        title: <span className={styles.type}>{typeLabel}</span>
      },
      { header: "value", title: value }
    ];
    return cells;
  });

  return (
    <>
      {extOptions && extOptions.length > 0 && (
        <Grid id={id}>
          <GridItem span={12} className={styles.section_margin}>
            {heading}
          </GridItem>
          <Table aria-label="extensions view table" cells={columns} rows={rows}>
            <TableHeader />
            <TableBody />
          </Table>
        </Grid>
      )}
    </>
  );
};

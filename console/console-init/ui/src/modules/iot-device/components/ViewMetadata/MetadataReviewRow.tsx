/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { StyleSheet, css } from "aphrodite";
import { AngleRightIcon, AngleDownIcon } from "@patternfly/react-icons";
import {
  GridItem,
  TextInput,
  Grid,
  Button,
  getUniqueId
} from "@patternfly/react-core";
import { MetadataReviewRows } from "./MetadataReviewRows";
import { DataType } from "constant";

const styles = StyleSheet.create({
  grid_align: {
    marginTop: "1rem",
    marginRight: "0.5rem",
    marginLeft: "0.5rem"
  },
  grid_button_align: {
    textAlign: "right",
    marginTop: "1rem",
    padding: "0 !important"
  }
});

interface IMetadataReviewRowProps {
  metadataRow: any;
  prevKey?: string;
  viewAll?: boolean;
}

const MetadataReviewRow: React.FunctionComponent<IMetadataReviewRowProps> = ({
  metadataRow,
  prevKey,
  viewAll = true
}) => {
  const [isVisible, setIsVisible] = useState<boolean>(viewAll);

  const onToggle = () => {
    setIsVisible(!isVisible);
  };
  const renderGridItem = (value: string) => {
    return (
      <GridItem span={3} className={css(styles.grid_align)}>
        <TextInput
          key={getUniqueId()}
          id={getUniqueId()}
          type="text"
          value={value}
          isReadOnly={true}
        />
      </GridItem>
    );
  };

  const hasObjectValue: boolean =
    metadataRow.type === DataType.ARRAY || metadataRow.type === DataType.OBJECT;
  const key: string = prevKey
    ? metadataRow.key !== ""
      ? prevKey + "/" + metadataRow.key
      : prevKey
    : metadataRow.key;
  return (
    <>
      <Grid>
        <GridItem span={1} className={css(styles.grid_button_align)}>
          {hasObjectValue && (
            <Button
              variant="plain"
              id={key}
              style={{ textAlign: "right" }}
              onClick={onToggle}
            >
              {isVisible ? <AngleDownIcon /> : <AngleRightIcon />}
            </Button>
          )}
        </GridItem>
        {renderGridItem(key)}
        {renderGridItem(metadataRow.typeLabel)}
        {renderGridItem(hasObjectValue ? "" : metadataRow.value)}
        {hasObjectValue && isVisible && (
          <>
            <MetadataReviewRows
              metadataRows={metadataRow.value}
              prevkey={key}
            />
          </>
        )}
      </Grid>
    </>
  );
};
export { MetadataReviewRow };

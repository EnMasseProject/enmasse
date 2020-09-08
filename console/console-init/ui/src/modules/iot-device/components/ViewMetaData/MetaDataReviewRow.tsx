/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { StyleSheet, css } from "aphrodite";
import { AngleRightIcon, AngleDownIcon } from "@patternfly/react-icons";
import { GridItem, TextInput, Grid, Button } from "@patternfly/react-core";
import { MetaDataReviewRows } from "./MetaDataReviewRows";
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

interface IMetaDataReviewRowProps {
  value: any;
  prevKey?: string;
  index?: number;
  viewAll?: boolean;
}

const MetaDataReviewRow: React.FunctionComponent<IMetaDataReviewRowProps> = ({
  value,
  prevKey,
  index,
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
          key={value + index}
          id={value + index}
          type="text"
          value={value}
          isReadOnly={true}
        />
      </GridItem>
    );
  };

  const hasObjectValue: boolean =
    value.type === DataType.ARRAY || value.type === DataType.OBJECT;
  const key: string = prevKey
    ? value.key !== ""
      ? prevKey + "/" + value.key
      : prevKey
    : value.key;
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
        {renderGridItem(value.typeLabel)}
        {renderGridItem(hasObjectValue ? "" : value.value)}
        {hasObjectValue && isVisible && (
          <>
            <MetaDataReviewRows values={value.value} prevkey={key} />
          </>
        )}
      </Grid>
    </>
  );
};
export { MetaDataReviewRow };

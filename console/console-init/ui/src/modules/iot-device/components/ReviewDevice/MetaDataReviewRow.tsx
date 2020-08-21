/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { StyleSheet, css } from "aphrodite";
import { AngleRightIcon, AngleDownIcon } from "@patternfly/react-icons";
import { GridItem, TextInput, Grid, Button } from "@patternfly/react-core";
import { MetaDataReviewRows } from "./MetaDataReviewRows";

const styles = StyleSheet.create({
  grid_align: { marginTop: 10, marginRight: 5, marginLeft: 5 },
  grid_button_align: {
    textAlign: "right",
    marginTop: 10,
    marginRight: 5,
    marginLeft: 5
  }
});

interface IMetaDataReviewRowProps {
  value: any;
  prevKey?: string;
  index?: number;
}

const MetaDataReviewRow: React.FunctionComponent<IMetaDataReviewRowProps> = ({
  value,
  prevKey,
  index
}) => {
  const [isVisible, setIsVisible] = useState<boolean>(false);

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
    value.type === "array" || value.type === "object";
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

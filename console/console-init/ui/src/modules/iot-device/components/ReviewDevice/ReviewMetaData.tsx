/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import { convertJsonToMetadataOptions } from "utils";
import { StyleSheet, css } from "aphrodite";
import { MetaDataReviewRows } from "./MetaDataReviewRows";

const styles = StyleSheet.create({
  text_center_align: { textAlign: "center" }
});
interface IReviewMetaDataProrps {
  defaults?: any[];
  ext?: any[];
}
const ReviewMetaData: React.FunctionComponent<IReviewMetaDataProrps> = ({
  defaults,
  ext
}) => {
  if (ext === undefined && defaults === undefined) {
    return <>--</>;
  }
  const convertedDefaultsData = convertJsonToMetadataOptions(defaults);
  const convertedExtData = convertJsonToMetadataOptions(ext);
  if (convertedDefaultsData.length === 0 && convertedExtData.length === 0) {
    return <>--</>;
  }
  const renderGrid = (values: any[], label: string) => {
    return (
      <>
        <Grid>
          <GridItem span={1}></GridItem>
          <GridItem span={3} className={css(styles.text_center_align)}>
            <b>{label}</b>
          </GridItem>
          <GridItem span={3} className={css(styles.text_center_align)}>
            <b>Type</b>
          </GridItem>
          <GridItem span={3} className={css(styles.text_center_align)}>
            <b>Value</b>
          </GridItem>
        </Grid>
        <MetaDataReviewRows values={values} />
      </>
    );
  };
  return (
    <>
      {convertedDefaultsData.length > 0 &&
        renderGrid(convertedDefaultsData, "Default properties parameter")}
      <br />
      {convertedExtData.length > 0 &&
        renderGrid(convertedExtData, "Extension parameter")}
    </>
  );
};

export { ReviewMetaData };

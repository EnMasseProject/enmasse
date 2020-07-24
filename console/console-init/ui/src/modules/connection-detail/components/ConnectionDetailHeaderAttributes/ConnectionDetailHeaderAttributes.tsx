/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Flex, FlexItem } from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  flex_right_border_with_padding: {
    paddingRight: "48px",
    marginRight: "48px",
    borderRight: "2px solid",
    borderRightColor: "lightgrey"
  },
  flex_bottom_boder: {
    borderBottom: "2px solid",
    borderBottomColor: "lightgrey",
    paddingBottom: "12px"
  }
});
export interface IConnectionDetailHeaderAttributesProps {
  isMobileView: boolean;
  product?: string;
  version?: string;
  jvm?: string;
  os?: string;
}
export const ConnectionDetailHeaderAttributes: React.FunctionComponent<IConnectionDetailHeaderAttributesProps> = ({
  isMobileView,
  product,
  version,
  jvm,
  os
}) => {
  return (
    <Flex
      direction={{ sm: "column" }}
      className={
        !isMobileView
          ? css(styles.flex_right_border_with_padding)
          : css(styles.flex_bottom_boder)
      }
    >
      <Flex>
        <FlexItem id="connection-detail-header-attr-product-flexitem">
          <b>Product</b> {product || "-"}
        </FlexItem>
        <FlexItem id="connection-detail-header-attr-version-flexitem">
          <b>Version </b>
          {version || "-"}
        </FlexItem>
      </Flex>
      <Flex>
        <FlexItem id="connection-detail-header-attr-platform-flexitem">
          <b>Platform</b>
        </FlexItem>
        <FlexItem>
          <Flex direction={{ lg: "row", sm: "column" }}>
            <FlexItem id="connection-detail-header-attr-jvm-flexitem">
              <b>JVM: </b>
              {jvm || "-"}
            </FlexItem>
            <FlexItem id="connection-detail-header-attr-os-flexitem">
              <b>OS: </b>
              {os || "-"}
            </FlexItem>
          </Flex>
        </FlexItem>
      </Flex>
    </Flex>
  );
};

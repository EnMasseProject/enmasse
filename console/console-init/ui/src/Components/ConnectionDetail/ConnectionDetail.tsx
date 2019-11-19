import * as React from "react";
import { Flex, FlexItem } from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  flex_right_boder_with_padding: {
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
export interface IConnectionDetailProps {
  product: string;
  version: string;
  jvm: string;
  os: string;
  isMobileView: boolean;
}
export const ConnectionDetail: React.FunctionComponent<
  IConnectionDetailProps
> = ({ product, version, jvm, os, isMobileView }) => {
  return (
    <Flex
      breakpointMods={[{ modifier: "column", breakpoint: "sm" }]}
      className={
        !isMobileView
          ? css(styles.flex_right_boder_with_padding)
          : css(styles.flex_bottom_boder)
      }
    >
      <Flex>
        <FlexItem>
          <b>Product</b> {product}
        </FlexItem>
        <FlexItem>
          <b>Version </b>
          {version} SNAPSHOT
        </FlexItem>
      </Flex>
      <Flex>
        <FlexItem>
          <b>Platform</b>
        </FlexItem>
        <FlexItem>
          <Flex
            breakpointMods={[
              { modifier: "row", breakpoint: "lg" },
              { modifier: "column", breakpoint: "sm" }
            ]}
          >
            <FlexItem>
              <b>JVM: </b>
              {jvm}
            </FlexItem>
            <FlexItem>
              <b>OS: </b>
              {os}
            </FlexItem>
          </Flex>
        </FlexItem>
      </Flex>
    </Flex>
  );
};

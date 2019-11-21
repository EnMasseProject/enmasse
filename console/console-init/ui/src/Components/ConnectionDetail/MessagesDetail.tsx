import * as React from "react";
import { Flex, FlexItem } from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  message_in_flex: {
    paddingTop: 15,
    textAlign: "center",
    paddingLeft: 48,
    fontSize: 21
  },
  message_out_flex: {
    textAlign: "center",
    paddingLeft: 48,
    fontSize: 21
  }
});
export interface IMessagesDetail {
  messagesIn?: number;
  messagesOut?: number;
  isMobileView: boolean;
}

export const MessagesDetail: React.FunctionComponent<IMessagesDetail> = ({
  messagesIn,
  messagesOut,
  isMobileView
}) => {
  return (
    <Flex breakpointMods={[{ modifier: "row", breakpoint: "sm" }]}>
      <FlexItem className={css(styles.message_in_flex)}>
        {messagesIn || messagesIn === 0 ? messagesIn : "-"}{" "}
        {isMobileView ? "" : <br />} Message in
      </FlexItem>
      <FlexItem className={css(styles.message_out_flex)}>
        {messagesOut || messagesOut === 0 ? messagesOut : "-"}{" "}
        {isMobileView ? "" : <br />} Message out
      </FlexItem>
    </Flex>
  );
};

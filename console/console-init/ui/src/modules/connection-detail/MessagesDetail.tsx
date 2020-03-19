/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Split, SplitItem } from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  message_split: {
    paddingTop: 15,
    textAlign: "center",
    paddingLeft: 48,
    fontSize: 21
  }
});
export interface IMessagesDetail {
  messageIn?: number | string;
  messageOut?: number | string;
  addressSpaceType?: string;
  isMobileView: boolean;
}

export const MessagesDetail: React.FunctionComponent<IMessagesDetail> = ({
  messageIn,
  messageOut,
  addressSpaceType,
  isMobileView
}) => {
  return (
    <Split id="message-detail">
      <SplitItem
        id="message-detail-message-in"
        span={6}
        className={css(styles.message_split)}
      >
        {messageIn || messageIn === "" ? messageIn : 0}
        {isMobileView ? "" : <br />} Message in/sec
      </SplitItem>
      {!addressSpaceType || addressSpaceType === "brokered" ? (
        ""
      ) : (
        <SplitItem
          id="message-detail-message-out"
          span={6}
          className={css(styles.message_split)}
        >
          {messageOut || messageOut === "" ? messageOut : 0}
          {isMobileView ? "" : <br />} Message out/sec
        </SplitItem>
      )}
    </Split>
  );
};

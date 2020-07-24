/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Split, SplitItem } from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  message_split: {
    paddingTop: 15,
    textAlign: "center",
    paddingLeft: 48,
    fontSize: 21
  }
});
export interface IMessagesDetailHeaderAttributes {
  messageIn?: number | string;
  messageOut?: number | string;
  addressSpaceType?: string;
  isMobileView: boolean;
}

export const MessagesDetailHeaderAttributes: React.FunctionComponent<IMessagesDetailHeaderAttributes> = ({
  messageIn,
  messageOut,
  addressSpaceType,
  isMobileView
}) => {
  return (
    <Split id="message-detail-split">
      <SplitItem
        id="message-detail-message-in-splititem"
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
          id="message-detail-message-out-splititem"
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

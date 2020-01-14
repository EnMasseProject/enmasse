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
    <Split>
      <SplitItem span={6} className={css(styles.message_split)}>
        {messagesIn || messagesIn === 0 ? messagesIn : "-"}{" "}
        {isMobileView ? "" : <br />} Message in
      </SplitItem>
      <SplitItem span={6} className={css(styles.message_split)}>
        {messagesOut || messagesOut === 0 ? messagesOut : "-"}{" "}
        {isMobileView ? "" : <br />} Message out
      </SplitItem>
    </Split>
  );
};

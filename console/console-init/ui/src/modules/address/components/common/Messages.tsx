/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";

interface IMessagesProps {
  count: any;
  column: string;
  status?: string;
  isReady: boolean;
}

export const Messages: React.FunctionComponent<IMessagesProps> = message => {
  return <>{message.count}</>;
};

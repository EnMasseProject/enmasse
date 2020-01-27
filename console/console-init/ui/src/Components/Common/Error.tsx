/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { ExclamationCircleIcon, InProgressIcon } from "@patternfly/react-icons";

interface IErrorProps {
  message: string;
  type?: string;
}

export const Error: React.FunctionComponent<IErrorProps> = ({
  message,
  type
}) => {
  return type === "Pending" ? (
    <React.Fragment>
      <ExclamationCircleIcon color="red" />
      &nbsp;
      {message}
    </React.Fragment>
  ) : (
      <>
        <InProgressIcon />
        &nbsp;
      {message}
      </>
    );
};

import * as React from "react";
import { ExclamationCircleIcon } from "@patternfly/react-icons";

interface IErrorProps {
  message: string;
}

export const Error: React.FunctionComponent<IErrorProps> = error => {
  return (
    <React.Fragment>
      <ExclamationCircleIcon />
      {error.message}
    </React.Fragment>
  );
};

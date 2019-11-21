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

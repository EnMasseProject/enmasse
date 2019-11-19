import * as React from "react";
import { CheckCircleIcon, TimesCircleIcon } from "@patternfly/react-icons";
import { Badge } from "@patternfly/react-core";

interface AddressSpaceTypeProps {
  type: string;
}
interface AddressSpaceStatusProps {
  isReady: boolean;
}
const typeToDisplay = (type: string) => {
  switch (type) {
    case "standard":
      return " Standard";
    case "brokered":
      return " Brokered";
  }
};
export const AddressSpaceIcon = () => {
  return <Badge style={{ backgroundColor: "#EC7A08" }}>AS</Badge>;
};
const statusToDisplay = (isReady: boolean) => {
  return isReady ? (
    <>
      <CheckCircleIcon color="green" /> Active
    </>
  ) : (
    <>
      <TimesCircleIcon color="red" /> Failed
    </>
  );
};
export const AddressSpaceType: React.FunctionComponent<
  AddressSpaceTypeProps
> = ({ type }) => {
  return <>{typeToDisplay(type)}</>;
};

export const AddressSpaceStatus: React.FunctionComponent<
  AddressSpaceStatusProps
> = ({ isReady }) => {
  return <>{statusToDisplay(isReady)}</>;
};

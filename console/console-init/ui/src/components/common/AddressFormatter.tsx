import { statusToDisplay } from "./AddressSpaceListFormatter";

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
interface IAddressStatusProps {
  phase: string;
}
interface IAddressErrorMessageProps {
  messages: Array<string>;
}
export const getPlanAndTypeForAddress = (
  plan: string,
  addressType: string,
  type: string
) => {
  return (
    type.toLowerCase() +
    "-" +
    plan.toLowerCase() +
    "-" +
    addressType.toLowerCase()
  );
};

export const getPlanAndTypeForAddressEdit = (
  plan: string,
  addressSpaceType: string
) => {
  return (
    addressSpaceType.toLowerCase() + "-" + plan.toLowerCase().replace(" ", "-")
  );
};

export const AddressStatus: React.FunctionComponent<IAddressStatusProps> = ({
  phase
}) => {
  return statusToDisplay(phase);
};

import { statusToDisplay } from "modules/address-space";
import { AddressTypes } from "constant";

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

interface IAddressStatusProps {
  phase: string;
}
export interface IFilterValue {
  value: string;
  isExact: boolean;
}

const getFilteredAddressesByType = (addresses: any[]) => {
  if (addresses && addresses.length > 0) {
    return addresses.filter(
      address =>
        address.type.toLowerCase() === AddressTypes.QUEUE ||
        address.type.toLowerCase() === AddressTypes.SUBSCRIPTION
    );
  }
};

const getHeaderTextForPurgeAll = (addresses: any[]) => {
  const filteredAddresses = getFilteredAddressesByType(addresses);
  const header =
    filteredAddresses && filteredAddresses.length > 1
      ? "Purge these Addresses ?"
      : "Purge this Address ?";
  return header;
};

const getFilteredAddressDisplayName = (addresses: any[]) => {
  if (addresses && addresses.length > 0) {
    return addresses.filter(
      address =>
        address.type.toLowerCase() === AddressTypes.QUEUE ||
        address.type.toLowerCase() === AddressTypes.SUBSCRIPTION
    )[0].displayName;
  }
};

const getDetailTextForPurgeAll = (addresses: any[]) => {
  const filteredAddresses = getFilteredAddressesByType(addresses);
  const filteredDispalyName = getFilteredAddressDisplayName(addresses);
  let detail = "";

  if (filteredAddresses && filteredAddresses.length > 1) {
    detail = `Are you sure you want to purge all of these addresses: ${filteredAddresses.map(
      address => " " + address.displayName
    )} ?`;
  } else {
    detail = `Are you sure you want to purge this address: ${filteredAddresses &&
      filteredDispalyName} ?`;
  }
  return detail;
};

const getFilteredAdressNames = (addresses: any[]) => {
  return (
    addresses &&
    addresses
      .filter(
        address =>
          address.type.toLowerCase() === AddressTypes.QUEUE ||
          address.type.toLowerCase() === AddressTypes.SUBSCRIPTION
      )
      .map(address => address.name)
  );
};

const getHeaderTextForDelateAll = (addresses: any[]) => {
  return addresses && addresses.length > 1
    ? "Delete these Addresses ?"
    : "Delete this Address ?";
};

const getDetailTextForDeleteAll = (addresses: any[]) => {
  return addresses && addresses.length > 1
    ? `Are you sure you want to delete all of these addresses: ${addresses.map(
        as => " " + as.displayName
      )} ?`
    : `Are you sure you want to delete this address: ${addresses[0].displayName} ?`;
};

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

const getPlanAndTypeForAddressEdit = (
  plan: string,
  addressSpaceType: string
) => {
  return (
    addressSpaceType.toLowerCase() + "-" + plan.toLowerCase().replace(" ", "-")
  );
};

const AddressStatus: React.FunctionComponent<IAddressStatusProps> = ({
  phase
}) => {
  return statusToDisplay(phase);
};

export {
  getFilteredAddressesByType,
  getHeaderTextForPurgeAll,
  getDetailTextForPurgeAll,
  getFilteredAddressDisplayName,
  getFilteredAdressNames,
  getHeaderTextForDelateAll,
  getDetailTextForDeleteAll,
  AddressStatus,
  getPlanAndTypeForAddressEdit
};

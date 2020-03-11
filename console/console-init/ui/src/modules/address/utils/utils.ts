/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const getFilteredAddressesByType = (addresses: any[]) => {
  if (addresses && addresses.length > 0) {
    return addresses.filter(
      address =>
        address.type.toLowerCase() === "queue" ||
        address.type.toLowerCase() === "subscription"
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
        address.type.toLowerCase() === "queue" ||
        address.type.toLowerCase() === "subscription"
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
          address.type.toLowerCase() === "queue" ||
          address.type.toLowerCase() === "subscription"
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

export {
  getFilteredAddressesByType,
  getHeaderTextForPurgeAll,
  getDetailTextForPurgeAll,
  getFilteredAddressDisplayName,
  getFilteredAdressNames,
  getHeaderTextForDelateAll,
  getDetailTextForDeleteAll
};

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

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
  addressSpaceType: string,
) => {
  return (
    addressSpaceType.toLowerCase() +
    "-" +
    plan.toLowerCase().replace(" ","-")
  );
};

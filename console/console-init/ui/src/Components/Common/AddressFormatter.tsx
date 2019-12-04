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

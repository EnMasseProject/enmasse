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

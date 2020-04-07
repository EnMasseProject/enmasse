export const dnsSubDomainRfc1123NameRegexp = new RegExp(
  "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$"
);
export const messagingAddressNameRegexp = new RegExp("^[^#*\\s]+$");
export const forbiddenBackslashRegexp = new RegExp(/\\/g);
export const forbiddenSingleQuoteRegexp = new RegExp(/'/g);
export const forbiddenDoubleQuoteRegexp = new RegExp(/"/g);

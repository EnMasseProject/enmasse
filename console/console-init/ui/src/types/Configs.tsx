/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export const dnsSubDomainRfc1123NameRegexp = new RegExp(
  "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$"
);
export const messagingAddressNameRegexp = new RegExp("^[^#*\\s]+$");
export const forbiddenBackslashRegexp = new RegExp(/\\/g);
export const forbiddenSingleQuoteRegexp = new RegExp(/'/g);
export const forbiddenDoubleQuoteRegexp = new RegExp(/"/g);
export const deviceIDRegExp = new RegExp(/^[A-Z][A-Z0-9._(){}#@-]+$/i);

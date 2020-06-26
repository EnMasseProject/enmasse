/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { IDropdownOption } from "components";
import { CredentialsType } from "constant";

const credentialsTypeOptions: IDropdownOption[] = [
  {
    key: "all-credentials",
    value: "all",
    label: "All credentials"
  },
  {
    key: "enabled",
    value: "enabled",
    label: "Enabled credentials",
    separator: true
  },
  {
    key: CredentialsType.PASSWORD,
    value: CredentialsType.PASSWORD,
    label: "Password"
  },
  { key: CredentialsType.PSK, value: CredentialsType.PSK, label: "PSK" },
  {
    key: CredentialsType.X509_CERTIFICATE,
    value: CredentialsType.X509_CERTIFICATE,
    label: "X.509 certificate"
  }
];

const getDefaultCredentialsFiterOption = (credentialType: string) => {
  let filterOptions: IDropdownOption[] = [];
  switch (credentialType) {
    case CredentialsType.PASSWORD:
      filterOptions.push({
        key: "all",
        value: "all",
        label: "All passwords"
      });
      break;
    case CredentialsType.PSK:
      filterOptions.push({
        key: "all",
        value: "all",
        label: "All PSK"
      });
      break;
    case CredentialsType.X509_CERTIFICATE:
      filterOptions.push({
        key: "all",
        value: "all",
        label: "All X.590 certificates"
      });
      break;
    default:
      break;
  }
  return filterOptions;
};

export { credentialsTypeOptions, getDefaultCredentialsFiterOption };

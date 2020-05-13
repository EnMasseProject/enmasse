/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const getProductFilteredValue = (object: any[], value: string) => {
  if (object && object != null) {
    const filtered = object.filter(obj => obj.Key === value);
    if (filtered.length > 0) {
      return filtered[0].Value;
    }
  }
  return "-";
};

const getSplitValue = (value: string) => {
  let string1 = value.split(", OS: ");
  let string2 = string1[0].split("JVM:");
  let os, jvm;
  if (string1.length > 1) {
    os = string1[1];
  }
  if (string2.length > 0) {
    jvm = string2[1];
  }
  return { jvm: jvm, os: os };
};

export { getProductFilteredValue, getSplitValue };

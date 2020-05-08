/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import {
  MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN,
  TypeAheadMessage,
  NUMBER_OF_RECORDS_TO_DISPLAY_IF_SERVER_HAS_MORE_DATA
} from "constant";
import {
  forbiddenBackslashRegexp,
  forbiddenSingleQuoteRegexp,
  forbiddenDoubleQuoteRegexp
} from "types/Configs";

export interface ISelectOption {
  value: string;
  isDisabled?: boolean;
  key?: string;
  label?: string;
}

/**
 * Create a object of type ISelectOption
 * @param value
 * @param isDisabled
 */
const createSelectOptionObject = (value: string, isDisabled: boolean) => {
  const data: ISelectOption = { value: value, isDisabled: isDisabled };
  return data;
};

/**
 * Returns a Array of ISelcetOption to populate dropdown for TypeAHead Component
 * @param list //Array of strings to create SelectOption
 * @param totalRecords // Number of records present in the server
 */
const getSelectOptionList = (list: string[], totalRecords: number) => {
  const uniqueList = Array.from(new Set(list));
  let records: ISelectOption[] = [];
  if (totalRecords > MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN) {
    const allRecords = [...uniqueList];
    const top_10_records = allRecords.splice(
      0,
      NUMBER_OF_RECORDS_TO_DISPLAY_IF_SERVER_HAS_MORE_DATA
    );
    if (top_10_records.length >= 10) {
      records.push({
        value: TypeAheadMessage.MORE_CHAR_REQUIRED,
        isDisabled: true
      });
    }
    top_10_records.map((data: string) =>
      records.push(createSelectOptionObject(data, false))
    );
  } else {
    uniqueList.map((data: string) =>
      records.push(createSelectOptionObject(data, false))
    );
  }
  return records;
};

const compareObject = (obj1: any, obj2: any) => {
  if (obj1 && obj2) {
    return JSON.stringify(obj1) === JSON.stringify(obj2);
  }
};

const getType = (type: string) => {
  switch (type && type.toLowerCase()) {
    case "standard":
      return " Standard";
    case "brokered":
      return " Brokered";
  }
};

const removeForbiddenChars = (input: string) => {
  let escapedInput = input.replace(forbiddenBackslashRegexp, "\\\\");
  escapedInput = escapedInput.replace(forbiddenSingleQuoteRegexp, "''");
  escapedInput = escapedInput.replace(forbiddenDoubleQuoteRegexp, '\\"');
  return escapedInput;
};

export const getTypeColor = (type: string) => {
  let iconColor = "";
  switch (type.toUpperCase()) {
    case "Q": {
      iconColor = "#8A8D90";
      break;
    }
    case "T": {
      iconColor = "#8481DD";
      break;
    }
    case "S": {
      iconColor = "#EC7A08";
      break;
    }
    case "M": {
      iconColor = "#009596";
      break;
    }
    case "A": {
      iconColor = "#F4C145";
      break;
    }
  }
  return iconColor;
};

const dnsSubDomainRfc1123NameRegexp = new RegExp(
  "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$"
);
const messagingAddressNameRegexp = new RegExp("^[^#*\\s]+$");

const kFormatter = (num: number) => {
  const absoluteNumber: number = Math.abs(num);
  const sign = Math.sign(num);
  return absoluteNumber > 999
    ? sign * parseInt((absoluteNumber / 1000).toFixed(1)) + "k"
    : sign * absoluteNumber;
};

const uniqueId = () => {
  return Math.random()
    .toString(16)
    .slice(-4);
};

const findIndexByProperty = (
  items: any[],
  targetProperty: string,
  targetPropertyValue: string
) => {
  if (items && targetProperty && targetPropertyValue) {
    return items.findIndex(
      item => item[targetProperty] === targetPropertyValue
    );
  }
  return -1;
};

const hasOwnProperty = (obj: Object, property: string) => {
  if (obj && property) {
    return obj.hasOwnProperty(property);
  }
};

const getCombinedString = (a: string, b?: string) => {
  let s: string = "";
  s += a;
  if (b !== undefined) {
    s += ", ";
    s += b;
  }
  return s;
};

const getLabelForTypeOfObject = (value: any) => {
  switch (typeof value) {
    case "object": {
      if (Array.isArray(value)) {
        return "Array";
      } else {
        return "Object";
      }
    }
    case "string": {
      //TODO: add validations for date and time
      return "String";
    }
    case "number":
      return "Numeric";
    case "boolean":
      return "Boolean";
  }
};

const getJsonForMetadata = (object: any, type?: string) => {
  const keys = Object.keys(object);
  let data = [];
  for (var i of keys) {
    if (typeof object[i] === "object") {
      if (Array.isArray(object[i])) {
        const datas: any[] = getJsonForMetadata(object[i], "array");
        data.push({
          key: i,
          value: datas,
          type: "array",
          typeLabel: getLabelForTypeOfObject(object[i])
        });
      } else {
        const datas: any[] = getJsonForMetadata(object[i]);
        data.push({
          key: type && type === "array" ? "" : i,
          value: datas,
          type: "object",
          typeLabel: getLabelForTypeOfObject(object[i])
        });
      }
    } else {
      data.push({
        key: type && type === "array" ? "" : i,
        value: object[i],
        type: typeof object[i],
        typeLabel: getLabelForTypeOfObject(object[i])
      });
    }
  }
  return data;
};

const getJsonForObject = (object: any) => {
  const obj: any = {};
  switch (object.type) {
    case "array":
      let res: any[] = [];
      for (let i of object.value) {
        const data = getJson(i);
        res.push(data[i.key]);
      }
      obj[object.key] = res;
      break;
    case "object":
      let objs: any = {};
      for (let i of object.value) {
        const data = getJsonForObject(i);
        const key = Object.keys(data)[0];
        const value = data[key];
        objs[key] = value;
      }
      obj[object.key] = objs;
      break;
    default:
      obj[object.key] = object.value;
      break;
  }
  return obj;
};

const getJson = (objects: any[]) => {
  let options: any[];
  if (!Array.isArray(objects)) {
    options = [objects];
  } else {
    options = objects;
  }
  let object: any = {};
  for (let i of options) {
    const data = getJsonForObject(i);
    object = Object.assign(object, data);
  }
  return object;
};

const compareJsonObject = (object1: any, object2: any) => {
  if (JSON.stringify(object1) === JSON.stringify(object2)) {
    return true;
  }
  return false;
};

export {
  getSelectOptionList,
  compareObject,
  getType,
  removeForbiddenChars,
  dnsSubDomainRfc1123NameRegexp,
  messagingAddressNameRegexp,
  kFormatter,
  uniqueId,
  findIndexByProperty,
  hasOwnProperty,
  getCombinedString,
  getJsonForMetadata,
  getJson,
  compareJsonObject
};

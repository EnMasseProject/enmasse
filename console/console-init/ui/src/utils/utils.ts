/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import {
  MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN,
  TypeAheadMessage,
  SERVER_DATA_THRESHOLD,
  DeviceConnectionType
} from "constant";
import {
  forbiddenBackslashRegexp,
  forbiddenSingleQuoteRegexp,
  forbiddenDoubleQuoteRegexp
} from "types/Configs";
import { ICredential } from "modules/iot-device/components";

export interface ISelectOption {
  value: string;
  isDisabled?: boolean;
  key?: string;
  label?: string;
  id?: string;
}

/**
 * Create a object of type ISelectOption
 * @param value
 * @param isDisabled
 */
const createSelectOptionObject = (value: string, isDisabled: boolean) => {
  const data: ISelectOption = {
    key: `key-${value}`,
    id: `id-${value}`,
    value: value,
    isDisabled: isDisabled
  };
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
    const top_10_records = allRecords.splice(0, SERVER_DATA_THRESHOLD);
    if (top_10_records.length >= SERVER_DATA_THRESHOLD) {
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

const getAddressSpaceLabelForType = (type: string) => {
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
  targetPropertyValue: any
) => {
  if (items && targetProperty && targetPropertyValue) {
    return items.findIndex(
      item => item[targetProperty] === targetPropertyValue
    );
  }
  return -1;
};

const hasOwnProperty = (obj: Object, property: string) => {
  if (obj && property && property.trim() !== "") {
    return obj.hasOwnProperty(property);
  }
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

/**
 * Accepts a json object and convert it into array of objects with
 * key,value,type and typeLabel as key with values
 * @Examle
    input ={
      'asdf':"ASD"
    }
    output=
    [
      {
        key:'asdf',
        value:"ASD",
        type:"string",
        typeLabel:"String"
      }
    ]
 * @param object 
 * @param type 
 */
const convertJsonToMetadataOptions = (
  object: any,
  type?: string,
  isTypeLabel?: boolean
) => {
  const keys = Object.keys(object);
  let metadataArray = [];
  for (var key of keys) {
    if (typeof object[key] === "object") {
      if (Array.isArray(object[key])) {
        const datas: any[] = convertJsonToMetadataOptions(object[key], "array");
        metadataArray.push({
          key: key,
          value: datas,
          type: "array",
          typeLabel: getLabelForTypeOfObject(object[key])
        });
      } else {
        const datas: any[] = convertJsonToMetadataOptions(object[key]);
        metadataArray.push({
          key: type && type === "array" ? "" : key,
          value: datas,
          type: "object",
          typeLabel: getLabelForTypeOfObject(object[key])
        });
      }
    } else {
      metadataArray.push({
        id: uniqueId(),
        key: type && type === "array" ? "" : key,
        value: object[key],
        type: isTypeLabel
          ? getLabelForTypeOfObject(object[key])
          : typeof object[key],
        typeLabel: getLabelForTypeOfObject(object[key])
      });
    }
  }
  return metadataArray;
};

/** Accepts a object with key,value,type and typeLabel as convet into a exact json
 * with key and values from the form
 * @param object
 * Internal function used in convertMetadataOptionsToJson
 * */
const convertObjectIntoJson = (object: any) => {
  const obj: any = {};
  switch (object.type) {
    case "array":
      let res: any[] = [];
      for (let objectValue of object.value) {
        const data = convertMetadataOptionsToJson(objectValue);
        res.push(data[objectValue.key]);
      }
      obj[object.key] = res;
      break;
    case "object":
      let objs: any = {};
      for (let objectValue of object.value) {
        const data = convertObjectIntoJson(objectValue);
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

/**
 * Accepts a array of objects with key,value,type and typeLabel as key and values from the form
 * And converts it into actual Json
 * @Examle
    input =
    [
      {
        key:'asdf',
        value:"ASD",
        type:"string",
        typeLabel:"String"
      }
    ]
    output ={
      'asdf':"ASD"
    }
 * @param object 
 * @param type 
 */
const convertMetadataOptionsToJson = (objects: any[]) => {
  let options: any[];
  if (!Array.isArray(objects)) {
    options = [objects];
  } else {
    options = objects;
  }
  let object: any = {};
  for (let option of options) {
    const data = convertObjectIntoJson(option);
    object = Object.assign(object, data);
  }
  return object;
};

const createDeepCopy = (object: any) => {
  return JSON.parse(JSON.stringify(object));
};

const getFormattedJsonString = (json: any) => {
  return JSON.stringify(json, undefined, 2);
};

const getLabelByKey = (key: string) => {
  const keyLabels: any = {
    "auth-id": "Auth ID",
    type: "Credential type",
    "not-after": "Not after",
    "not-before": "Not before",
    "pwd-hash": "Password",
    "hashed-password": "Password",
    psk: "PSK",
    "x-509": "X-509 certificate",
    "subject-dn": "Subject-dn",
    "public-key": "Public key",
    "auto-provisioning-enabled": "Auto-provision",
    "x509-cert": "X-509 certificate"
  };

  if (key in keyLabels) {
    return keyLabels[key];
  }

  return key;
};

const getDeviceConnectionType = (
  viaGateway: boolean,
  credentials: ICredential[]
) => {
  let connectionType: string = "";
  if (viaGateway && !credentials?.length) {
    connectionType = DeviceConnectionType.VIA_GATEWAYS;
  } else if (!viaGateway && credentials?.length > 0) {
    connectionType = DeviceConnectionType.CONNECTED_DIRECTLY;
  } else {
    connectionType = DeviceConnectionType.NA;
  }
  return connectionType;
};

const deepClean = (obj: object, omitAttributes?: string[]) => {
  for (let propName in obj) {
    let propValue = (obj as any)[propName];
    if (
      propValue === null ||
      propValue === undefined ||
      propValue === "" ||
      omitAttributes?.includes(propName)
    ) {
      delete (obj as any)[propName];
    } else if (
      Object.prototype.toString.call(propValue) === "[object Object]"
    ) {
      deepClean(propValue);
    } else if (Array.isArray(propValue)) {
      for (let propName in obj) {
        let propValue = (obj as any)[propName];
        deepClean(propValue);
      }
    }
  }
};

export {
  getSelectOptionList,
  compareObject,
  getAddressSpaceLabelForType,
  removeForbiddenChars,
  dnsSubDomainRfc1123NameRegexp,
  messagingAddressNameRegexp,
  kFormatter,
  uniqueId,
  findIndexByProperty,
  hasOwnProperty,
  convertJsonToMetadataOptions,
  convertMetadataOptionsToJson,
  createDeepCopy,
  getFormattedJsonString,
  getLabelByKey,
  getDeviceConnectionType,
  deepClean
};

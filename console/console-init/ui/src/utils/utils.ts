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
  isDisabled: boolean;
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

export {
  getSelectOptionList,
  compareObject,
  getType,
  removeForbiddenChars,
  dnsSubDomainRfc1123NameRegexp,
  messagingAddressNameRegexp
};

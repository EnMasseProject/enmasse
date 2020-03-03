import {
  MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN,
  TypeAheadMessage,
  NUMBER_OF_RECORDS_TO_DISPLAY_IF_SERVER_HAS_MORE_DATA
} from "constants/constants";

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
    records.push({
      value: TypeAheadMessage.MORE_CHAR_REQUIRED,
      isDisabled: true
    });
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

export { getSelectOptionList };

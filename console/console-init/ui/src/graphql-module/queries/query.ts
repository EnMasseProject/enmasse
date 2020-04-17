/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { removeForbiddenChars } from "utils";
import { IFilterValue } from "modules/address";

const generateFilterPattern = (filterPattern: string, filterItems?: any[]) => {
  let filter = "";
  let filterItemsLength = filterItems && filterItems.length;
  let filterItem = filterItems && filterItems[0];
  let filterItemValue =
    filterItem &&
    filterItem.value &&
    removeForbiddenChars(filterItem.value.trim());

  if (
    filterItemValue &&
    filterItemsLength &&
    filterItemsLength > 0 &&
    filterPattern
  ) {
    if (filterItemsLength > 1) {
      filter += "(";
    }
    if (filterItem) {
      if (filterItem.isExact) {
        filter += "`$." + [filterPattern] + "` = '" + filterItemValue + "'";
      } else {
        filter += "`$." + [filterPattern] + "` LIKE '" + filterItemValue + "%'";
      }
    }
    if (filterItemsLength > 1) {
      for (let i = 1; i < filterItemsLength; i++) {
        let filterItem = filterItems && filterItems[i];
        let filterItemValue =
          filterItem &&
          filterItem.value &&
          removeForbiddenChars(filterItem.value.trim());
        if (filterItem) {
          if (filterItem.isExact) {
            filter +=
              "OR `$." + [filterPattern] + "` = '" + filterItemValue + "'";
          } else {
            filter +=
              "OR `$." + [filterPattern] + "` LIKE '" + filterItemValue + "%'";
          }
        }
      }
      filter += ")";
    }
  }
  return filter;
};

export { generateFilterPattern };

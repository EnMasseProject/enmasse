/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const getDetailForDeleteDialog = (selectedItems: any[]) => {
  const detail =
    selectedItems.length > 1
      ? `Are you sure you want to delete all of these address spaces: ${selectedItems.map(
          as => " " + as.name
        )} ?`
      : `Are you sure you want to delete this address space: ${selectedItems[0].name} ?`;
  return detail;
};

const getHeaderForDeleteDialog = (selectedItems: any[]) => {
  const header =
    selectedItems.length > 1
      ? "Delete these Address Spaces ?"
      : "Delete this Address Space ?";
  return header;
};

export { getDetailForDeleteDialog, getHeaderForDeleteDialog };

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  DataToolbarContent,
  DataToolbar,
  DataToolbarItem
} from "@patternfly/react-core/dist/js/experimental";
import {
  AddressSpaceFilterContainer,
  AddressSpaceListKebab
} from "modules/address-space";
import { ISortBy } from "@patternfly/react-table";
import { SortForMobileView } from "components/common/SortForMobileView";
import useWindowDimensions from "components/common/WindowDimension";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";

interface IAddressSpaceToolbarProps {
  filterNames: any[];
  setFilterNames: (value: Array<any>) => void;
  filterNamespaces: any[];
  setFilterNamespaces: (value: Array<any>) => void;
  filterType?: string | null;
  setFilterType: (value: string | null) => void;
  totalAddressSpaces: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onDeleteAll: () => void;
  isDeleteAllDisabled: boolean;
}
export const AddressSpaceToolbar: React.FunctionComponent<IAddressSpaceToolbarProps> = ({
  filterNames,
  setFilterNames,
  filterNamespaces,
  setFilterNamespaces,
  filterType,
  setFilterType,
  totalAddressSpaces,
  sortValue,
  setSortValue,
  onDeleteAll,
  isDeleteAllDisabled
}) => {
  const { width } = useWindowDimensions();
  const { dispatch } = useStoreContext();

  const [filterValue, setFilterValue] = useState<string>("Name");

  const onClearAllFilters = () => {
    setFilterValue("Name");
    setFilterNamespaces([]);
    setFilterNames([]);
    setFilterType(null);
  };

  const onCreateAddressSpace = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CREATE_ADDRESS_SPACE
    });
  };

  const onSelectDeleteAll = async (event: any) => {
    if (event.target.value === "deleteAll") {
      await onDeleteAll();
    }
  };

  const sortMenuItems = [
    { key: "name", value: "Name", index: 1 },
    { key: "creationTimestamp", value: "Time Created", index: 4 }
  ];
  const toolbarItems = (
    <>
      <AddressSpaceFilterContainer
        filterValue={filterValue}
        setFilterValue={setFilterValue}
        filterNames={filterNames}
        setFilterNames={setFilterNames}
        filterNamespaces={filterNamespaces}
        setFilterNamespaces={setFilterNamespaces}
        filterType={filterType}
        setFilterType={setFilterType}
        totalAddressSpaces={totalAddressSpaces}
      />
      {width < 769 && (
        <SortForMobileView
          sortMenu={sortMenuItems}
          sortValue={sortValue}
          setSortValue={setSortValue}
        />
      )}
      <DataToolbarItem>
        <AddressSpaceListKebab
          onCreateAddressSpace={onCreateAddressSpace}
          onSelectDeleteAll={onSelectDeleteAll}
          isDeleteAllDisabled={isDeleteAllDisabled}
        />
      </DataToolbarItem>
    </>
  );
  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}
    >
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};

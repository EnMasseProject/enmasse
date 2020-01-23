/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  DataToolbarContent,
  DataToolbar,
  DataToolbarItem
} from "@patternfly/react-core/dist/js/experimental";
import { CreateAddressSpace } from "../CreateAddressSpace/CreateAddressSpacePage";
import {
  AddressSpaceListFilter,
  AddressSpaceListKebab
} from "src/Components/AddressSpaceList/AddressSpaceListFilter";
import { ISortBy, IRowData } from "@patternfly/react-table";
import { SortForMobileView } from "../../Components/Common/SortForMobileView";
import useWindowDimensions from "src/Components/Common/WindowDimension";

interface IAddressSpaceListFilterPageProps {
  filterValue?: string;
  setFilterValue: (value: string) => void;
  filterNames: any[];
  setFilterNames: (value: Array<any>) => void;
  filterNamespaces: any[];
  setFilterNamespaces: (value: Array<any>) => void;
  filterType?: string | null;
  setFilterType: (value: string | null) => void;
  totalAddressSpaces: number;
  setOnCreationRefetch?: (value: boolean) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  onDeleteAll:()=>void;
}
export const AddressSpaceListFilterPage: React.FunctionComponent<IAddressSpaceListFilterPageProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterNamespaces,
  setFilterNamespaces,
  filterType,
  setFilterType,
  totalAddressSpaces,
  setOnCreationRefetch,
  sortValue,
  setSortValue,
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  onDeleteAll
}) => {
  const { width } = useWindowDimensions();
  const onClearAllFilters = () => {
    setFilterValue("Name");
    setFilterNamespaces([]);
    setFilterNames([]);
    setFilterType(null);
  };
  const createAddressSpaceOnClick = async () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
  };
  const sortMenuItems = [{ key: "name", value: "Name", index: 1 }];
  const toolbarItems = (
    <>
      <AddressSpaceListFilter
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
        {isCreateWizardOpen && (
          <CreateAddressSpace
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
            setOnCreationRefetch={setOnCreationRefetch}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <AddressSpaceListKebab
          createAddressSpaceOnClick={createAddressSpaceOnClick}
          onDeleteAll={onDeleteAll}
        />
      </DataToolbarItem>
    </>
  );
  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}>
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};

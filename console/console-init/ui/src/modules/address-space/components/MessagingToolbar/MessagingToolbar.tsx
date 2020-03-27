import React from "react";
import { MessagingToolbarToggleGroup } from "../MessagingDatatoolbarToggleGroup";
import { SortForMobileView, useWindowDimensions } from "components";
import {
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  SelectOptionObject,
  DataToolbarChipGroup,
  DataToolbarChip,
  DataToolbarContentProps
} from "@patternfly/react-core";
import { AddressSpaceListKebab } from "modules/address-space/components";
import { ISortBy } from "@patternfly/react-table";
export interface IMessageToolbarProps extends DataToolbarContentProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  namespaceSelected?: string;
  namespaceInput?: string;
  nameOptions?: any[];
  namespaceOptions?: any[];
  typeIsExpanded: boolean;
  typeSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedNamespaces: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onNamespaceSelect: (e: any, selection: SelectOptionObject) => void;
  onNamespaceClear: () => void;
  onNamespaceFilter: (e: any) => any[];
  onTypeToggle: () => void;
  onTypeSelect: (e: any, selection: SelectOptionObject) => void;
  onDeleteAll: () => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
  onCreateAddressSpace: () => void;
  isDeleteAllDisabled: boolean;
  onSelectDeleteAll: (event: any) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
}
const MessagingToolbar: React.FunctionComponent<IMessageToolbarProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  namespaceSelected,
  namespaceInput,
  nameOptions,
  namespaceOptions,
  typeIsExpanded,
  typeSelected,
  selectedNames,
  selectedNamespaces,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onNamespaceSelect,
  onNamespaceClear,
  onNamespaceFilter,
  onTypeToggle,
  onTypeSelect,
  onDeleteAll,
  onSearch,
  onDelete,
  onCreateAddressSpace,
  isDeleteAllDisabled,
  onSelectDeleteAll,
  sortValue,
  setSortValue,
  onClearAllFilters
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "name", value: "Name", index: 1 },
    { key: "creationTimestamp", value: "Time Created", index: 4 }
  ];
  const toolbarItems = (
    <>
      <MessagingToolbarToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        namespaceSelected={namespaceSelected}
        namespaceInput={namespaceInput}
        nameOptions={nameOptions}
        namespaceOptions={namespaceOptions}
        typeIsExpanded={typeIsExpanded}
        typeSelected={typeSelected}
        selectedNames={selectedNames}
        selectedNamespaces={selectedNamespaces}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onNameFilter={onNameFilter}
        onNamespaceSelect={onNamespaceSelect}
        onNamespaceClear={onNamespaceClear}
        onNamespaceFilter={onNamespaceFilter}
        onTypeToggle={onTypeToggle}
        onTypeSelect={onTypeSelect}
        onDeleteAll={onDeleteAll}
        onSearch={onSearch}
        onDelete={onDelete}
      />
      <DataToolbarItem>
        {width < 769 && (
          <SortForMobileView
            sortMenu={sortMenuItems}
            sortValue={sortValue}
            setSortValue={setSortValue}
          />
        )}
      </DataToolbarItem>
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
export { MessagingToolbar };

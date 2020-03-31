import React from "react";
import {
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  SelectOptionObject,
  DataToolbarChipGroup,
  DataToolbarChip,
  DataToolbarContentProps
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { SortForMobileView, useWindowDimensions } from "components";
import { AddressLinksToggleGroup } from "../AddressLinksToggleGroup";

export interface IAddressLinksToolbarProps extends DataToolbarContentProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  containerSelected?: string;
  containerInput?: string;
  nameOptions?: any[];
  containerOptions?: any[];
  roleIsExpanded: boolean;
  roleSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedContainers: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onContainerSelect: (e: any, selection: SelectOptionObject) => void;
  onContainerClear: () => void;
  onContainerFilter: (e: any) => any[];
  onRoleToggle: () => void;
  onRoleSelect: (e: any, selection: SelectOptionObject) => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
}
const AddressLinksToolbar: React.FunctionComponent<IAddressLinksToolbarProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  containerSelected,
  containerInput,
  nameOptions,
  containerOptions,
  roleIsExpanded,
  roleSelected,
  selectedNames,
  selectedContainers,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onContainerSelect,
  onContainerClear,
  onContainerFilter,
  onRoleToggle,
  onRoleSelect,
  onSearch,
  onDelete,
  sortValue,
  setSortValue,
  onClearAllFilters
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "name", value: "Name", index: 2 },
    { key: "deliveryRate", value: "DeliveryRate", index: 3 },
    { key: "backlog", value: "Backlog", index: 4 }
  ];
  const toolbarItems = (
    <>
      <AddressLinksToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        containerSelected={containerSelected}
        containerInput={containerInput}
        nameOptions={nameOptions}
        containerOptions={containerOptions}
        roleIsExpanded={roleIsExpanded}
        roleSelected={roleSelected}
        selectedNames={selectedNames}
        selectedContainers={selectedContainers}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onNameFilter={onNameFilter}
        onContainerSelect={onContainerSelect}
        onContainerClear={onContainerClear}
        onContainerFilter={onContainerFilter}
        onRoleToggle={onRoleToggle}
        onRoleSelect={onRoleSelect}
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
export { AddressLinksToolbar };

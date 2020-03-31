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
import { ConnectionLinksToggleGroup } from "modules/connection-detail/components";
import { SortForMobileView, useWindowDimensions } from "components";

export interface IConnectionLinksToolbarProps extends DataToolbarContentProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  addressSelected?: string;
  addressInput?: string;
  nameOptions?: any[];
  addressOptions?: any[];
  roleIsExpanded: boolean;
  roleSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedAddresses: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onAddressSelect: (e: any, selection: SelectOptionObject) => void;
  onAddressClear: () => void;
  onAddressFilter: (e: any) => any[];
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
const ConnectionLinksToolbar: React.FunctionComponent<IConnectionLinksToolbarProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  addressSelected,
  addressInput,
  nameOptions,
  addressOptions,
  roleIsExpanded,
  roleSelected,
  selectedNames,
  selectedAddresses,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onAddressSelect,
  onAddressClear,
  onAddressFilter,
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
    { key: "name", value: "Name", index: 1 },
    { key: "address", value: "Address", index: 2 },
    { key: "deliveries", value: "Deliveries", index: 3 },
    { key: "accepted", value: "Accepted", index: 4 },
    { key: "rejected", value: "Rejected", index: 5 },
    { key: "released", value: "Released", index: 6 },
    { key: "modified", value: "Modified", index: 7 },
    { key: "presettled", value: "Presettled", index: 8 },
    { key: "undelievered", value: "Undelievered", index: 9 }
  ];
  const toolbarItems = (
    <>
      <ConnectionLinksToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        addressSelected={addressSelected}
        addressInput={addressInput}
        nameOptions={nameOptions}
        addressOptions={addressOptions}
        roleIsExpanded={roleIsExpanded}
        roleSelected={roleSelected}
        selectedNames={selectedNames}
        selectedAddresses={selectedAddresses}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onNameFilter={onNameFilter}
        onAddressSelect={onAddressSelect}
        onAddressClear={onAddressClear}
        onAddressFilter={onAddressFilter}
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
export { ConnectionLinksToolbar };

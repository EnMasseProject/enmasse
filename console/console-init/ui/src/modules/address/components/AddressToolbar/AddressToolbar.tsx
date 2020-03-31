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
import { AddressToggleGroup } from "../AddressToggleGroup";
import { CreateAddress } from "modules/address/dialogs";
import { AddressListKebab } from "../AddressListKebab";

export interface IAddressToolbarProps extends DataToolbarContentProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  nameOptions?: any[];
  typeIsExpanded: boolean;
  typeSelected?: string | null;
  statusIsExpanded: boolean;
  statusSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onTypeToggle: () => void;
  onTypeSelect: (e: any, selection: SelectOptionObject) => void;
  onStatusToggle: () => void;
  onStatusSelect: (e: any, selection: SelectOptionObject) => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
  onClickCreateAddress: () => void;
  setOnCreationRefetch: (value: boolean) => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  namespace: string;
  addressspaceName: string;
  addressspaceType: string;
  addressspacePlan: string;
}
const AddressToolbar: React.FunctionComponent<IAddressToolbarProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  nameOptions,
  typeIsExpanded,
  typeSelected,
  statusIsExpanded,
  statusSelected,
  selectedNames,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onTypeToggle,
  onTypeSelect,
  onStatusToggle,
  onStatusSelect,
  onSearch,
  onDelete,
  sortValue,
  setSortValue,
  onClearAllFilters,
  onDeleteAllAddress,
  onPurgeAllAddress,
  isDeleteAllDisabled,
  isPurgeAllDisabled,
  onClickCreateAddress,
  setOnCreationRefetch,
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  namespace,
  addressspaceName,
  addressspaceType,
  addressspacePlan
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "name", value: "Address", index: 1 },
    { key: "creationTimestamp", value: "Time Created", index: 4 },
    { key: "messageIn", value: "Message In", index: 5 },
    { key: "messageOut", value: "Message Out", index: 6 },
    { key: "storedMessage", value: "Stored Messages", index: 7 },
    { key: "senders", value: "Senders", index: 8 },
    { key: "receivers", value: "Receivers", index: 9 }
  ];
  const toolbarItems = (
    <>
      <AddressToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        nameOptions={nameOptions}
        typeIsExpanded={typeIsExpanded}
        typeSelected={typeSelected}
        statusIsExpanded={statusIsExpanded}
        statusSelected={statusSelected}
        selectedNames={selectedNames}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onNameFilter={onNameFilter}
        onTypeToggle={onTypeToggle}
        onTypeSelect={onTypeSelect}
        onStatusToggle={onStatusToggle}
        onStatusSelect={onStatusSelect}
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
        {isCreateWizardOpen && (
          <CreateAddress
            name={addressspaceName || ""}
            namespace={namespace || ""}
            addressSpace={addressspaceName || ""}
            addressSpacePlan={addressspacePlan || ""}
            addressSpaceType={addressspaceType || ""}
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
            setOnCreationRefetch={setOnCreationRefetch}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <AddressListKebab
          createAddressOnClick={onClickCreateAddress}
          onDeleteAllAddress={onDeleteAllAddress}
          onPurgeAllAddress={onPurgeAllAddress}
          isDeleteAllDisabled={isDeleteAllDisabled}
          isPurgeAllDisabled={isPurgeAllDisabled}
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
export { AddressToolbar };

import React from "react";
import {
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbarChip
} from "@patternfly/react-core/dist/js/experimental";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import {
  Badge,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  Button,
  ButtonVariant,
  SelectOptionObject,
  SelectVariant,
  Select,
  SelectOption
} from "@patternfly/react-core";
import { TypeAheadSelect } from "components";
import { ISelectOption } from "utils";
import {
  TYPEAHEAD_REQUIRED_LENGTH,
  TypeAheadMessage
} from "constants/constants";

interface IAddressToolbarProps {
  totalAddresses: number;
  onFilterSelect: (e: any) => void;
  filterIsExpanded: boolean;
  setFilterIsExpanded: (value: boolean) => void;
  typeIsExpanded: boolean;
  setTypeIsExpanded: (value: boolean) => void;
  statusIsExpanded: boolean;
  setStatusIsExpanded: (value: boolean) => void;
  filterValue?: string | null;
  filterNames: Array<{ value: string; isExact: boolean }>;
  onDelete: (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => void;
  onClickSearchIcon: (event: any) => void;
  nameOptions?: Array<ISelectOption>;
  nameSelected?: string;
  setNameSelected: (value?: string) => void;
  nameInput: string;
  typeValue: string | null;
  statusValue: string | null;
  onTypeSelect: (event: any) => void;
  onStatusSelect: (event: any) => void;
  onNameSelectToggle: () => void;
  onNameSelect: (event: any, selection: string | SelectOptionObject) => void;
  isSelectNameExpanded: boolean;
  setIsSelectNameExpanded: (expanded: boolean) => void;
  onNameSelectFilterChange: (
    event: React.ChangeEvent<HTMLInputElement>
  ) => React.ReactElement[];
}
const AddressToolbarToggleGroup: React.FunctionComponent<IAddressToolbarProps> = ({
  totalAddresses,
  onFilterSelect,
  filterIsExpanded,
  setFilterIsExpanded,
  statusIsExpanded,
  setStatusIsExpanded,
  typeIsExpanded,
  setTypeIsExpanded,
  filterValue,
  filterNames,
  onDelete,
  onClickSearchIcon,
  nameOptions,
  nameSelected,
  setNameSelected,
  nameInput,
  typeValue,
  statusValue,
  onTypeSelect,
  onStatusSelect,
  onNameSelectToggle,
  onNameSelect,
  isSelectNameExpanded,
  setIsSelectNameExpanded,
  onNameSelectFilterChange
}) => {
  const filterMenuItems = [
    { key: "filterAddress", value: "Address" },
    { key: "filterType", value: "Type" },
    { key: "filterStatus", value: "Status" }
  ];

  const typeMenuItems = [
    { key: "typeQueue", value: "Queue" },
    { key: "typeTopic", value: "Topic" },
    { key: "typeSubscription", value: "Subscription" },
    { key: "typeMulitcast", value: "Mulitcast" },
    { key: "typeAnycast", value: "Anycast" }
  ];

  const statusMenuItems = [
    { key: "statusActive", value: "Active" },
    { key: "statusConfiguring", value: "Configuring" },
    { key: "statusFailed", value: "Failed" }
  ];

  const checkIsFilterApplied = () => {
    if (
      (filterNames && filterNames.length > 0) ||
      (typeValue && typeValue.trim() !== "") ||
      (statusValue && statusValue.trim() !== "")
    ) {
      return true;
    }
    return false;
  };

  return (
    <>
      <DataToolbarToggleGroup
        toggleIcon={
          <>
            <FilterIcon />
            {checkIsFilterApplied() && (
              <Badge key={1} isRead>
                {totalAddresses}
              </Badge>
            )}
          </>
        }
        breakpoint="xl"
      >
        <DataToolbarGroup variant="filter-group">
          <DataToolbarFilter categoryName="Filter">
            <Dropdown
              id="al-filter-dropdown"
              position="left"
              onSelect={onFilterSelect}
              isOpen={filterIsExpanded}
              toggle={
                <DropdownToggle onToggle={setFilterIsExpanded}>
                  <FilterIcon />
                  &nbsp;
                  {filterValue && filterValue.trim() !== ""
                    ? filterValue
                    : "Filter"}
                </DropdownToggle>
              }
              dropdownItems={filterMenuItems.map(option => (
                <DropdownItem
                  id={`al-filter-dropdown${option.key}`}
                  key={option.key}
                  value={option.value}
                  itemID={option.key}
                  component={"button"}
                >
                  {option.value}
                </DropdownItem>
              ))}
            />
          </DataToolbarFilter>
          {filterValue && filterValue.trim() !== "" && (
            <>
              <DataToolbarItem>
                <DataToolbarFilter
                  chips={filterNames.map(filter => filter.value)}
                  deleteChip={onDelete}
                  categoryName="Address"
                >
                  {filterValue && filterValue === "Address" && (
                    <InputGroup>
                      <Select
                        id="al-filter-input-name"
                        variant={SelectVariant.typeahead}
                        aria-label="Select a Name"
                        onToggle={onNameSelectToggle}
                        onSelect={onNameSelect}
                        onClear={() => {
                          setNameSelected(undefined);
                          setIsSelectNameExpanded(false);
                        }}
                        maxHeight="200px"
                        selections={nameSelected}
                        onFilter={onNameSelectFilterChange}
                        isExpanded={isSelectNameExpanded}
                        ariaLabelledBy={"typeahead-select-id"}
                        placeholderText="Select name"
                        isDisabled={false}
                        isCreatable={false}
                      >
                        {nameOptions && nameOptions.length > 0 ? (
                          nameOptions.map((option: any, index: number) => (
                            <SelectOption
                              key={index}
                              value={option.value}
                              isDisabled={option.isDisabled}
                            />
                          ))
                        ) : nameInput.trim().length <
                          TYPEAHEAD_REQUIRED_LENGTH ? (
                          <SelectOption
                            key={"invalid-input-length"}
                            value={TypeAheadMessage.MORE_CHAR_REQUIRED}
                            disabled={true}
                          />
                        ) : (
                          <SelectOption
                            key={"no-results-found"}
                            value={TypeAheadMessage.NO_RESULT_FOUND}
                            disabled={true}
                          />
                        )}
                        {/* {} */}
                      </Select>
                      <Button
                        id={"al-filter-select-name-search"}
                        variant={ButtonVariant.control}
                        aria-label="search button for search input"
                        onClick={onClickSearchIcon}
                      >
                        <SearchIcon />
                      </Button>
                    </InputGroup>
                  )}
                </DataToolbarFilter>
              </DataToolbarItem>
              <DataToolbarItem>
                <DataToolbarFilter
                  chips={typeValue ? [typeValue] : []}
                  deleteChip={onDelete}
                  categoryName="Type"
                >
                  {filterValue === "Type" && (
                    <Dropdown
                      id={"al-filter-select-type-dropdown"}
                      position="left"
                      onSelect={onTypeSelect}
                      isOpen={typeIsExpanded}
                      toggle={
                        <DropdownToggle onToggle={setTypeIsExpanded}>
                          {typeValue || "Select Type"}
                        </DropdownToggle>
                      }
                      dropdownItems={typeMenuItems.map(option => (
                        <DropdownItem
                          id={`al-filter-select-type-dropdown-item${option.key}`}
                          key={option.key}
                          value={option.value}
                          itemID={option.key}
                          component={"button"}
                        >
                          {option.value}
                        </DropdownItem>
                      ))}
                    />
                  )}
                </DataToolbarFilter>
              </DataToolbarItem>
              <DataToolbarItem>
                <DataToolbarFilter
                  chips={statusValue ? [statusValue] : []}
                  deleteChip={onDelete}
                  categoryName="Status"
                >
                  {filterValue === "Status" && (
                    <Dropdown
                      id={"al-filter-select-status-dropdown"}
                      position="left"
                      onSelect={onStatusSelect}
                      isOpen={statusIsExpanded}
                      toggle={
                        <DropdownToggle onToggle={setStatusIsExpanded}>
                          &nbsp;{statusValue || "Select Status"}
                        </DropdownToggle>
                      }
                      dropdownItems={statusMenuItems.map(option => (
                        <DropdownItem
                          id={`al-filter-select-status-dropdown-item${option.key}`}
                          key={option.key}
                          value={option.value}
                          itemID={option.key}
                          component={"button"}
                        >
                          {option.value}
                        </DropdownItem>
                      ))}
                    />
                  )}
                </DataToolbarFilter>
              </DataToolbarItem>
            </>
          )}
        </DataToolbarGroup>
      </DataToolbarToggleGroup>
    </>
  );
};

export { AddressToolbarToggleGroup };

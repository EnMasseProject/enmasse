import React from "react";
import {
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbarChip,
  DataToolbarChipGroup
} from "@patternfly/react-core/dist/js/experimental";
import {
  InputGroup,
  Button,
  ButtonVariant,
  Badge,
  DropdownPosition
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { TypeAheadSelect, DropdownWithToggle } from "components";
import { ISelectOption } from "utils";

interface IAddressLinksToolbarToggleGroupProps {
  filterValue: string;
  filterNames: Array<{ value: string; isExact: boolean }>;
  filterContainers: Array<{ value: string; isExact: boolean }>;
  filterRole?: string;
  totalLinks: number;
  onAddInput: (event: any) => void;
  onFilterSelect: (value: string) => void;
  onRoleSelect: (value: string) => void;
  onChangeNameData: (value: string) => void;
  onChangeContainerData: (value: string) => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
  filterIsExpanded: boolean;
  setFilterIsExpanded: (value: boolean) => void;
  nameOptions?: Array<ISelectOption>;
  nameSelected?: string;
  setNameSelected: (value?: string) => void;
  nameInput: string;
  setNameInput: (value: string) => void;
  containerOptions?: Array<ISelectOption>;
  containerSelected?: string;
  setContainerSelected: (value?: string) => void;
  containerInput: string;
  setContainerInput: (value: string) => void;
  roleIsExpanded: boolean;
  setRoleIsExpanded: (value: boolean) => void;
}

const AddressLinksToolbarToggleGroup: React.FunctionComponent<IAddressLinksToolbarToggleGroupProps> = ({
  filterValue,
  filterNames,
  filterContainers,
  filterRole,
  totalLinks,
  onAddInput,
  onFilterSelect,
  onRoleSelect,
  onChangeNameData,
  onChangeContainerData,
  onDelete,
  nameOptions,
  nameSelected,
  setNameSelected,
  nameInput,
  setNameInput,
  containerOptions,
  containerSelected,
  setContainerSelected,
  containerInput,
  setContainerInput
}) => {
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterContainers", value: "Container" },
    { key: "filterRole", value: "Role" }
  ];

  const roleMenuItems = [
    { key: "roleSender", value: "Sender" },
    { key: "roleReceiver", value: "Receiver" }
  ];

  const checkIsFilterApplied = () => {
    if (
      (filterNames && filterNames.length > 0) ||
      (filterContainers && filterContainers.length > 0) ||
      (filterRole && filterRole.trim() !== "")
    ) {
      return true;
    }
    return false;
  };

  return (
    <DataToolbarToggleGroup
      toggleIcon={
        <>
          <FilterIcon />
          {checkIsFilterApplied() && (
            <Badge key={1} isRead>
              {totalLinks}
            </Badge>
          )}
        </>
      }
      breakpoint="xl"
    >
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter categoryName="Filter">
          <DropdownWithToggle
            id="ad-links-filter-dropdown"
            position={DropdownPosition.left}
            value={filterValue}
            onSelectItem={onFilterSelect}
            dropdownItemIdPrefix="ad-links-filter-dropdown-item"
            toggleIcon={
              <>
                <FilterIcon />
                &nbsp;
              </>
            }
          />
        </DataToolbarFilter>
        <>
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterNames.map(filter => filter.value)}
              deleteChip={onDelete}
              categoryName="Name"
            >
              {filterValue && filterValue === "Name" && (
                <InputGroup>
                  <TypeAheadSelect
                    id={"ad-links-filter-select-name"}
                    ariaLabel={"Select a Name"}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText={"Select name"}
                    options={nameOptions}
                    selected={nameSelected}
                    setSelected={setNameSelected}
                    inputData={nameInput}
                    setInputData={setNameInput}
                    onChangeInputData={onChangeNameData}
                  />
                  <Button
                    id="ad-links-filter-search-name"
                    variant={ButtonVariant.control}
                    aria-label="search button for search name"
                    onClick={onAddInput}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroup>
              )}
            </DataToolbarFilter>
          </DataToolbarItem>
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterContainers.map(filter => filter.value)}
              deleteChip={onDelete}
              categoryName="Container"
            >
              {filterValue && filterValue === "Container" && (
                <InputGroup>
                  <TypeAheadSelect
                    id={"ad-links-filter-select-container"}
                    ariaLabel={"Select a Container"}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText={"Select container"}
                    options={containerOptions}
                    selected={containerSelected}
                    setSelected={setContainerSelected}
                    inputData={containerInput}
                    setInputData={setContainerInput}
                    onChangeInputData={onChangeContainerData}
                  />
                  <Button
                    id="ad-links-filter-search-container"
                    variant={ButtonVariant.control}
                    aria-label="search button for search containers"
                    onClick={onAddInput}
                  >
                    <SearchIcon />
                  </Button>
                </InputGroup>
              )}
            </DataToolbarFilter>
          </DataToolbarItem>

          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterRole ? [filterRole] : []}
              deleteChip={onDelete}
              categoryName="Role"
            >
              {filterValue === "Role" && (
                <DropdownWithToggle
                  id="ad-links-filter-select-role"
                  position={DropdownPosition.left}
                  onSelectItem={onRoleSelect}
                  value={filterRole || "Select Role"}
                  dropdownItems={roleMenuItems}
                  dropdownItemIdPrefix="d-links-filter-select-option-role"
                />
              )}
            </DataToolbarFilter>
          </DataToolbarItem>
        </>
      </DataToolbarGroup>
    </DataToolbarToggleGroup>
  );
};

export { AddressLinksToolbarToggleGroup };

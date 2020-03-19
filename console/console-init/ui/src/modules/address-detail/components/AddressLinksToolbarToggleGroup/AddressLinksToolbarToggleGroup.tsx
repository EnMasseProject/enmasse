import React from "react";
import {
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbarChip
} from "@patternfly/react-core/dist/js/experimental";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  Button,
  ButtonVariant,
  Badge
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { TypeAheadSelect } from "components";
import { ISelectOption } from "utils";

interface IAddressLinksToolbarToggleGroupProps {
  filterValue: string;
  filterNames: Array<{ value: string; isExact: boolean }>;
  filterContainers: Array<{ value: string; isExact: boolean }>;
  filterRole?: string;
  totalLinks: number;
  onAddInput: (event: any) => void;
  onFilterSelect: (event: any) => void;
  onRoleSelect: (event: any) => void;
  onChangeNameData: (value: string) => void;
  onChangeContainerData: (value: string) => void;
  onDelete: (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
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
  filterIsExpanded,
  setFilterIsExpanded,
  nameOptions,
  nameSelected,
  setNameSelected,
  nameInput,
  setNameInput,
  containerOptions,
  containerSelected,
  setContainerSelected,
  containerInput,
  setContainerInput,
  roleIsExpanded,
  setRoleIsExpanded
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
          <Dropdown
            id="ad-links-filter-dropdown"
            position="left"
            onSelect={onFilterSelect}
            isOpen={filterIsExpanded}
            toggle={
              <DropdownToggle onToggle={setFilterIsExpanded}>
                <FilterIcon />
                &nbsp;
                {filterValue}
              </DropdownToggle>
            }
            dropdownItems={filterMenuItems.map(option => (
              <DropdownItem
                id={`ad-links-filter-dropdown-item${option.key}`}
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
                <Dropdown
                  id="ad-links-filter-select-role"
                  position="left"
                  onSelect={onRoleSelect}
                  isOpen={roleIsExpanded}
                  toggle={
                    <DropdownToggle onToggle={setRoleIsExpanded}>
                      {filterRole || "Select Role"}
                    </DropdownToggle>
                  }
                  dropdownItems={roleMenuItems.map(option => (
                    <DropdownItem
                      id={`ad-links-filter-select-option-role${option.key}`}
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
      </DataToolbarGroup>
    </DataToolbarToggleGroup>
  );
};

export { AddressLinksToolbarToggleGroup };

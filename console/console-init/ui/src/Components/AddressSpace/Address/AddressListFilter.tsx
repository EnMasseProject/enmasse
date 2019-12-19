import * as React from "react";
import {
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbarChip,
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl,
  DataToolbarToggleGroup
} from "@patternfly/react-core/dist/js/experimental";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  KebabToggle,
  Badge
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";

interface IAddressListFilterProps {
  filterValue: string | null;
  setFilterValue: (value: string | null) => void;
  filterNames: string[];
  setFilterNames: (value: Array<string>) => void;
  typeValue: string | null;
  setTypeValue: (value: string | null) => void;
  statusValue: string | null;
  setStatusValue: (value: string | null) => void;
  totalAddresses: number;
}

interface IAddressListKebabProps {
  createAddressOnClick: () => void;
}
export const AddressListFilter: React.FunctionComponent<IAddressListFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  typeValue,
  setTypeValue,
  statusValue,
  setStatusValue,
  totalAddresses
}) => {
  const [inputValue, setInputValue] = React.useState<string | null>(null);
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [typeIsExpanded, setTypeIsExpanded] = React.useState(false);
  const [statusIsExpanded, setStatusIsExpanded] = React.useState(false);
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
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

  const onInputChange = (newValue: string) => {
    setInputValue(newValue);
  };

  const onAddInput = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (inputValue && inputValue.trim() !== "")
        if (filterNames.indexOf(inputValue.trim()) < 0) {
          setFilterNames([...filterNames, inputValue.trim()]);
        }
    }
    setInputValue(null);
  };

  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };
  const onTypeSelect = (event: any) => {
    setTypeValue(event.target.value);
    setTypeIsExpanded(!typeIsExpanded);
  };

  const onStatusSelect = (event: any) => {
    setStatusValue(event.target.value);
    setStatusIsExpanded(!statusIsExpanded);
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    switch (type) {
      case "Name":
        let index;
        if (filterNames && id) {
          index = filterNames.indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Type":
        setTypeValue(null);
        break;
      case "Status":
        setStatusValue(null);
        break;
    }
  };
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

  const toggleGroupItems = (
    <>
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter categoryName="Filter">
          <Dropdown
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
                key={option.key}
                value={option.value}
                itemID={option.key}
                component={"button"}>
                {option.value}
              </DropdownItem>
            ))}
          />
        </DataToolbarFilter>
        {filterValue && filterValue.trim() !== "" ? (
          <>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterNames}
                deleteChip={onDelete}
                categoryName="Name">
                {filterValue && filterValue === "Name" && (
                  <InputGroup>
                    <TextInput
                      name="name"
                      id="name"
                      type="search"
                      aria-label="search input name"
                      placeholder="Filter By Name ..."
                      onChange={onInputChange}
                      value={inputValue || ""}
                    />
                    <Button
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
                      onClick={onAddInput}>
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
                categoryName="Type">
                {filterValue === "Type" && (
                  <Dropdown
                    position="left"
                    onSelect={onTypeSelect}
                    isOpen={typeIsExpanded}
                    toggle={
                      <DropdownToggle onToggle={setTypeIsExpanded}>
                        <FilterIcon />
                        &nbsp;{typeValue}
                      </DropdownToggle>
                    }
                    dropdownItems={typeMenuItems.map(option => (
                      <DropdownItem
                        key={option.key}
                        value={option.value}
                        itemID={option.key}
                        component={"button"}>
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
                categoryName="Status">
                {filterValue === "Status" && (
                  <Dropdown
                    position="left"
                    onSelect={onStatusSelect}
                    isOpen={statusIsExpanded}
                    toggle={
                      <DropdownToggle onToggle={setStatusIsExpanded}>
                        <FilterIcon />
                        &nbsp;{statusValue}
                      </DropdownToggle>
                    }
                    dropdownItems={statusMenuItems.map(option => (
                      <DropdownItem
                        key={option.key}
                        value={option.value}
                        itemID={option.key}
                        component={"button"}>
                        {option.value}
                      </DropdownItem>
                    ))}
                  />
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
          </>
        ) : (
          <DataToolbarItem>
            <DataToolbarFilter categoryName="">
              <InputGroup>
                <TextInput
                  name="search diabled"
                  id="search disabled"
                  type="search"
                  aria-label="search input diabled"
                  placeholder="Search By filter ..."
                  onChange={onInputChange}
                  isDisabled={true}
                  value={inputValue || ""}
                />
                <Button
                  variant={ButtonVariant.control}
                  aria-label="search button for search input"
                  isDisabled={true}>
                  <SearchIcon />
                </Button>
              </InputGroup>
            </DataToolbarFilter>
          </DataToolbarItem>
        )}
      </DataToolbarGroup>
    </>
  );

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
        breakpoint="xl">
        {toggleGroupItems}
      </DataToolbarToggleGroup>
    </>
  );
};

export const AddressListKebab: React.FunctionComponent<IAddressListKebabProps> = ({
  createAddressOnClick
}) => {
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);
  const dropdownItems = [
    <DropdownItem key="delete-all" onClick={() => console.log("deleted")}>
      Delete All
    </DropdownItem>
    // <OverflowMenuDropdownItem key="secondary" isShared={true}>
    //   Create Address
    // </OverflowMenuDropdownItem>,
    // <OverflowMenuDropdownItem key="delete-all">
    //   Delete All
    // </OverflowMenuDropdownItem>
  ];
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = (event: any) => {
    setIsKebabOpen(isKebabOpen);
  };
  return (
    <>
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent isPersistent>
          <OverflowMenuGroup groupType="button" isPersistent>
            {/* Remove is Persistent after fixing dropdown items for overflow menu */}
            <OverflowMenuItem isPersistent>
              <Button
                id="al-filter-overflow-button"
                variant={ButtonVariant.primary}
                onClick={createAddressOnClick}>
                Create Address
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <Dropdown
            id="al-filter-overflow-dropdown"
            onSelect={onKebabSelect}
            toggle={<KebabToggle onToggle={onKebabToggle} />}
            isOpen={isKebabOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};

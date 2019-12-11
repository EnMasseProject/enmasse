import * as React from "react";

import {
  DataToolbar,
  DataToolbarItem,
  DataToolbarContent,
  DataToolbarFilter,
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarChip,
  OverflowMenuDropdownItem,
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import {
  Button,
  ButtonVariant,
  InputGroup,
  Dropdown,
  DropdownItem,
  KebabToggle,
  TextInput,
  DropdownToggle
} from "@patternfly/react-core";
import { SearchIcon, FilterIcon } from "@patternfly/react-icons";
import { CreateAddress } from "../../CreateAddress/CreateAddressPage";
import { useParams } from "react-router";
interface AddressListFilterProps {
  inputValue: string | null;
  setInputValue: (value: string | null) => void;
  filterValue: string | null;
  setFilterValue: (value: string | null) => void;
  typeValue: string | null;
  setTypeValue: (value: string | null) => void;
  statusValue: string | null;
  setStatusValue: (value: string | null) => void;
}
export const AddressListFilterPage: React.FunctionComponent<AddressListFilterProps> = ({
  inputValue,
  setInputValue,
  filterValue,
  setFilterValue,
  typeValue,
  setTypeValue,
  statusValue,
  setStatusValue
}) => {
  const { name, namespace, type } = useParams();
  const onInputChange = (newValue: string) => {
    if (!filterValue || filterValue !== "Name") {
      setFilterValue("Name");
    }
    setInputValue(newValue);
  };
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [typeIsExpanded, setTypeIsExpanded] = React.useState(false);
  const [statusIsExpanded, setStatusIsExpanded] = React.useState(false);
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);
  const onFilterSelect = (event: any) => {
    console.log(event.target.value);
    setFilterValue(event.target.value);
    if (event.target.value === "Name") {
      setTypeValue("");
      setStatusValue("");
    } else if (event.target.value === "Type") {
      setInputValue("");
      setStatusValue("");
    } else {
      setInputValue("");
      setTypeValue("");
    }
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
      case "Filter":
        setFilterValue(null);
        setInputValue(null);
        setTypeValue(null);
        setStatusValue(null);
        break;
      case "Type":
        setTypeValue(null);
        break;
      case "Name":
        setInputValue(null);
        break;
      case "Status":
        setStatusValue(null);
        break;
    }
  };
  const onDeleteAll = () => {
    setFilterValue(null);
    setTypeValue(null);
    setStatusValue(null);
    setInputValue(null);
  };
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };
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

  const toggleGroupItems = (
    <React.Fragment>
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter
          chips={filterValue && filterValue.trim() !== "" ? [filterValue] : []}
          deleteChip={onDelete}
          categoryName="Filter"
        >
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
                component={"button"}
              >
                {option.value}
              </DropdownItem>
            ))}
          />
        </DataToolbarFilter>
        {!filterValue || filterValue === "Name" || filterValue.trim() === "" ? (
          <DataToolbarItem>
            <DataToolbarFilter
              chips={inputValue && inputValue.trim() !== "" ? [inputValue] : []}
              deleteChip={onDelete}
              categoryName="Name"
            >
              <InputGroup>
                <TextInput
                  name="textInput2"
                  id="textInput2"
                  type="search"
                  aria-label="search input example"
                  placeholder="Filter by Name ..."
                  onChange={onInputChange}
                  value={inputValue || ""}
                />
                <Button
                  variant={ButtonVariant.control}
                  aria-label="search button for search input"
                >
                  <SearchIcon />
                </Button>
              </InputGroup>
            </DataToolbarFilter>
          </DataToolbarItem>
        ) : filterValue === "Type" ? (
          <DataToolbarFilter
            chips={typeValue && typeValue.trim() !== "" ? [typeValue] : []}
            deleteChip={onDelete}
            categoryName="Type"
          >
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
                  component={"button"}
                >
                  {option.value}
                </DropdownItem>
              ))}
            />
          </DataToolbarFilter>
        ) : (
          <DataToolbarFilter
            chips={
              statusValue && statusValue.trim() !== "" ? [statusValue] : []
            }
            deleteChip={onDelete}
            categoryName="Status"
          >
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
                  component={"button"}
                >
                  {option.value}
                </DropdownItem>
              ))}
            />
          </DataToolbarFilter>
        )}
      </DataToolbarGroup>
    </React.Fragment>
  );

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

  const onKebabSelect = (event: any) => {
    setIsKebabOpen(isKebabOpen);
  };
  const toolbarItems = (
    <React.Fragment>
      <DataToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
        {toggleGroupItems}
      </DataToolbarToggleGroup>
      <DataToolbarItem>
        {isCreateWizardOpen && (
          <CreateAddress
            namespace={namespace || ""}
            addressSpace={name || ""}
            addressSpaceType={type || ""}
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <OverflowMenu breakpoint="lg">
          <OverflowMenuContent isPersistent>
            <OverflowMenuGroup groupType="button" isPersistent>
              {/* Remove is Persistent after fixing dropdown items for overflow menu */}
              <OverflowMenuItem isPersistent>
                <Button
                  id="al-filter-overflow-button"
                  variant={ButtonVariant.primary}
                  onClick={() => setIsCreateWizardOpen(!isCreateWizardOpen)}
                >
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
      </DataToolbarItem>
    </React.Fragment>
  );
  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onDeleteAll}
    >
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};

import * as React from "react";
import {
  DataToolbarContent,
  DataToolbar,
  DataToolbarToggleGroup,
  DataToolbarChip,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  KebabToggle
} from "@patternfly/react-core";
import { CreateAddressSpace } from "../CreateAddressSpace/CreateAddressSpacePage";

interface IAddressSpaceListFilterPageProps {
  filterValue?: string | null;
  setFilterValue: (value: string | null) => void;
  filterNames: string[];
  setFilterNames: (value: Array<string>) => void;
  filterNamespaces: string[];
  setFilterNamespaces: (value: Array<string>) => void;
  filterType?: string | null;
  setFilterType: (value: string | null) => void;
}
export const AddressSpaceListFilterPage: React.FunctionComponent<IAddressSpaceListFilterPageProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterNamespaces,
  setFilterNamespaces,
  filterType,
  setFilterType
}) => {
  const [inputValue, setInputValue] = React.useState<string | null>(null);
  const [filterIsExpanded, setFilterIsExpanded] = React.useState<boolean>(
    false
  );
  const [typeFilterIsExpanded, setTypeFilterIsExpanded] = React.useState<
    boolean
  >(false);
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);

  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterNamespace", value: "Namespace" },
    { key: "filterType", value: "Type" }
  ];
  const typeFilterMenuItems = [
    { key: "typeStandard", value: "Standard" },
    { key: "typeBrokered", value: "Brokered" }
  ];
  const onInputChange = (newValue: string) => {
    setInputValue(newValue);
  };
  const onAddInput = (event: any) => {
    if (filterValue) {
      if (inputValue && inputValue.trim() != "" && filterNames) {
        if (filterValue === "Name") {
          if (filterNames.indexOf(inputValue) < 0) {
            setFilterNames([...filterNames, inputValue]);
          }
          setInputValue(null);
        } else if (filterValue === "Namespace") {
          if (filterNamespaces.indexOf(inputValue) < 0) {
            setFilterNamespaces([...filterNamespaces, inputValue]);
          }
          setInputValue(null);
        }
      }
    }
  };
  const onClearAllFilters = () => {
    setFilterValue(null);
    setFilterNamespaces([]);
    setFilterNames([]);
    setFilterType(null);
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    let index;
    switch (type) {
      case "Name":
        if (filterNames && id) {
          index = filterNames.indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Namespace":
        if (filterNamespaces && id) {
          index = filterNamespaces.indexOf(id.toString());
          if (index >= 0) filterNamespaces.splice(index, 1);
          setFilterNamespaces([...filterNamespaces]);
        }
        setFilterNamespaces([...filterNamespaces]);
        break;
      case "Type":
        setFilterType(null);
        break;
    }
  };
  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };

  const onTypeFilterSelect = (event: any) => {
    setFilterType(event.target.value);
    setTypeFilterIsExpanded(!typeFilterIsExpanded);
  };

  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = (event: any) => {
    setIsKebabOpen(isKebabOpen);
  };
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
                chips={filterNamespaces}
                deleteChip={onDelete}
                categoryName="Namespace">
                {filterValue && filterValue === "Namespace" && (
                  <InputGroup>
                    <TextInput
                      name="namespace"
                      id="namespace"
                      type="search"
                      aria-label="search input namespace"
                      placeholder="Filter By Namespace ..."
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
                chips={filterType ? [filterType] : []}
                deleteChip={onDelete}
                categoryName="Type">
                {filterValue && filterValue === "Type" && (
                  <InputGroup>
                    <Dropdown
                      position="left"
                      onSelect={onTypeFilterSelect}
                      isOpen={typeFilterIsExpanded}
                      toggle={
                        <DropdownToggle onToggle={setTypeFilterIsExpanded}>
                          <FilterIcon />
                          &nbsp;
                          {filterType && filterType.trim() !== ""
                            ? filterType
                            : "Type"}
                        </DropdownToggle>
                      }
                      dropdownItems={typeFilterMenuItems.map(option => (
                        <DropdownItem
                          key={option.key}
                          value={option.value}
                          itemID={option.key}
                          component={"button"}>
                          {option.value}
                        </DropdownItem>
                      ))}
                    />
                  </InputGroup>
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
  const toolbarItems = (
    <>
      <DataToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
        {toggleGroupItems}
      </DataToolbarToggleGroup>
      <DataToolbarItem>
        {isCreateWizardOpen && (
          <CreateAddressSpace
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
                  variant={ButtonVariant.primary}
                  onClick={() => setIsCreateWizardOpen(!isCreateWizardOpen)}>
                  Create Address space
                </Button>
              </OverflowMenuItem>
            </OverflowMenuGroup>
          </OverflowMenuContent>
          <OverflowMenuControl hasAdditionalOptions>
            <Dropdown
              onSelect={onKebabSelect}
              toggle={<KebabToggle onToggle={onKebabToggle} />}
              isOpen={isKebabOpen}
              isPlain
              dropdownItems={dropdownItems}
            />
          </OverflowMenuControl>
        </OverflowMenu>
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

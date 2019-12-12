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
  DropdownToggle,
  Badge
} from "@patternfly/react-core";
import { SearchIcon, FilterIcon } from "@patternfly/react-icons";
import { CreateAddressPage } from "../../CreateAddress/CreateAddressPage";
import { useParams } from "react-router";
import { useApolloClient } from "@apollo/react-hooks";
import { RETURN_ADDRESS_SPACE_DETAIL } from "src/Queries/Queries";
import { IAddressSpace } from "src/Components/AddressSpaceList/AddressSpaceList";
import { IAddressSpacesResponse } from "src/Types/ResponseTypes";
interface AddressListFilterProps {
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
export const AddressListFilterPage: React.FunctionComponent<AddressListFilterProps> = ({
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
  const { name, namespace, type } = useParams();
  const [inputValue, setInputValue] = React.useState<string | null>(null);
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [typeIsExpanded, setTypeIsExpanded] = React.useState(false);
  const [statusIsExpanded, setStatusIsExpanded] = React.useState(false);
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);
  const [addressSpacePlan, setAddressSpacePlan] = React.useState();

  const client = useApolloClient();
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
      if (inputValue && inputValue.trim() != "")
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
  const onDeleteAll = () => {
    setFilterValue(null);
    setTypeValue(null);
    setStatusValue(null);
    setFilterNames([]);
  };
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = (event: any) => {
    setIsKebabOpen(isKebabOpen);
  };
  const createAddressOnClick = async () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
    if (name && namespace) {
      const addressSpace = await client.query<IAddressSpacesResponse>({
        query: RETURN_ADDRESS_SPACE_DETAIL(name, namespace)
      });
      console.log(addressSpace);
      if (
        addressSpace.data &&
        addressSpace.data.addressSpaces &&
        addressSpace.data.addressSpaces.AddressSpaces.length > 0
      ) {
        const plan =
          addressSpace.data.addressSpaces.AddressSpaces[0].Spec.Plan.ObjectMeta
            .Name;
        if (plan) {
          setAddressSpacePlan(plan);
        }
      }
    }
  };
  const toggleGroupItems = (
    <React.Fragment>
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

  const toolbarItems = (
    <React.Fragment>
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
      <DataToolbarItem>
        {isCreateWizardOpen && (
          <CreateAddressPage
            name={name || ""}
            namespace={namespace || ""}
            addressSpace={name || ""}
            addressSpacePlan={addressSpacePlan || ""}
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
      </DataToolbarItem>
    </React.Fragment>
  );
  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onDeleteAll}>
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};

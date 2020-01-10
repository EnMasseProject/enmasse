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
  Button,
  ButtonVariant,
  KebabToggle,
  Badge,
  SelectOption,
  Select,
  SelectVariant,
  SelectOptionObject
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
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [typeIsExpanded, setTypeIsExpanded] = React.useState(false);
  const [statusIsExpanded, setStatusIsExpanded] = React.useState(false);
  const [isSelectNameExpanded, setIsSelectNameExpanded] = React.useState<
    boolean
  >(false);
  const [nameSelected, setNameSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<string>>();
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

  const onClickSearchIcon = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (nameSelected && nameSelected.trim() !== "")
        if (filterNames.indexOf(nameSelected.trim()) < 0) {
          setFilterNames([...filterNames, nameSelected.trim()]);
          setNameSelected(undefined);
        }
    }
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

  const onNameSelectToggle = () => {
    setIsSelectNameExpanded(!isSelectNameExpanded);
  };

  const onChangeNameData = async (value: string) => {
    setNameOptions(undefined);
    if(value.trim().length<6) {
      setNameOptions([]);
      return;
    }
    // const response = await client.query<IConnectionLinksNameSearchResponse>({
    //   query: RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH(
    //     connectionName,
    //     addressSpaceName,
    //     namespace,
    //     value.trim()
    //   )
    // });
    // if (
    //   response &&
    //   response.data &&
    //   response.data.connections &&
    //   response.data.connections.Connections &&
    //   response.data.connections.Connections.length > 0 &&
    //   response.data.connections.Connections[0].Links &&
    //   response.data.connections.Connections[0].Links.Links &&
    //   response.data.connections.Connections[0].Links.Links.length > 0
    // ) {
    //   const obtainedList = response.data.connections.Connections[0].Links.Links.map(
    //     (link: any) => {
    //       console.log("Name", link);
    //       return link.ObjectMeta.Name;
    //     }
    //   );
    //   setNameOptions(obtainedList);
    // }
  };

  const onNameSelectFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChangeNameData(e.target.value);
    const options: React.ReactElement[] = nameOptions
      ? nameOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onNameSelect = (event: any, selection: string | SelectOptionObject) => {
    setNameSelected(selection.toString());
    setIsSelectNameExpanded(false);
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
        {filterValue && filterValue.trim() !== "" && (
          <>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterNames}
                deleteChip={onDelete}
                categoryName="Name">
                {filterValue && filterValue === "Name" && (
                  <InputGroup>
                    <Select
                      variant={SelectVariant.typeahead}
                      aria-label="Select a Name"
                      onToggle={onNameSelectToggle}
                      onSelect={onNameSelect}
                      onClear={() => {
                        setNameSelected(undefined);
                        setIsSelectNameExpanded(false);
                      }}
                      selections={nameSelected}
                      onFilter={onNameSelectFilterChange}
                      isExpanded={isSelectNameExpanded}
                      ariaLabelledBy={"typeahead-select-id"}
                      placeholderText="Select name"
                      isDisabled={false}
                      isCreatable={false}>
                      {nameOptions &&
                        nameOptions.map((option, index) => (
                          <SelectOption key={index} value={option} />
                        ))}
                      {/* {} */}
                    </Select>
                    <Button
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
                      onClick={onClickSearchIcon}>
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
                        &nbsp;{typeValue || "Select Type"}
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
                        &nbsp;{statusValue || "Select Status"}
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
    </DropdownItem>,
    <DropdownItem key="purge-all" onClick={() => console.log("purged")}>
      Purge All
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

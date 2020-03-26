import React, { useState } from "react";
import {
  Select,
  SelectVariant,
  SelectOptionObject,
  SelectOption,
  DataToolbarContent,
  DataToolbar,
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  InputGroup,
  Button,
  DataToolbarItem,
  ButtonVariant,
  DataToolbarChip,
  DataToolbarChipGroup
} from "@patternfly/react-core";
import { ISelectOption } from "utils";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";

const ASToolbar: React.FunctionComponent = () => {
  const [filterIsExpanded, setFilterIsExpanded] = useState<boolean>(false);
  const [filterSelected, setFilterSelected] = useState<string>("Name");
  const [nameSelectIsExpanded, setNameSelectIsExpanded] = useState<boolean>(
    false
  );
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [namespaceSelectIsExpanded, setNamespaceSelectIsExpanded] = useState<
    boolean
  >(false);
  const [namespaceSelected, setNamespaceSelected] = useState<string>();
  const [namespaceInput, setNamespaceInput] = useState<string>();
  const [nameOptions, setNameOptions] = useState<any[]>([]);
  const [namespaceOptions, setNamespaceOptions] = useState<any[]>([]);
  const [typeIsExpanded, setTypeIsExpanded] = useState<boolean>(false);
  const [typeSelected, setTypeSelected] = useState<string>();
  const [selectedNames, setSelectedNames] = useState<
    { value: string; isExact: boolean }[]
  >([]);
  const [selectedNamespaces, setSelectedNamespaces] = useState<
    { value: string; isExact: boolean }[]
  >([]);

  const filterOptions: ISelectOption[] = [
    { value: "Name", isDisabled: false },
    { value: "Namespace", isDisabled: false },
    { value: "Type", isDisabled: false }
  ];
  const typeOptions: ISelectOption[] = [
    { value: "Standard", isDisabled: false },
    { value: "Brokered", isDisabled: false }
  ];
  const onFilterToggle = () => {
    setFilterIsExpanded(!filterIsExpanded);
  };
  const onFilterSelect = (e: any, selection: SelectOptionObject) => {
    setFilterSelected(selection.toString());
    setFilterIsExpanded(false);
  };

  const onNameToggle = () => {
    setNameSelectIsExpanded(!nameSelectIsExpanded);
  };
  const onNameSelect = (e: any, selection: SelectOptionObject) => {
    setNameSelected(selection.toString());
    setNameSelectIsExpanded(false);
  };
  const onNameClear = () => {
    setNameSelected(undefined);
    setNameInput(undefined);
  };
  const onNameFilter = (e: any) => {};

  const onNamespaceToggle = () => {
    setNamespaceSelectIsExpanded(!namespaceSelectIsExpanded);
  };
  const onNamespaceSelect = (e: any, selection: SelectOptionObject) => {
    setNamespaceSelected(selection.toString());
    setNamespaceSelectIsExpanded(false);
  };
  const onNamespaceClear = () => {
    setNamespaceSelected(undefined);
    setNamespaceInput(undefined);
  };
  const onNamespaceFilter = (e: any) => {};

  const onTypeToggle = () => {
    setTypeIsExpanded(!typeIsExpanded);
  };
  const onTypeSelect = (e: any, selection: SelectOptionObject) => {
    setTypeSelected(selection.toString());
    setTypeIsExpanded(false);
  };

  const onDeleteAll = () => {
    setFilterSelected("Name");
    setTypeSelected(undefined);
    setSelectedNames([]);
    setSelectedNamespaces([]);
  };
  const onSearch = () => {
    console.log("Clicked");
  };
  const onDelete = (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => {
    console.log(chip, category);
  };
  return (
    <>
      <DataToolbar
        id="data-toolbar-with-filter"
        className="pf-m-toggle-group-container"
        collapseListedFiltersBreakpoint="xl"
        clearAllFilters={onDeleteAll}
      >
        <DataToolbarContent>
          <DataToolbarToggleGroup
            toggleIcon={
              <>
                <FilterIcon />
              </>
            }
            breakpoint="xl"
          >
            <DataToolbarGroup variant="filter-group">
              <DataToolbarFilter categoryName="Filter">
                <Select
                  variant={SelectVariant.single}
                  aria-label="Select Input"
                  onToggle={onFilterToggle}
                  onSelect={onFilterSelect}
                  selections={filterSelected}
                  isExpanded={filterIsExpanded}
                >
                  {filterOptions.map((option, index) => (
                    <SelectOption
                      isDisabled={option.isDisabled}
                      key={index}
                      value={option.value}
                    />
                  ))}
                </Select>
              </DataToolbarFilter>
              <>
                <DataToolbarItem>
                  <DataToolbarFilter
                    chips={selectedNames.map(filter => filter.value)}
                    deleteChip={onDelete}
                    categoryName="Name"
                  >
                    {filterSelected && filterSelected === "Name" && (
                      <InputGroup>
                        <Select
                          variant={SelectVariant.typeahead}
                          ariaLabelTypeAhead="Select a state"
                          onToggle={onNameToggle}
                          onSelect={onNameSelect}
                          onClear={onNameClear}
                          // onFilter={onNameFilter}
                          selections={nameSelected || nameInput}
                          isExpanded={nameSelectIsExpanded}
                          ariaLabelledBy={"fdsg"}
                          placeholderText="Select a state"
                        >
                          {nameOptions}
                        </Select>
                        <Button
                          id="ad-links-filter-search-name"
                          variant={ButtonVariant.control}
                          aria-label="search button for search name"
                          onClick={onSearch}
                        >
                          <SearchIcon />
                        </Button>
                      </InputGroup>
                    )}
                  </DataToolbarFilter>
                </DataToolbarItem>
                <DataToolbarItem>
                  <DataToolbarFilter
                    chips={selectedNamespaces.map(filter => filter.value)}
                    deleteChip={onDelete}
                    categoryName="Namespace"
                  >
                    {filterSelected && filterSelected === "Namespace" && (
                      <InputGroup>
                        <Select
                          variant={SelectVariant.typeahead}
                          ariaLabelTypeAhead="Select a state"
                          onToggle={onNamespaceToggle}
                          onSelect={onNamespaceSelect}
                          onClear={onNamespaceClear}
                          // onFilter={onNamespaceFilter}
                          selections={namespaceSelected || namespaceInput}
                          isExpanded={namespaceSelectIsExpanded}
                          ariaLabelledBy={"fdsg"}
                          placeholderText="Select a state"
                        >
                          {namespaceOptions}
                        </Select>
                        <Button
                          id="ad-links-filter-search-name"
                          variant={ButtonVariant.control}
                          aria-label="search button for search name"
                          onClick={onSearch}
                        >
                          <SearchIcon />
                        </Button>
                      </InputGroup>
                    )}
                  </DataToolbarFilter>
                </DataToolbarItem>
                <DataToolbarItem>
                  <DataToolbarFilter
                    chips={typeSelected ? [typeSelected] : []}
                    deleteChip={onDelete}
                    categoryName="Type"
                  >
                    {filterSelected === "Type" && (
                      <Select
                        variant={SelectVariant.single}
                        aria-label="Select Type"
                        onToggle={onTypeToggle}
                        onSelect={onTypeSelect}
                        selections={typeSelected || "Select Type"}
                        isExpanded={typeIsExpanded}
                      >
                        {typeOptions.map((option, index) => (
                          <SelectOption
                            isDisabled={option.isDisabled}
                            key={index}
                            value={option.value}
                          />
                        ))}
                      </Select>
                    )}
                  </DataToolbarFilter>
                </DataToolbarItem>
              </>
            </DataToolbarGroup>
          </DataToolbarToggleGroup>
        </DataToolbarContent>
      </DataToolbar>
    </>
  );
};
export { ASToolbar };

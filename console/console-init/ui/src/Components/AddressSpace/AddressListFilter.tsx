import * as React from "react";
import { FilterDropdown, IDropdownOption } from "../Common/FilterDropdown";
import {
  InputGroup,
  Button,
  TextInput,
  ButtonVariant
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";

export interface IAddressListFilterProps {
  onSearch: () => void;
  onFilterSelect: (item: any) => void;
  filterValue: string;
  onTypeSelect: (item: any) => void;
  typeValue: string;
  onStatusSelect: (item: any) => void;
  statusValue: string;
}
export const filterOptions: IDropdownOption[] = [
    { value: "name", label: "Name" },
    { value: "type", label: "Type" },
    { value: "status", label: "Status" }
  ],
  typeOptions: IDropdownOption[] = [
    { value: "queue", label: "Queue" },
    { value: "topic", label: "Topic" },
    { value: "subscripition", label: "Subscripition" },
    { value: "mulitcast", label: "Mulitcast" },
    { value: "anycast", label: "Anycast" }
  ],
  statusOptions: IDropdownOption[] = [
    { value: "active", label: "Active" },
    { value: "configuring", label: "Configuring" },
    { value: "failed", label: "Failed" }
  ];

export const AddressListFilter: React.FunctionComponent<
  IAddressListFilterProps
> = ({
  onSearch,
  onFilterSelect,
  filterValue,
  onTypeSelect,
  typeValue,
  onStatusSelect,
  statusValue
}) => {
  return (
    <InputGroup>
      <FilterDropdown
        value={filterValue}
        onSelect={onFilterSelect}
        options={filterOptions}
      />
      {filterValue === "Name" && (
        <InputGroup>
          <TextInput
            name="search name"
            id="searchName"
            type="search"
            placeholder="Filter by name..."
            aria-label="search input name"
            style={{ marginLeft: 2 }}
          />
          <Button
            variant={ButtonVariant.control}
            aria-label="search button for search input"
            onClick={onSearch}>
            <SearchIcon />
          </Button>
        </InputGroup>
      )}
      {filterValue === "Type" && (
        <FilterDropdown
          value={typeValue}
          onSelect={onTypeSelect}
          options={typeOptions}
        />
      )}
      {filterValue === "Status" && (
        <FilterDropdown
          value={statusValue}
          onSelect={onStatusSelect}
          options={statusOptions}
        />
      )}
    </InputGroup>
  );
};

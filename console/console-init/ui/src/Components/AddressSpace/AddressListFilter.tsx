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
  setFilterValue: (item: any) => void;
  filterValue: string;
  setTypeValue: (item: any) => void;
  typeValue: string;
  setStatusValue: (item: any) => void;
  statusValue: string;
}
export const filterOptions: IDropdownOption[] = [
    { value: "Name", label: "Name" },
    { value: "Type", label: "Type" },
    { value: "Status", label: "Status" }
  ],
  typeOptions: IDropdownOption[] = [
    { value: "Queue", label: "Queue" },
    { value: "Topic", label: "Topic" },
    { value: "Subscripition", label: "Subscripition" },
    { value: "Mulitcast", label: "Mulitcast" },
    { value: "Anycast", label: "Anycast" }
  ],
  statusOptions: IDropdownOption[] = [
    { value: "Active", label: "Active" },
    { value: "Configuring", label: "Configuring" },
    { value: "Failed", label: "Failed" }
  ];

export const AddressListFilter: React.FunctionComponent<IAddressListFilterProps> = ({
  onSearch,
  setFilterValue,
  filterValue,
  setTypeValue,
  typeValue,
  setStatusValue,
  statusValue
}) => {
  return (
    <InputGroup>
      <FilterDropdown
        value={filterValue}
        setValue={setFilterValue}
        options={filterOptions}
      />
      {filterValue === "Name" ? (
        <InputGroup>
          <TextInput
            name="search name"
            id="searchName"
            type="search"
            placeholder="Filter by name..."
            aria-label="search input name"
          />
          <Button
            variant={ButtonVariant.control}
            aria-label="search button for search input"
            onClick={onSearch}
            style={{ marginRight: 10 }}
          >
            <SearchIcon />
          </Button>
        </InputGroup>
      ) : filterValue === "Type" ? (
        <FilterDropdown
          value={typeValue}
          setValue={setTypeValue}
          options={typeOptions}
        />
      ) : filterValue === "Status" ? (
        <FilterDropdown
          value={statusValue}
          setValue={setStatusValue}
          options={statusOptions}
        />
      ) : (
        ""
      )}
    </InputGroup>
  );
};

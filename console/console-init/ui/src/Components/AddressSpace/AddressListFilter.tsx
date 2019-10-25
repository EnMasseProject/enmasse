import * as React from "react";
import {
  DropdownMenu,
  IDropdownOption
} from "../Common/DropdownComponent";
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
  isTypeDropdownOpen: boolean;
  onTypeSelect: (item: any) => void;
  typeValue: string;
  setTypeOpen: () => void;
  isStatusDropdownOpen: boolean;
  onStatusSelect: (item: any) => void;
  statusValue: string;
  setStatusOpen: () => void;
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
  isTypeDropdownOpen,
  onTypeSelect,
  typeValue,
  setTypeOpen,
  isStatusDropdownOpen,
  onStatusSelect,
  statusValue,
  setStatusOpen
}) => {
  const [isFilterDropdownOpen, setIsFilterDropdwonOpen] = React.useState(false);
  return (
    <InputGroup>
      <DropdownMenu
        isOpen={isFilterDropdownOpen}
        value={filterValue}
        onSelect={onFilterSelect && setIsFilterDropdwonOpen}
        setIsOpen={() => setIsFilterDropdwonOpen(!isFilterDropdownOpen)}
        options={filterOptions}
      />
      {filterValue==="Name" && 
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
          onClick={onSearch}>
          <SearchIcon />
        </Button>
        </InputGroup>
      }
      {filterValue === "Type" && 
        <DropdownMenu
          isOpen={isTypeDropdownOpen}
          value={typeValue}
          onSelect={onTypeSelect}
          setIsOpen={setTypeOpen}
          options={typeOptions}
        />
      }
      {filterValue === "Status" && (
        <DropdownMenu
          isOpen={isStatusDropdownOpen}
          value={statusValue}
          onSelect={onStatusSelect}
          setIsOpen={setStatusOpen}
          options={statusOptions}
        />
      )}
    </InputGroup>
  );
};

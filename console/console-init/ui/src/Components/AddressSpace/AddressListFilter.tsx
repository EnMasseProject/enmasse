import * as React from "react";
import DropdownComponent, {
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
  isFilterDropdownOpen: boolean;
  onFilterSelect: (item: any) => void;
  filterValue: string;
  setFilterOpen: () => void;
  isTypeDropdownOpen: boolean;
  onTypeSelect: (item: any) => void;
  typeValue: string;
  setTypeOpen: () => void;
  isStatusDropdownOpen: boolean
  onStatusSelect: (item: any) => void;
  statusValue: string;
  setStatusOpen: () => void;
}
const filterOptions: IDropdownOption[] = [
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

const AddressListFilter: React.FunctionComponent<IAddressListFilterProps> = ({
  onSearch,
  isFilterDropdownOpen,
  onFilterSelect,
  filterValue,
  setFilterOpen,
  isTypeDropdownOpen,
  onTypeSelect,
  typeValue,
  setTypeOpen,
  isStatusDropdownOpen,
  onStatusSelect,
  statusValue,
  setStatusOpen
}) => {
  const DataToRender = () => {
    switch (filterValue) {
      case "Name":
        return (
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
        );
      case "Type":
        return (
          <DropdownComponent
            isOpen={isTypeDropdownOpen}
            value={typeValue}
            onSelect={onTypeSelect}
            setIsOpen={setTypeOpen}
            options={typeOptions}
          />
        );
      case "Status":
        return (
          <DropdownComponent
            isOpen={isStatusDropdownOpen}
            value={statusValue}
            onSelect={onStatusSelect}
            setIsOpen={setStatusOpen}
            options={statusOptions}
          />
        );
      default:
        return "";
    }
  };
  return (
    <InputGroup>
      <DropdownComponent
        isOpen={isFilterDropdownOpen}
        value={filterValue}
        onSelect={onFilterSelect}
        setIsOpen={setFilterOpen}
        options={filterOptions}
      />
      {DataToRender()}
    </InputGroup>
  );
};

export default AddressListFilter;

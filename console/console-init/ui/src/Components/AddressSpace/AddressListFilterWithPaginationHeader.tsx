import * as React from "react";
import {
  Grid,
  GridItem,
  InputGroup,
  Dropdown,
  DropdownToggle,
  TextInput,
  Button,
  ButtonVariant,
  DropdownPosition,
  KebabToggle,
  Pagination,
  PageSection,
  PageSectionVariants
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { AddressListFilter } from "./AddressListFilter";

export const AddressListFilterWithPagination: React.FunctionComponent<
  any
> = () => {
  const [filter, setFilter] = React.useState("Name");
  const onFilterSelect = (item: any) => {
    console.log(item);
  };
  return (
      <InputGroup>
        <AddressListFilter
          onSearch={() => {
            console.log("on Search");
          }}
          onFilterSelect={onFilterSelect}
          filterValue={filter}
          onTypeSelect={() => {}}
          typeValue={"Small"}
          onStatusSelect={() => {}}
          statusValue={"Active"}
        />
        <Button variant="primary">Create address</Button>
        <Dropdown
          isPlain
          position={DropdownPosition.right}
          isOpen={false}
          onSelect={() => {}}
          toggle={<KebabToggle onToggle={() => {}} />}
          dropdownItems={[]}
        />
      </InputGroup>
  );
};

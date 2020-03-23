import React from "react";
import { DataToolbarChip } from "@patternfly/react-core/dist/js/experimental";
import { useApolloClient } from "@apollo/react-hooks";
import { ISelectOption, getSelectOptionList } from "utils";
import { FetchPolicy, TYPEAHEAD_REQUIRED_LENGTH } from "constants/constants";
import {
  RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH
} from "graphql-module";
import {
  ISearchAddressLinkContainerResponse,
  ISearchAddressLinkNameResponse
} from "types/ResponseTypes";
import { AddressLinksToolbarToggleGroup } from "modules/address-detail/components/AddressLinksToolbarToggleGroup";

interface IAddressLinksFilterProps {
  filterValue: string;
  setFilterValue: (value: string) => void;
  filterNames: Array<{ value: string; isExact: boolean }>;
  setFilterNames: (value: Array<{ value: string; isExact: boolean }>) => void;
  filterContainers: Array<{ value: string; isExact: boolean }>;
  setFilterContainers: (
    value: Array<{ value: string; isExact: boolean }>
  ) => void;
  filterRole?: string;
  setFilterRole: (role: string | undefined) => void;
  totalLinks: number;
  addressName: string;
  namespace: string;
}
const AddressLinksFilter: React.FunctionComponent<IAddressLinksFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterContainers,
  setFilterContainers,
  filterRole,
  setFilterRole,
  totalLinks,
  addressName,
  namespace
}) => {
  const client = useApolloClient();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState<boolean>(
    false
  );
  const [roleIsExpanded, setRoleIsExpanded] = React.useState<boolean>(false);
  const [nameSelected, setNameSelected] = React.useState<string>();
  const [containerSelected, setContainerSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<ISelectOption>>();
  const [containerOptions, setContainerOptions] = React.useState<
    Array<ISelectOption>
  >();
  const [nameInput, setNameInput] = React.useState<string>("");
  const [containerInput, setContainerInput] = React.useState<string>("");

  const onAddInput = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (nameSelected && nameSelected.trim() !== "")
        if (
          filterNames.map(filter => filter.value).indexOf(nameSelected.trim()) <
          0
        ) {
          setFilterNames([
            ...filterNames,
            { value: nameSelected.trim(), isExact: true }
          ]);
          setNameSelected(undefined);
        }
      if (!nameSelected && nameInput && nameInput.trim() !== "")
        if (
          filterNames.map(filter => filter.value).indexOf(nameInput.trim()) < 0
        )
          setFilterNames([
            ...filterNames,
            { value: nameInput.trim(), isExact: false }
          ]);
    } else if (filterValue && filterValue === "Container") {
      if (containerSelected && containerSelected.trim() !== "")
        if (
          filterContainers
            .map(filter => filter.value)
            .indexOf(containerSelected.trim()) < 0
        ) {
          setFilterContainers([
            ...filterContainers,
            { value: containerSelected.trim(), isExact: true }
          ]);
          setContainerSelected(undefined);
        }
      if (!containerSelected && containerInput && containerInput.trim() !== "")
        if (
          filterContainers
            .map(filter => filter.value)
            .indexOf(containerInput.trim()) < 0
        )
          setFilterContainers([
            ...filterContainers,
            { value: containerInput.trim(), isExact: false }
          ]);
    }
  };

  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };
  const onRoleSelect = (event: any) => {
    setFilterRole(event.target.value);
    setRoleIsExpanded(!roleIsExpanded);
  };
  const onChangeNameData = async (value: string) => {
    setNameOptions(undefined);
    if (value.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<ISearchAddressLinkNameResponse>({
      query: RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH(
        addressName,
        namespace,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.addresses &&
      response.data.addresses.addresses &&
      response.data.addresses.addresses.length > 0 &&
      response.data.addresses.addresses[0].links &&
      response.data.addresses.addresses[0].links.links &&
      response.data.addresses.addresses[0].links.links.length > 0
    ) {
      let obtainedList = [];
      obtainedList = response.data.addresses.addresses[0].links.links.map(
        (link: any) => {
          return link.metadata.name;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const uniqueList = getSelectOptionList(
        obtainedList,
        response.data.addresses.addresses[0].links.total
      );
      if (uniqueList.length > 0) setNameOptions(uniqueList);
    }
  };

  const onChangeContainerData = async (value: string) => {
    setContainerOptions(undefined);
    if (value.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setContainerOptions([]);
      return;
    }
    const response = await client.query<ISearchAddressLinkContainerResponse>({
      query: RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH(
        addressName,
        namespace,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.addresses &&
      response.data.addresses.addresses &&
      response.data.addresses.addresses.length > 0 &&
      response.data.addresses.addresses[0].links &&
      response.data.addresses.addresses[0].links.links &&
      response.data.addresses.addresses[0].links.links.length > 0
    ) {
      let obtainedList = [];
      obtainedList = response.data.addresses.addresses[0].links.links.map(
        (link: any) => {
          return link.spec.connection.spec.containerId;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const uniqueList = getSelectOptionList(
        obtainedList,
        response.data.addresses.addresses[0].links.total
      );
      if (uniqueList.length > 0) setContainerOptions(uniqueList);
    }
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    switch (type) {
      case "Name":
        if (filterNames && id) {
          let index = filterNames
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Container":
        if (filterContainers && id) {
          let index = filterContainers
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterContainers.splice(index, 1);
          setFilterContainers([...filterContainers]);
        }
        break;
      case "Role":
        setFilterRole(undefined);
        break;
    }
  };

  return (
    <AddressLinksToolbarToggleGroup
      filterValue={filterValue}
      filterNames={filterNames}
      filterContainers={filterContainers}
      filterRole={filterRole}
      totalLinks={totalLinks}
      onAddInput={onAddInput}
      onFilterSelect={onFilterSelect}
      onRoleSelect={onRoleSelect}
      onChangeNameData={onChangeNameData}
      onChangeContainerData={onChangeContainerData}
      onDelete={onDelete}
      filterIsExpanded={filterIsExpanded}
      setFilterIsExpanded={setFilterIsExpanded}
      nameOptions={nameOptions}
      nameSelected={nameSelected}
      setNameSelected={setNameSelected}
      nameInput={nameInput}
      setNameInput={setNameInput}
      containerOptions={containerOptions}
      containerSelected={containerSelected}
      setContainerSelected={setContainerSelected}
      containerInput={containerInput}
      setContainerInput={setContainerInput}
      roleIsExpanded={roleIsExpanded}
      setRoleIsExpanded={setRoleIsExpanded}
    />
  );
};

export { AddressLinksFilter };

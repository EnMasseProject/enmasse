import * as React from "react";

import {
  DataToolbar,
  DataToolbarItem,
  DataToolbarContent
} from "@patternfly/react-core/dist/js/experimental";
import { CreateAddressPage } from "../../CreateAddress/CreateAddressPage";
import { useParams } from "react-router";
import { useApolloClient } from "@apollo/react-hooks";
import { RETURN_ADDRESS_SPACE_DETAIL } from "src/Queries/Queries";
import { IAddressSpacesResponse } from "src/Types/ResponseTypes";
import {
  AddressListFilter,
  AddressListKebab
} from "src/Components/AddressSpace/Address/AddressListFilter";
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
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);
  const [addressSpacePlan, setAddressSpacePlan] = React.useState();

  const client = useApolloClient();

  const onDeleteAll = () => {
    setFilterValue("Name");
    setTypeValue(null);
    setStatusValue(null);
    setFilterNames([]);
  };

  const createAddressOnClick = async () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
    if (name && namespace) {
      const addressSpace = await client.query<IAddressSpacesResponse>({
        query: RETURN_ADDRESS_SPACE_DETAIL(name, namespace)
      });
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

  const toolbarItems = (
    <>
      <AddressListFilter
        filterValue={filterValue}
        setFilterValue={setFilterValue}
        filterNames={filterNames}
        setFilterNames={setFilterNames}
        typeValue={typeValue}
        setTypeValue={setTypeValue}
        statusValue={statusValue}
        setStatusValue={setStatusValue}
        totalAddresses={totalAddresses}
      />
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
        <AddressListKebab createAddressOnClick={createAddressOnClick} />
      </DataToolbarItem>
    </>
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

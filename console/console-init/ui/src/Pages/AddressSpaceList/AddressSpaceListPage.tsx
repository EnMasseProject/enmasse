import React from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import {
  Button,
  Modal
} from "@patternfly/react-core";
import {
  AddressSpaceList,
  IAddressSpace
} from "src/Components/AddressSpaceList/AddressSpaceList";
import { EmptyAddressSpace } from "src/Components/Common/EmptyAddressSpace";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import {
  DELETE_ADDRESS_SPACE,
  RETURN_ALL_ADDRESS_SPACES
} from "src/Queries/Queries";
import { IAddressSpacesResponse } from "src/Types/ResponseTypes";
import { EditAddressSpace } from "../EditAddressSpace";

interface AddressSpaceListPageProps {
  page: number;
  perPage: number;
  totalAddressSpaces: number;
  setTotalAddressSpaces: (value: number) => void;
  filter_Names: string[];
  filter_NameSpace: string[];
  filter_Type: string | null;
}
export const AddressSpaceListPage: React.FunctionComponent<AddressSpaceListPageProps> = ({
  page,
  perPage,
  totalAddressSpaces,
  setTotalAddressSpaces,
  filter_Names,
  filter_NameSpace,
  filter_Type
}) => {
  useDocumentTitle("Addressspace List");
  useA11yRouteChange();
  const client = useApolloClient();
  const [
    addressSpaceBeingEdited,
    setAddressSpaceBeingEdited
  ] = React.useState<IAddressSpace | null>();

  const [
    addressSpaceBeingDeleted,
    setAddressSpaceBeingDeleted
  ] = React.useState<IAddressSpace | null>();

  const { loading, error, data, refetch } = useQuery<IAddressSpacesResponse>(
    RETURN_ALL_ADDRESS_SPACES(
      page,
      perPage,
      filter_Names,
      filter_NameSpace,
      filter_Type
    ),
    { pollInterval: 20000 }
  );
  console.log(data);
  const handleCancelEdit = () => setAddressSpaceBeingEdited(null);
  const handleSaving = () => void 0;
  const handleEditChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingEdited(addressSpace);

  const handleCancelDelete = () => setAddressSpaceBeingDeleted(null);
  const handleDelete = async () => {
    if (addressSpaceBeingDeleted) {
      const deletedData = await client.mutate({
        mutation: DELETE_ADDRESS_SPACE,
        variables: {
          a: {
            Name: addressSpaceBeingDeleted.name,
            Namespace: addressSpaceBeingDeleted.nameSpace
          }
        }
      });
      if (
        deletedData &&
        deletedData.data &&
        deletedData.data.deleteAddressSpace === true
      ) {
        setAddressSpaceBeingDeleted(null);
        refetch();
      }
    }
  };
  const handleDeleteChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingDeleted(addressSpace);

  const handlePlanChange = (plan: string) => {
    if(addressSpaceBeingEdited){
      addressSpaceBeingEdited.displayName = plan;
      setAddressSpaceBeingEdited({...addressSpaceBeingEdited});
    }
  }

  if(error) {
    console.log(error);
  }
  console.log(data);

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };
  setTotalAddressSpaces(addressSpaces.Total);
  const addressSpacesList = addressSpaces.AddressSpaces.map(addSpace => ({
    name: addSpace.ObjectMeta.Name,
    nameSpace: addSpace.ObjectMeta.Namespace,
    creationTimestamp: addSpace.ObjectMeta.CreationTimestamp,
    type: addSpace.Spec.Type,
    displayName: addSpace.Spec.Plan.Spec.DisplayName,
    isReady: addSpace.Status.IsReady
  }));
  return (
    <>
      {totalAddressSpaces > 0 ? (
        <AddressSpaceList
          rows={addressSpacesList}
          onEdit={handleEditChange}
          onDelete={handleDeleteChange}
        />
      ) : (
        <EmptyAddressSpace />
      )}
      {addressSpaceBeingEdited && (
        <Modal
          isLarge
          id="as-list-edit-modal"
          title="Edit"
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button key="confirm" id="as-list-edit-confirm" variant="primary" onClick={handleSaving}>
              Confirm
            </Button>,
            <Button key="cancel" id="as-list-edit-cancel" variant="link" onClick={handleCancelEdit}>
              Cancel
            </Button>
          ]}
          isFooterLeftAligned={true}
        >
          <EditAddressSpace
            addressSpace={addressSpaceBeingEdited}
            onPlanChange={handlePlanChange}
          >
          </EditAddressSpace>
        </Modal>
      )}
      {addressSpaceBeingDeleted && (
        <DeletePrompt
          detail={`Are you sure you want to delete ${addressSpaceBeingDeleted.name} ?`}
          name={addressSpaceBeingDeleted.name}
          header="Delete this Address Space ?"
          handleCancelDelete={handleCancelDelete}
          handleConfirmDelete={handleDelete}
        />
      )}
    </>
  );
};

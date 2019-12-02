import React from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Button,
  Dropdown,
  KebabToggle,
  Modal,
  DropdownItem
} from "@patternfly/react-core";
import {
  AddressSpaceList,
  IAddressSpace
} from "src/Components/AddressSpaceList/AddressSpaceList";
import { EmptyAddressSpace } from "src/Components/Common/EmptyAddressSpace";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import { DELETE_ADDRESS_SPACE, ALL_ADDRESS_SPACES } from "src/Queries/Quries";
import { CreateAddressSpace } from "./CreateAddressSpace.tsx/CreateAddressSpacePage";

interface IAddressSpacesResponse {
  addressSpaces: {
    Total: number;
    AddressSpaces: Array<{
      ObjectMeta: {
        Name: string;
        Namespace: string;
        CreationTimestamp: string;
      };
      Spec: {
        Type: string;
        Plan: {
          Spec: {
            DisplayName: string;
          };
        };
      };
      Status: {
        IsReady: boolean;
      };
    }>;
  };
}

function AddressSpaceListFunc() {
  useDocumentTitle("Addressspace List");
  useA11yRouteChange();
  const client = useApolloClient();
  const [
    addressSpaceBeingEdited,
    setAddressSpaceBeingEdited
  ] = React.useState<IAddressSpace | null>();

  const [isOpen, setIsOpen] = React.useState(false);
  const [
    addressSpaceBeingDeleted,
    setAddressSpaceBeingDeleted
  ] = React.useState<IAddressSpace | null>();

  const { loading, error, data, refetch } = useQuery<IAddressSpacesResponse>(
    ALL_ADDRESS_SPACES,
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
      console.log(deletedData);
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

  if (loading) return <Loading />;
  if (error) {
    console.log(error);
    return <Loading />;
  }
  console.log(data);

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };
  const addressSpacesList = addressSpaces.AddressSpaces.map(addSpace => ({
    name: addSpace.ObjectMeta.Name,
    nameSpace: addSpace.ObjectMeta.Namespace,
    creationTimestamp: addSpace.ObjectMeta.CreationTimestamp,
    type: addSpace.Spec.Type,
    displayName: addSpace.Spec.Plan.Spec.DisplayName,
    isReady: addSpace.Status.IsReady
  }));
  return (
    <PageSection variant={PageSectionVariants.light}>
      {/* TODO: Replace with component*/}
      {/*START*/}
      {/* <Button
        variant={ButtonVariant.primary}
        style={{ marginBottom: 24, marginLeft: 24 }}
      >
        Create
      </Button> */}
      <CreateAddressSpace />
      <Dropdown
        onSelect={() => {}}
        toggle={
          <KebabToggle
            onToggle={() => {
              setIsOpen(!isOpen);
            }}
          />
        }
        isOpen={isOpen}
        isPlain={true}
        dropdownItems={[<DropdownItem key="Delete">Delete</DropdownItem>]}
      />
      {/*END*/}
      {addressSpaces.Total > 0 ? (
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
          title="Modal Header"
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button key="confirm" variant="primary" onClick={handleSaving}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={handleCancelEdit}>
              Cancel
            </Button>
          ]}
          isFooterLeftAligned={true}
          children
        />
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
    </PageSection>
  );
}

export default function AddressSpaceListPage() {
  return (
    <PageSection>
      <AddressSpaceListFunc />
    </PageSection>
  );
}

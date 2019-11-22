import React from "react";
// import { useHistory, useLocation } from "react-router-dom";
import { gql } from "apollo-boost";
import { useQuery } from "@apollo/react-hooks";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Button,
  ButtonVariant,
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

const ALL_ADDRESS_SPACES = gql`
  query all_address_spaces {
    addressSpaces {
      Total
      AddressSpaces {
        ObjectMeta {
          Namespace
          Name
          CreationTimestamp
        }
        Spec {
          Type
          Plan {
            Spec {
              DisplayName
            }
          }
        }
        Status {
          IsReady
        }
      }
    }
  }
`;

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

  const [
    addressSpaceBeingEdited,
    setAddressSpaceBeingEdited
  ] = React.useState<IAddressSpace | null>();

  const [isOpen, setIsOpen] = React.useState(false);
  const [
    addressSpaceBeingDeleted,
    setAddressSpaceBeingDeleted
  ] = React.useState<IAddressSpace | null>();

  const handleCancelEdit = () => setAddressSpaceBeingEdited(null);
  const handleSaving = () => void 0;
  const handleEditChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingEdited(addressSpace);

  const handleCancelDelete = () => setAddressSpaceBeingDeleted(null);
  const handleDelete = () => void 0;
  const handleDeleteChange = (addressSpace: IAddressSpace) =>
    setAddressSpaceBeingDeleted(addressSpace);
  // const location = useLocation();
  // const history = useHistory();
  // const searchParams = new URLSearchParams(location.search);
  // const page = parseInt(searchParams.get("page") || "", 10) || 0;
  // const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const { loading, error, data } = useQuery<IAddressSpacesResponse>(
    ALL_ADDRESS_SPACES,
    { pollInterval: 20000 }
  );
  // const setSearchParam = React.useCallback(
  //   (name: string, value: string) => {
  //     searchParams.set(name, value.toString());
  //   },
  //   [searchParams]
  // );

  // const handlePageChange = React.useCallback(
  //   (newPage: number) => {
  //     setSearchParam("page", (newPage - 1).toString());
  //     history.push({
  //       search: searchParams.toString()
  //     });
  //   },
  //   [setSearchParam, history, searchParams]
  // );

  // const handlePerPageChange = React.useCallback(
  //   (newPerPage: number) => {
  //     setSearchParam("page", "0");
  //     setSearchParam("perPage", newPerPage.toString());
  //     history.push({
  //       search: searchParams.toString()
  //     });
  //   },
  //   [setSearchParam, history, searchParams]
  // );

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
      <Button variant={ButtonVariant.primary} style={{ marginBottom: 24, marginLeft: 24 }}>Create</Button>
      <Dropdown
        onSelect={() => { }}
        toggle={<KebabToggle onToggle={() => { setIsOpen(!isOpen) }} />}
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

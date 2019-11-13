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
  KebabToggle
} from "@patternfly/react-core";
import { AddressSpaceLsit } from "src/Components/AddressSpaceList/AddressSpaceList";

const ALL_ADDRESS_SPACES = gql`
  query all_address_spaces {
    addressSpaces {
      Total
      AddressSpaces {
        Metadata {
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
      Metadata: {
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
  // const location = useLocation();
  // const history = useHistory();
  // const searchParams = new URLSearchParams(location.search);
  // const page = parseInt(searchParams.get("page") || "", 10) || 0;
  // const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const { loading, data } = useQuery<IAddressSpacesResponse>(
    ALL_ADDRESS_SPACES,
    {pollInterval:2000}
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

  console.log(data);

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };
  const addressSpacesList = addressSpaces.AddressSpaces.map(addSpace => ({
    name: addSpace.Metadata.Name,
    nameSpace: addSpace.Metadata.Namespace,
    creationTimestamp: addSpace.Metadata.CreationTimestamp,
    type: addSpace.Spec.Type,
    displayName: addSpace.Spec.Plan.Spec.DisplayName,
    isReady: addSpace.Status.IsReady
  }));
  return (
    <PageSection variant={PageSectionVariants.light}>
      {/* TODO: Replace with component*/}
      {/*START*/}
      <Button variant={ButtonVariant.primary}>Create</Button>
      <Dropdown
        onSelect={() => {}}
        toggle={<KebabToggle onToggle={() => {}} />}
        isOpen={false}
        isPlain={true}
        dropdownItems={[]}
      />
      {/*END*/}
      <AddressSpaceLsit
        rows={addressSpacesList}
        onEdit={() => {
          console.log("on Edit");
        }}
        onDelete={() => {
          console.log("on Delete");
        }}
      />
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

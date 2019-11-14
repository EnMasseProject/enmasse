import * as React from "react";
import { AddressSpaceNavigation } from "src/Components/AddressSpace/AddressSpaceNavigation";
import {
  useA11yRouteChange,
  useDocumentTitle,
  SwitchWith404,
  LazyRoute,
  Loading
} from "use-patternfly";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { Redirect, BrowserRouter, useParams } from "react-router-dom";
import {
  IAddressSpaceHeaderProps,
  AddressSpaceHeader
} from "src/Components/AddressSpace/AddressSpaceHeader";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";

const getConnectionsList = () => import("./ConnectionsListPage");
const getAddressesList = () => import("./AddressesListPage");

interface IAddressSpaceDetailResponse {
  addressSpaces: {
    AddressSpaces: Array<{
      ObjectMeta: {
        Namespace: string;
        Name: string;
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
        Messages: Array<string>;
      };
    }>;
  };
}
const return_ADDRESS_SPACE_DETAIL = (name?: string, namespace?: string) => {
  const ADDRESS_SPACE_DETAIL = gql`
    query all_address_spaces {
      addressSpaces(
        filter: "\`$..Name\` = '${name}' AND \`$..Namespace\` = '${namespace}'"
      ) {
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
            Messages
          }
        }
      }
    }`;
  return ADDRESS_SPACE_DETAIL;
};

export default function AddressSpaceDetailPage() {
  const { name, namespace, subList } = useParams();
  useA11yRouteChange();
  useDocumentTitle("Address Space Detail");
  const { loading, data } = useQuery<IAddressSpaceDetailResponse>(
    return_ADDRESS_SPACE_DETAIL(name, namespace)
  );

  if (loading) return <Loading />;

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };
  const addressSpaceDetails: IAddressSpaceHeaderProps = {
    name: addressSpaces.AddressSpaces[0].ObjectMeta.Name,
    namespace: addressSpaces.AddressSpaces[0].ObjectMeta.Namespace,
    createdOn: addressSpaces.AddressSpaces[0].ObjectMeta.CreationTimestamp,
    type: addressSpaces.AddressSpaces[0].Spec.Type,
    onDownload: data => {
      console.log(data);
    },
    onDelete: data => {
      console.log(data);
    }
  };

  return (
    <>
      <PageSection
        variant={PageSectionVariants.light}
        style={{ paddingBottom: 0 }}
      >
        <AddressSpaceHeader {...addressSpaceDetails} />
        <AddressSpaceNavigation
          activeItem={subList || "addresses"}
          name={name}
          namespace={namespace}
        ></AddressSpaceNavigation>
      </PageSection>
      <PageSection>
        <SwitchWith404>
          <Redirect path="/" to="/address-spaces" exact={true} />
          <LazyRoute
            path="/address_space/name=:name&namespace=:namespace/addresses"
            getComponent={getAddressesList}
            exact={true}
          />
          <LazyRoute
            path="/address_space/name=:name&namespace=:namespace/connections"
            getComponent={getConnectionsList}
            exact={true}
          />
        </SwitchWith404>
      </PageSection>
      </>
  );
}

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
      Metadata: {
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

export default function AddressSpaceDetailPage() {
  //Chnage
  const name = "jupiter_as1",
    namespace = "app1_ns";
  const ADDRESS_SPACE_DETAIL = gql`
query all_address_spaces {
  addressSpaces(
     filter: "\`$.Metadata.Name\` = '${name}' AND \`$.Metadata.Namespace\` = '${namespace}'"
  ) {
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
        Messages
      }
    }
  }
}
`;
  useA11yRouteChange();
  useDocumentTitle("Address Space Detail");
  const { subList } = useParams();
  const [activeNavItem, setActiveNavItem] = React.useState(
    subList || "addresses"
  );
  const onNavSelect = () => {
    if (subList === "connections" || activeNavItem === "addresses")
      setActiveNavItem("connections");
    if (activeNavItem === "connections" || subList === "addresses")
      setActiveNavItem("addresses");
  };
  const { loading, data } = useQuery<IAddressSpaceDetailResponse>(
    ADDRESS_SPACE_DETAIL
  );

  if (loading) return <Loading />;
  // console.log(data);

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };
  // console.log("add", addressSpaces.AddressSpaces[0]);
  const addressSpaceDetails: IAddressSpaceHeaderProps = {
    name: addressSpaces.AddressSpaces[0].Metadata.Name,
    namespace: addressSpaces.AddressSpaces[0].Metadata.Namespace,
    createdOn: addressSpaces.AddressSpaces[0].Metadata.CreationTimestamp,
    type: addressSpaces.AddressSpaces[0].Spec.Type,
    onDownload: data => {
      console.log(data);
    },
    onDelete: data => {
      console.log(data);
    }
  };

  return (
    <BrowserRouter>
      <PageSection
        variant={PageSectionVariants.light}
        style={{ paddingBottom: 0 }}>
        <AddressSpaceHeader {...addressSpaceDetails} />
        <AddressSpaceNavigation
          activeItem={activeNavItem}
          onSelect={onNavSelect}></AddressSpaceNavigation>
      </PageSection>
      <PageSection>
        <SwitchWith404>
          <Redirect path="/" to="/address-spaces" exact={true} />
          <LazyRoute
            path="/address-space/:id/addresses"
            getComponent={getAddressesList}
            exact={true}
          />
          <LazyRoute
            path="/address-space/:id/connections"
            getComponent={getConnectionsList}
            exact={true}
          />
        </SwitchWith404>
      </PageSection>
    </BrowserRouter>
  );
}

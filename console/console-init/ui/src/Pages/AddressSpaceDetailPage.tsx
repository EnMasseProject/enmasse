import * as React from "react";
import { AddressDetailHeader } from "src/Components/AddressDetail/AddressDetailHeader";
import { AddressSpaceNavigation } from "src/Components/AddressSpace/AddressSpaceNavigation";
import {
  useA11yRouteChange,
  useDocumentTitle,
  SwitchWith404,
  LazyRoute,
  Loading
} from "use-patternfly";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { Redirect, BrowserRouter } from "react-router-dom";
import {
  IAddressSpaceHeaderProps,
  AddressSpaceHeader
} from "src/Components/AddressSpace/AddressSpaceHeader";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";

const getConnectionsList = () => import("./ConnectionsListPage");
const getAddressesList = () => import("./AddressesListPage");

interface IAddressSpaceDetailResponse {
  addressSpace :{
    AddressSpaces: Array<{
      MetaData:{
        Namespace:string;
        Name:string;
        CreationTimestamp:string;
      };
      Spec:{
        Type:{
          Plan:{
            Spec:{
              DisplayName:string;
            };
          };
        };
      };
      Status: {
        IsReady:boolean;
        Messages:Array<string>;
      };
    }>;
  }
}

export default function AddressSpaceDetailPage() {
  //Chnage 
const name ="jupiter_as1", namespace="app1_ns";
const ADDRESS_SPACE_DETAIL= gql`
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

  const [activeNavItem, setActiveNavItem] = React.useState("addresses");
  useA11yRouteChange();
  useDocumentTitle("Address Space Detail");
  const onNavSelect = () => {
    if (activeNavItem === "addresses") setActiveNavItem("connections");
    if (activeNavItem === "connections") setActiveNavItem("addresses");
  };
  const {loading, data} = useQuery<any>(ADDRESS_SPACE_DETAIL)
  
  if(loading) return <Loading/>
  console.log(data);


  const { addressSpaces } = data || {
    connections: { Total: 0, AddressSpaces: [] }
  };
console.log("add",addressSpaces.AddressSpaces[0]);
  const addressSpaceDetails: IAddressSpaceHeaderProps = {
    name:addressSpaces.AddressSpaces[0].Metadata.Name,
    namespace:addressSpaces.AddressSpaces[0].Metadata.Namespace,
    createdOn:addressSpaces.AddressSpaces[0].Metadata.CreationTimestamp,
    type:addressSpaces.AddressSpaces[0].Spec.Type,
    onDownload:(data)=>{console.log(data)},
    onDelete:(data)=>{console.log(data)},
  }
  
  // {
  //   name: "jupiter_as1",
  //   namespace: "app1_ns",
  //   createdOn: "2019-11-10T05:08:31.489Z",
  //   type: "standard",
  //   onDownload: () => {
  //     console.log("on Downlaod");
  //   },
  //   onDelete: () => {
  //     console.log("on Delete");
  //   }
  // };
  return (
    <BrowserRouter>
      <PageSection variant={PageSectionVariants.light}>
        <AddressSpaceHeader {...addressSpaceDetails} />
        {/* <h1>Address Space Detail Page</h1> */}
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

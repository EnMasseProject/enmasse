import * as React from "react";
import { AddressSpaceNavigation } from "src/Components/AddressSpace/AddressSpaceNavigation";
import {
  useA11yRouteChange,
  useDocumentTitle,
  SwitchWith404,
  LazyRoute,
  Loading,
  useBreadcrumb
} from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import { Redirect, useParams, Link } from "react-router-dom";
import {
  IAddressSpaceHeaderProps,
  AddressSpaceHeader
} from "src/Components/AddressSpace/AddressSpaceHeader";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { StyleSheet, css } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  no_bottom_padding: {
    paddingBottom: 0
  }
});
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
const breadcrumb = (
  <Breadcrumb>
    <BreadcrumbItem>
      <Link to={"/"}>Home</Link>
    </BreadcrumbItem>
    <BreadcrumbItem isActive={true}>Address Space</BreadcrumbItem>
  </Breadcrumb>
);
export default function AddressSpaceDetailPage() {
  const { name, namespace, subList } = useParams();

  useA11yRouteChange();
  useBreadcrumb(breadcrumb);
  useDocumentTitle("Address Space Detail");

  const { loading, error, data } = useQuery<IAddressSpaceDetailResponse>(
    return_ADDRESS_SPACE_DETAIL(name, namespace),
    { pollInterval: 2000 }
  );

  if (loading) return <Loading />;

  if (error) {
    console.log(error);
  }

  const { addressSpaces } = data || {
    addressSpaces: { Total: 0, AddressSpaces: [] }
  };

  if (!addressSpaces || addressSpaces.AddressSpaces.length <= 0) {
    return <Loading />;
  }
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
        className={css(styles.no_bottom_padding)}>
        <AddressSpaceHeader {...addressSpaceDetails} />
        <AddressSpaceNavigation
          activeItem={subList || "addresses"}></AddressSpaceNavigation>
      </PageSection>
      <PageSection>
        <SwitchWith404>
          <Redirect path="/" to="/address-spaces" exact={true} />
          <LazyRoute
            path="/address-spaces/:namespace/:name/addresses"
            getComponent={getAddressesList}
            exact={true}
          />
          <LazyRoute
            path="/address-spaces/:namespace/:name/connections"
            getComponent={getConnectionsList}
            exact={true}
          />
        </SwitchWith404>
      </PageSection>
    </>
  );
}

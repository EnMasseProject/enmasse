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
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { StyleSheet, css } from "@patternfly/react-styles";
import { useHistory } from "react-router";
import {
  DOWNLOAD_CERTIFICATE,
  DELETE_ADDRESS_SPACE,
  RETURN_ADDRESS_SPACE_DETAIL
} from "src/Queries/Queries";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
const styles = StyleSheet.create({
  no_bottom_padding: {
    paddingBottom: 0
  }
});
const getConnectionsList = () =>
  import("./ConnectionList/ConnectionListWithFilterAndPaginationPage");
const getAddressesList = () =>
  import("./AddressList/AddressesListWithFilterAndPaginationPage");

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
export interface IObjectMeta_v1_Input {
  name: string;
  namespace: string;
}
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
  const [isDeleteModalOpen, setIsDeleteModalOpen] = React.useState(false);
  const history = useHistory();

  const { loading, error, data } = useQuery<IAddressSpaceDetailResponse>(
    RETURN_ADDRESS_SPACE_DETAIL(name, namespace),
    { pollInterval: 20000 }
  );
  const client = useApolloClient();
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

  //Download the certificate function
  const downloadCertificate = async (data: IObjectMeta_v1_Input) => {
    const dataToDownload = await client.query({
      query: DOWNLOAD_CERTIFICATE,
      variables: {
        as: {
          Name: data.name,
          Namespace: data.namespace
        }
      }
    });
    if (dataToDownload.errors) {
      console.log("Error while download", dataToDownload.errors);
    }
    const url = window.URL.createObjectURL(
      new Blob([dataToDownload.data.messagingCertificateChain])
    );
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `${name}.crt`);
    document.body.appendChild(link);
    link.click();
    if (link.parentNode) link.parentNode.removeChild(link);
  };
  const handleCancelDelete = () => {
    setIsDeleteModalOpen(!isDeleteModalOpen);
  };
  // async function to delete a address space
  const deleteAddressSpace = async (data: IObjectMeta_v1_Input) => {
    const deletedData = await client.mutate({
      mutation: DELETE_ADDRESS_SPACE,
      variables: {
        a: {
          Name: data.name,
          Namespace: data.namespace
        }
      }
    });
    console.log(deletedData);
    if (deletedData.data && deletedData.data.deleteAddressSpace) {
      setIsDeleteModalOpen(!isDeleteModalOpen);
      history.push("/");
    }
  };
  const handleDelete = () => {
    deleteAddressSpace({
      name: addressSpaceDetails.name,
      namespace: addressSpaceDetails.namespace
    });
  };
  const addressSpaceDetails: IAddressSpaceHeaderProps = {
    name: addressSpaces.AddressSpaces[0].ObjectMeta.Name,
    namespace: addressSpaces.AddressSpaces[0].ObjectMeta.Namespace,
    createdOn: addressSpaces.AddressSpaces[0].ObjectMeta.CreationTimestamp,
    type: addressSpaces.AddressSpaces[0].Spec.Type,
    onDownload: data => {
      downloadCertificate(data);
    },
    onDelete: data => {
      setIsDeleteModalOpen(!isDeleteModalOpen);
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
        {isDeleteModalOpen && (
          <DeletePrompt
            detail={`Are you sure you want to delete ${addressSpaceDetails.name} ?`}
            name={addressSpaceDetails.name}
            header="Delete this Address Space ?"
            handleCancelDelete={handleCancelDelete}
            handleConfirmDelete={handleDelete}
          />
        )}
      </PageSection>
      <PageSection>
        <SwitchWith404>
          <Redirect path="/" to="/address-spaces" exact={true} />
          <LazyRoute
            path="/address-spaces/:namespace/:name/:type/addresses/"
            getComponent={getAddressesList}
            exact={true}
          />
          <LazyRoute
            path="/address-spaces/:namespace/:name/:type/connections/"
            getComponent={getConnectionsList}
            exact={true}
          />
        </SwitchWith404>
      </PageSection>
    </>
  );
}

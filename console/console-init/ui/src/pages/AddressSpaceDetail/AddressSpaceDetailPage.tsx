/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { AddressSpaceNavigation } from "components/AddressSpace/AddressSpaceNavigation";
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
  BreadcrumbItem,
  Modal,
  Button
} from "@patternfly/react-core";
import { Redirect, useParams, Link } from "react-router-dom";
import {
  IAddressSpaceHeaderProps,
  AddressSpaceHeader
} from "components/AddressSpace/AddressSpaceHeader";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { StyleSheet, css } from "@patternfly/react-styles";
import { useHistory } from "react-router";
import {
  DOWNLOAD_CERTIFICATE,
  DELETE_ADDRESS_SPACE,
  RETURN_ADDRESS_SPACE_DETAIL,
  EDIT_ADDRESS_SPACE
} from "queries";
import { DialoguePrompt } from "components/common/DialoguePrompt";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";
import { ErrorAlert } from "components/common/ErrorAlert";
import { NoDataFound } from "components/common/NoDataFound";
import { EditAddressSpace } from "pages/EditAddressSpace";
import { IAddressSpace } from "components/AddressSpaceList/AddressSpaceList";
import { string } from "prop-types";
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
    addressSpaces: Array<{
      metadata: {
        namespace: string;
        name: string;
        creationTimestamp: string;
      };
      spec: {
        type: string;
        plan: {
          metadata: {
            name: string;
          };
          spec: {
            displayName: string;
          };
        };
        authenticationService: {
          name: string;
        };
      };
      status: {
        isReady: boolean;
        phase: string;
        messages: Array<string>;
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
  const [isEditModalOpen, setIsEditModalOpen] = React.useState(false);
  const [
    addressSpaceBeingEdited,
    setAddressSpaceBeingEdited
  ] = React.useState<IAddressSpace | null>();
  const history = useHistory();

  const { loading, error, data, refetch } = useQuery<
    IAddressSpaceDetailResponse
  >(RETURN_ADDRESS_SPACE_DETAIL(name, namespace), {
    fetchPolicy: FetchPolicy.NETWORK_ONLY,
    pollInterval: POLL_INTERVAL
  });

  const client = useApolloClient();
  if (loading) return <Loading />;

  if (error) {
    return <ErrorAlert error={error} />;
  }
  const { addressSpaces } = data || {
    addressSpaces: { total: 0, addressSpaces: [] }
  };
  if (addressSpaces.addressSpaces.length <= 0) {
    return (
      <NoDataFound type={"Address Space"} name={name || ""} routeLink={"/"} />
    );
  }

  const addressSpace: IAddressSpace | null =
    addressSpaces && addressSpaces.addressSpaces.length > 0
      ? {
          name: addressSpaces.addressSpaces[0].metadata.name,
          nameSpace: addressSpaces.addressSpaces[0].metadata.namespace,
          creationTimestamp:
            addressSpaces.addressSpaces[0].metadata.creationTimestamp,
          type: addressSpaces.addressSpaces[0].spec.type,
          displayName:
            addressSpaces.addressSpaces[0].spec.plan.spec.displayName,
          planValue: addressSpaces.addressSpaces[0].spec.plan.metadata.name,
          isReady: addressSpaces.addressSpaces[0].status.isReady,
          phase: addressSpaces.addressSpaces[0].status.phase,
          messages: addressSpaces.addressSpaces[0].status.messages,
          authenticationService:
            addressSpaces.addressSpaces[0].spec.authenticationService.name
        }
      : null;

  //Download the certificate function
  const downloadCertificate = async (data: IObjectMeta_v1_Input) => {
    const dataToDownload = await client.query({
      query: DOWNLOAD_CERTIFICATE,
      variables: {
        as: {
          name: data.name,
          namespace: data.namespace
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
          name: data.name,
          namespace: data.namespace
        }
      }
    });
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
    name: addressSpaces.addressSpaces[0].metadata.name,
    namespace: addressSpaces.addressSpaces[0].metadata.namespace,
    createdOn: addressSpaces.addressSpaces[0].metadata.creationTimestamp,
    type: addressSpaces.addressSpaces[0].spec.type,
    onDownload: data => {
      downloadCertificate(data);
    },
    onDelete: data => {
      setIsDeleteModalOpen(!isDeleteModalOpen);
    },
    onEdit: () => {
      setIsEditModalOpen(true);
      setAddressSpaceBeingEdited(addressSpace);
    }
  };

  const handleCancelEdit = () => {
    setIsEditModalOpen(false);
    setAddressSpaceBeingEdited(null);
  };
  const handleSaving = async () => {
    addressSpaceBeingEdited &&
      (await client.mutate({
        mutation: EDIT_ADDRESS_SPACE,
        variables: {
          a: {
            name: addressSpaceBeingEdited.name,
            namespace: addressSpaceBeingEdited.nameSpace
          },
          jsonPatch:
            '[{"op":"replace","path":"/spec/plan","value":"' +
            addressSpaceBeingEdited.planValue +
            '"},' +
            '{"op":"replace","path":"/spec/authenticationService/name","value":"' +
            addressSpaceBeingEdited.authenticationService +
            '"}' +
            "]",
          patchType: "application/json-patch+json"
        }
      }));
    setIsEditModalOpen(false);
    setAddressSpaceBeingEdited(null);
    refetch();
  };

  const handlePlanChange = (plan: string) => {
    if (addressSpaceBeingEdited) {
      addressSpaceBeingEdited.planValue = plan;
      setAddressSpaceBeingEdited({ ...addressSpaceBeingEdited });
    }
  };

  const handleAuthServiceChanged = (authService: string) => {
    if (addressSpaceBeingEdited) {
      addressSpaceBeingEdited.authenticationService = authService;
      setAddressSpaceBeingEdited({ ...addressSpaceBeingEdited });
    }
  };

  return (
    <>
      <PageSection
        variant={PageSectionVariants.light}
        className={css(styles.no_bottom_padding)}
      >
        <AddressSpaceHeader {...addressSpaceDetails} />
        <AddressSpaceNavigation
          activeItem={subList || "addresses"}
        ></AddressSpaceNavigation>
        {isDeleteModalOpen && (
          <DialoguePrompt
            option="Delete"
            detail={`Are you sure you want to delete this addressspace: ${addressSpaceDetails.name} ?`}
            names={[addressSpaceDetails.name]}
            header="Delete this Address Space ?"
            handleCancelDialogue={handleCancelDelete}
            handleConfirmDialogue={handleDelete}
          />
        )}
      </PageSection>
      {isEditModalOpen && addressSpaceBeingEdited && (
        <Modal
          isLarge
          id="as-list-edit-modal"
          title="Edit"
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button
              key="confirm"
              id="as-list-edit-confirm"
              variant="primary"
              onClick={handleSaving}
            >
              Confirm
            </Button>,
            <Button
              key="cancel"
              id="as-list-edit-cancel"
              variant="link"
              onClick={handleCancelEdit}
            >
              Cancel
            </Button>
          ]}
          isFooterLeftAligned={true}
        >
          <EditAddressSpace
            addressSpace={addressSpaceBeingEdited}
            onAuthServiceChanged={handleAuthServiceChanged}
            onPlanChange={handlePlanChange}
          />
        </Modal>
      )}
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

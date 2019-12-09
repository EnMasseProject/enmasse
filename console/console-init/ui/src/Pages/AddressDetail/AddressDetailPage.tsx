import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  BreadcrumbItem,
  Breadcrumb,
  Title,
  Pagination,
  Modal,
  Button
} from "@patternfly/react-core";
import { useBreadcrumb, useA11yRouteChange, Loading } from "use-patternfly";
import { Link, useLocation, useHistory } from "react-router-dom";
import { useParams } from "react-router";
import { AddressDetailHeader } from "src/Components/AddressDetail/AddressDetailHeader";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { IAddressDetailResponse } from "src/Types/ResponseTypes";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { css } from "@patternfly/react-styles";
import { GridStylesForTableHeader } from "../AddressSpace/AddressesListWithFilterAndPaginationPage";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import { DELETE_ADDRESS, RETURN_ADDRESS_DETAIL, EDIT_ADDRESS } from "src/Queries/Queries";
import { IObjectMeta_v1_Input } from "../AddressSpace/AddressSpaceDetailPage";
import { AddressLinksListPage } from "./AddressLinksListPage";
import { EditAddress } from "../EditAddressPage";
import { getPlanAndTypeForAddressEdit } from "src/Components/Common/AddressFormatter";

export default function AddressDetailPage() {
  const { namespace, name, type, addressname } = useParams();
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const breadcrumb = React.useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link to={"/"}>Home</Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link to={`/address-spaces/${namespace}/${name}/${type}/addresses`}>
            {name}
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem isActive={true}>Address</BreadcrumbItem>
      </Breadcrumb>
    ),
    [name, namespace, type]
  );

  const setSearchParam = React.useCallback(
    (name: string, value: string) => {
      searchParams.set(name, value.toString());
    },
    [searchParams]
  );

  const handlePageChange = React.useCallback(
    (_: any, newPage: number) => {
      setSearchParam("page", newPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  const handlePerPageChange = React.useCallback(
    (_: any, newPerPage: number) => {
      setSearchParam("page", "1");
      setSearchParam("perPage", newPerPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  useA11yRouteChange();
  useBreadcrumb(breadcrumb);
  const client = useApolloClient();
  const [isDeleteModalOpen, setIsDeleteModalOpen] = React.useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = React.useState(false);
  const [addresLinksTotal, setAddressLinksTotal] = React.useState<number>(0);
  const [addressPlan, setAddressPlan] = React.useState<string|null>(null);
  const { loading, error, data, refetch } = useQuery<IAddressDetailResponse>(
    RETURN_ADDRESS_DETAIL(page, perPage, name, namespace, addressname),
    { pollInterval: 20000 }
  );
  if (loading) return <Loading />;
  if (error) console.log(error);
  console.log(data);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  const addressDetail = addresses && addresses.Addresses[0];
  if(addressPlan===null){
    setAddressPlan(addressDetail.Spec.Plan.Spec.DisplayName);
  }

  const handleCancelDelete = () => {
    setIsDeleteModalOpen(!isDeleteModalOpen);
  };
  // async function to delete a address space
  const deleteAddressSpace = async (data: IObjectMeta_v1_Input) => {
    const deletedData = await client.mutate({
      mutation: DELETE_ADDRESS,
      variables: {
        a: {
          Name: data.name,
          Namespace: data.namespace
        }
      }
    });
    console.log(deletedData);
    if (deletedData.data && deletedData.data.deleteAddress) {
      setIsDeleteModalOpen(!isDeleteModalOpen);
      history.push(`/address-spaces/${namespace}/${name}/${type}/addresses`);
    }
  };
  const handleDelete = () => {
    deleteAddressSpace({
      name: addressDetail.ObjectMeta.Name,
      namespace: addressDetail.ObjectMeta.Namespace.toString()
    });
  };

  const renderPagination = (page: number, perPage: number) => {
    return (
      <Pagination
        itemCount={addresLinksTotal}
        perPage={perPage}
        page={page}
        onSetPage={handlePageChange}
        variant="top"
        onPerPageSelect={handlePerPageChange}
      />
    );
  };

  const handleSaving = async () => {
    if(addressDetail && type){
      await client.mutate({
        mutation: EDIT_ADDRESS,
        variables: {
          a: {
            Name: addressDetail.ObjectMeta.Name,
            Namespace: addressDetail.ObjectMeta.Namespace.toString()
          },
          jsonPatch:
            '[{"op":"replace","path":"/Plan","value":"' +
            getPlanAndTypeForAddressEdit(
              addressPlan || "",
              type
            ) +
            '"}]',
          patchType: "application/json-patch+json"
        }
      });
    }
    setIsEditModalOpen(!isEditModalOpen);
  };

  return (
    <>
      {addressDetail && (
        <AddressDetailHeader
          type={type || ""}
          name={addressDetail.Spec.Address}
          plan={addressDetail.Spec.Plan.Spec.DisplayName}
          shards={getFilteredValue(
            addressDetail.Metrics,
            "enmasse_messages_stored"
          )}
          onEdit={() => setIsEditModalOpen(!isEditModalOpen)}
          onDelete={() => setIsDeleteModalOpen(!isDeleteModalOpen)}
        />
      )}
      <PageSection>
        <PageSection variant={PageSectionVariants.light}>
          <Title
            size={"lg"}
            className={css(GridStylesForTableHeader.filter_left_margin)}
          >
            Clients
          </Title>
          {addresLinksTotal > 0 && renderPagination(page, perPage)}
          <AddressLinksListPage
            page={page}
            perPage={perPage}
            name={name}
            namespace={namespace}
            addressname={addressname}
            setAddressLinksTotal={setAddressLinksTotal}
            type={type}
          />
          {addresLinksTotal > 0 && renderPagination(page, perPage)}
        </PageSection>
        {isDeleteModalOpen && (
          <DeletePrompt
            detail={`Are you sure you want to delete ${addressDetail.ObjectMeta.Name} ?`}
            name={addressDetail.ObjectMeta.Name}
            header="Delete this Address ?"
            handleCancelDelete={handleCancelDelete}
            handleConfirmDelete={handleDelete}
          />
        )}
        <Modal
          title="Edit"
          isSmall
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(!isEditModalOpen)}
          actions={[
            <Button key="confirm" variant="primary" onClick={handleSaving}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={() => setIsEditModalOpen(!isEditModalOpen)}>
              Cancel
            </Button>
          ]}
          isFooterLeftAligned>
          <EditAddress
            name={addressDetail.ObjectMeta.Name}
            type={addressDetail.Spec.Plan.Spec.AddressType}
            plan={addressPlan||""} 
            onChange={setAddressPlan}
          />
        </Modal>
      </PageSection>
    </>
  );
}

import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  BreadcrumbItem,
  Breadcrumb,
  Title,
  Pagination
} from "@patternfly/react-core";
import { useBreadcrumb, useA11yRouteChange, Loading } from "use-patternfly";
import { Link } from "react-router-dom";
import { useParams, useHistory } from "react-router";
import { AddressDetailHeader } from "src/Components/AddressDetail/AddressDetailHeader";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { IAddressDetailResponse } from "src/Types/ResponseTypes";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { css } from "@patternfly/react-styles";
import { GridStylesForTableHeader } from "../AddressSpace/AddressesListWithFilterAndPaginationPage";
import { DeletePrompt } from "src/Components/Common/DeletePrompt";
import { DELETE_ADDRESS, RETURN_ADDRESS_DETAIL } from "src/Queries/Quries";
import { IObjectMeta_v1_Input } from "../AddressSpace/AddressSpaceDetailPage";
import { AddressLinksListPage } from "./AddressLinksListPage";

export default function AddressDetailPage() {
  const { namespace, name, type, addressname } = useParams();
  const history = useHistory();
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
  useA11yRouteChange();
  useBreadcrumb(breadcrumb);
  const client = useApolloClient();
  const [isDeleteModalOpen, setIsDeleteModalOpen] = React.useState(false);
  const [addresLinksTotal, setAddressLinksTotal] = React.useState<number>(0);
  const { loading, error, data } = useQuery<IAddressDetailResponse>(
    RETURN_ADDRESS_DETAIL(name, namespace, addressname),
    { pollInterval: 20000 }
  );
  if (loading) return <Loading />;
  if (error) console.log(error);
  console.log(data);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  const addressDetail = addresses && addresses.Addresses[0];

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
          onEdit={() => {}}
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
          {addresLinksTotal > 0 && (
            <Pagination
              itemCount={addresLinksTotal}
              perPage={10}
              page={1}
              onSetPage={() => {}}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={() => {}}
            />
          )}
          <AddressLinksListPage
            name={name}
            namespace={namespace}
            addressname={addressname}
            setAddressLinksTotal={setAddressLinksTotal}
            type={type}
          />
          {addresLinksTotal > 0 && (
            <Pagination
              itemCount={addresLinksTotal}
              perPage={10}
              page={1}
              onSetPage={() => {}}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={() => {}}
            />
          )}
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
      </PageSection>
    </>
  );
}

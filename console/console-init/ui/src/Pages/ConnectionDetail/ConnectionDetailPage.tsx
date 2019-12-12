import * as React from "react";
import {
  ConnectionDetailHeader,
  IConnectionHeaderDetailProps
} from "src/Components/ConnectionDetail/ConnectionDetailHeader";
import {
  PageSection,
  PageSectionVariants,
  Title,
  Breadcrumb,
  BreadcrumbItem,
  Pagination
} from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
import { useParams } from "react-router";
import { Loading, useA11yRouteChange, useBreadcrumb } from "use-patternfly";
import { ILink, LinkList } from "src/Components/ConnectionDetail/LinkList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionDetailResponse } from "src/Types/ResponseTypes";
import { css } from "@patternfly/react-styles";
import { GridStylesForTableHeader } from "../AddressSpaceDetail/AddressList/AddressesListWithFilterAndPaginationPage";
import { Link, useLocation, useHistory } from "react-router-dom";
import { RETURN_CONNECTION_DETAIL } from "src/Queries/Queries";
import { ConnectionLinksListPage } from "./ConnectionsLinksListPage";

const getProductFilteredValue = (object: any[], value: string) => {
  const filtered = object.filter(obj => obj.Key === value);
  if (filtered.length > 0) {
    return filtered[0].Value;
  }
  return 0;
};

const getSplitValue = (value: string) => {
  let string1 = value.split(", OS: ");
  let string2 = string1[0].split("JVM:");
  let os, jvm;
  if (string1.length > 1) {
    os = string1[1];
  }
  if (string2.length > 0) {
    jvm = string2[1];
  }
  return { jvm: jvm, os: os };
};

export default function ConnectionDetailPage() {
  const { name, namespace, type, connectionname } = useParams();
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const [totalLinks, setTotalLinks] = React.useState<number>(0);
  const page = parseInt(searchParams.get("page") || "", 10) || 0;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const breadcrumb = React.useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link to={"/"}>Home</Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link to={`/address-spaces/${namespace}/${name}/${type}/connections`}>
            {name}
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem isActive={true}>Connection</BreadcrumbItem>
      </Breadcrumb>
    ),
    [name, namespace, type]
  );

  useBreadcrumb(breadcrumb);

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
      setSearchParam("page", "0");
      setSearchParam("perPage", newPerPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );
  useA11yRouteChange();
  // useBreadcrumb(breadcrumb);
  const { loading, error, data } = useQuery<IConnectionDetailResponse>(
    RETURN_CONNECTION_DETAIL(name || "", namespace || "", connectionname || ""),
    { pollInterval: 20000 }
  );
  if (loading) return <Loading />;
  if (error) {
    console.log(error);
    return <Loading />;
  }
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };
  const connection = connections.Connections[0];
  // setTotalLinks(connections.Total);
  //Change this logic
  const jvmObject = getSplitValue(
    getProductFilteredValue(connection.Spec.Properties, "platform")
  );

  const connectionDetail: IConnectionHeaderDetailProps = {
    hostname: connection.ObjectMeta.Name,
    containerId: connection.ObjectMeta.Namespace,
    version: getProductFilteredValue(connection.Spec.Properties, "version"),
    protocol: connection.Spec.Protocol.toUpperCase(),
    encrypted: connection.Spec.Encrypted || false,
    messagesIn: getFilteredValue(connection.Metrics, "enmasse_messages_in"),
    messagesOut: getFilteredValue(connection.Metrics, "enmasse_messages_out"),
    //Change this logic
    platform: jvmObject.jvm,
    os: jvmObject.os,
    product: getProductFilteredValue(connection.Spec.Properties, "product")
  };

  const renderPagination = (page: number, perPage: number) => {
    return (
      <Pagination
        itemCount={totalLinks}
        perPage={perPage}
        page={page}
        onSetPage={handlePageChange}
        variant="top"
        onPerPageSelect={handlePerPageChange}
      />
    );
  };

  return (
    <>
      <ConnectionDetailHeader {...connectionDetail} />
      <PageSection>
        <PageSection variant={PageSectionVariants.light}>
          <Title
            size={"lg"}
            className={css(GridStylesForTableHeader.filter_left_margin)}>
            Links
          </Title>
          {renderPagination(page, perPage)}
          <ConnectionLinksListPage
            name={name || ""}
            namespace={namespace || ""}
            connectionName={connectionname || ""}
            page={page}
            perPage={perPage}
            setTotalLinks={setTotalLinks}
          />
          {renderPagination(page, perPage)}
        </PageSection>
      </PageSection>
    </>
  );
}

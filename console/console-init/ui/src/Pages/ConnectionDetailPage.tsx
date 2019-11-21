import * as React from "react";
import {
  ConnectionDetailHeader,
  IConnectionHeaderDetailProps
} from "src/Components/ConnectionDetail/ConnectionDetailHeader";
import {
  PageSection,
  PageSectionVariants,
  Title
  // Breadcrumb,
  // BreadcrumbItem
} from "@patternfly/react-core";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { useParams } from "react-router";
import {
  Loading,
  useA11yRouteChange
  // useBreadcrumb
} from "use-patternfly";
import { ILink, LinkList } from "src/Components/LinkList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionDetailResponse } from "src/Types/ResponseTypes";
import { css } from "@patternfly/react-styles";
import { GridStylesForTableHeader } from "./AddressesListPage";
// import { Link } from "react-router-dom";

const RETURN_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Namespace` = '" +
      addressSpaceNameSpcae +
      "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }

  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "${filter}"
    ) {
      Total
      Connections {
        ObjectMeta {
          Name
          Namespace
          CreationTimestamp
          ResourceVersion
        }
        Spec {
          Hostname
          ContainerId
          Protocol,
          Properties{
            Key
            Value
          }
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
        Links {
          Total
          Links {
            ObjectMeta {
              Name
              Namespace
            }
            Spec {
              Role
            }
            Metrics {
              Name
              Type
              Value
              Units
            }
          }
        }
      }
    }
  }
  `;
  return CONNECTION_DETAIL;
};

// const return_breadCrumb = (name?: string, namespace?: string) => {
//   const breadcrumb = (
//     <Breadcrumb>
//       <BreadcrumbItem>
//         <Link to={"/"}>Home</Link>
//       </BreadcrumbItem>
//       <BreadcrumbItem>
//         <Link to={`/address-spaces/${namespace}/${name}`}>{name}</Link>
//       </BreadcrumbItem>
//     </Breadcrumb>
//   );
//   return breadcrumb;
// };

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
  const { name, namespace, connectionname } = useParams();
  // useBreadcrumb(breadcrumb);
  // const breadcrumb = (
  //   <Breadcrumb>
  //     <BreadcrumbItem>
  //       <Link to={"/"}>Home</Link>
  //     </BreadcrumbItem>
  //   </Breadcrumb>
  // );
  useA11yRouteChange();
  // useBreadcrumb(breadcrumb);
  const { loading, error, data } = useQuery<IConnectionDetailResponse>(
    RETURN_CONNECTION_DETAIL(name || "", namespace || "", connectionname || ""),
    { pollInterval: 5000 }
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
  console.log(connection);

  //Change this logic
  const jvmObject = getSplitValue(
    getProductFilteredValue(connection.Spec.Properties, "platform")
  );
  const connectionDetail: IConnectionHeaderDetailProps = {
    hostname: connection.ObjectMeta.Name,
    containerId: connection.ObjectMeta.Namespace,
    version: getProductFilteredValue(connection.Spec.Properties, "version"),
    protocol: connection.Spec.Protocol.toUpperCase(),
    messagesIn: getFilteredValue(connection.Metrics, "enmasse_messages_in"),
    messagesOut: getFilteredValue(connection.Metrics, "enmasse_messages_out"),
    //Change this logic
    platform: jvmObject.jvm,
    os: jvmObject.os,
    product: getProductFilteredValue(connection.Spec.Properties, "product")
  };
  const linkRows: ILink[] = connection.Links.Links.map(link => ({
    name: link.ObjectMeta.Name,
    role: link.Spec.Role,
    address: link.ObjectMeta.Namespace,
    deliveries: getFilteredValue(link.Metrics, "enmasse_deliveries"),
    rejected: getFilteredValue(link.Metrics, "enmasse_rejected"),
    released: getFilteredValue(link.Metrics, "enmasse_released"),
    modified: getFilteredValue(link.Metrics, "enmasse_modified"),
    presettled: getFilteredValue(link.Metrics, "enmasse_presettled"),
    undelivered: getFilteredValue(link.Metrics, "enmasse_undelivered")
  }));

  // useBreadcrumb(return_breadCrumb(name,namespace));
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
          <LinkList rows={linkRows} />
        </PageSection>
      </PageSection>
    </>
  );
}

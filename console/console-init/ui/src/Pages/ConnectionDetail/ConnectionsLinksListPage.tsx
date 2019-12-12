import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { IConnectionLinksResponse } from "src/Types/ResponseTypes";
import { RETURN_CONNECTION_LINKS } from "src/Queries/Queries";
import { Loading } from "use-patternfly";
import { ILink, LinkList } from "src/Components/ConnectionDetail/LinkList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";

interface IConnectionLinksListPageProps {
  name: string;
  namespace: string;
  connectionName: string;
  page: number;
  perPage: number;
  setTotalLinks: (value: number) => void;
}
export const ConnectionLinksListPage: React.FunctionComponent<IConnectionLinksListPageProps> = ({
  name,
  namespace,
  connectionName,
  page,
  perPage,
  setTotalLinks
}) => {
  const { loading, error, data } = useQuery<IConnectionLinksResponse>(
    RETURN_CONNECTION_LINKS(
      page,
      perPage,
      name || "",
      namespace || "",
      connectionName || ""
    ),
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
  let linkRows: ILink[] = [];
  if (connection && connection.Links.Total > 0) {
    setTotalLinks(connection.Links.Total);
    linkRows = connection.Links.Links.map(link => ({
      name: link.ObjectMeta.Name,
      role: link.Spec.Role,
      //change it after confiramtion
      address: link.ObjectMeta.Namespace,
      deliveries: getFilteredValue(link.Metrics, "enmasse_deliveries"),
      rejected: getFilteredValue(link.Metrics, "enmasse_rejected"),
      released: getFilteredValue(link.Metrics, "enmasse_released"),
      modified: getFilteredValue(link.Metrics, "enmasse_modified"),
      presettled: getFilteredValue(link.Metrics, "enmasse_presettled"),
      undelivered: getFilteredValue(link.Metrics, "enmasse_undelivered")
    }));
  }
  return (
    <>
      <LinkList rows={linkRows} />
    </>
  );
};

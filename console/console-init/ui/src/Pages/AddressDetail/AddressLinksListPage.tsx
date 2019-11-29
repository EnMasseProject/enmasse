import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ADDRESS_LINKS } from "src/Queries/Quries";
import { IAddressLinksResponse } from "src/Types/ResponseTypes";
import { Loading } from "use-patternfly";
import { IClient, ClientList } from "src/Components/ClientList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";

export interface IAddressLinksListProps {
  name?: string;
  namespace?: string;
  addressname?: string;
  type?: string;
  setAddressLinksTotal: (total: number) => void;
}
export const AddressLinksListPage: React.FunctionComponent<IAddressLinksListProps> = ({
  name,
  namespace,
  addressname,
  type,
  setAddressLinksTotal
}) => {
  const { loading, error, data } = useQuery<IAddressLinksResponse>(
    RETURN_ADDRESS_LINKS(name, namespace, addressname),
    { pollInterval: 20000 }
  );
  if (loading) return <Loading />;
  if (error) console.log(error);
  console.log(data);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  console.log(addresses);
  const links = addresses.Addresses[0].Links;
  setAddressLinksTotal(addresses.Addresses[0].Links.Total);
  let clientRows: IClient[] = addresses.Addresses[0].Links.Links.map(link => ({
    role: link.Spec.Role.toString(),
    containerId: link.ObjectMeta.Namespace,
    name: link.ObjectMeta.Name,
    deliveryRate: getFilteredValue(link.Metrics, "enmasse_messages_in"),
    backlog: getFilteredValue(link.Metrics, "enmasse_messages_backlog"),
    connectionName: link.Spec.Connection.ObjectMeta.Name,
    addressSpaceName: name,
    addressSpaceNamespace: namespace,
    addressSpaceType: type
  }));
  console.log(clientRows);
  return (
    <>
      {links.Total > 0 ? (
        <ClientList rows={clientRows} />
      ) : (
        // <EmptyAddressSpace />
        ""
      )}
    </>
  );
};

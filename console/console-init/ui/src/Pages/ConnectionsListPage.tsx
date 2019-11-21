import * as React from "react";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import {
  IConnection,
  ConnectionList
} from "src/Components/AddressSpace/ConnectionList";
import { EmptyConnection } from "src/Components/Common/EmptyConnection";
import { useParams } from "react-router";
import {
  Pagination,
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionListResponse } from "src/Types/ResponseTypes";
import { css } from "@patternfly/react-styles";
import {
  FilterDropdown,
  IDropdownOption
} from "src/Components/Common/FilterDropdown";
import { SearchIcon } from "@patternfly/react-icons";
import { GridStylesForTableHeader } from "./AddressesListPage";
const RETURN_ALL_CONECTION_LIST = (name?: string, namespace?: string) => {
  let filter = "";
  if (name) {
    filter += "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + name + "'";
  }
  if (namespace) {
    filter +=
      " AND `$.Spec.AddressSpace.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
    connections(
      filter: "${filter}"
    ) {
      Total
      Connections {
        ObjectMeta {
          Name
        }
        Spec {
          Hostname
          ContainerId
          Protocol
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
      }
    }
  }`
  );
  return ALL_CONECTION_LIST;
};
export default function ConnectionsListPage() {
  const { name, namespace } = useParams();
  let { loading, error, data } = useQuery<IConnectionListResponse>(
    RETURN_ALL_CONECTION_LIST(name, namespace),
    { pollInterval: 5000 }
  );

  if (error) console.log(error);
  if (loading) return <Loading />;
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };

  console.log(connections);
  // connections.Total=0;
  // connections.Connections=[];
  const connectionList: IConnection[] = connections.Connections.map(
    connection => ({
      hostname: connection.Spec.Hostname,
      containerId: connection.Spec.ContainerId,
      protocol: connection.Spec.Protocol,
      messagesIn: getFilteredValue(connection.Metrics, "enmasse_messages_in"),
      messagesOut: getFilteredValue(connection.Metrics, "enmasse_messages_out"),
      senders: getFilteredValue(connection.Metrics, "enmasse_senders"),
      receivers: getFilteredValue(connection.Metrics, "enmasse_receivers"),
      status: "running"
    })
  );

  const filterOptions: IDropdownOption[] = [
    { value: "container", label: "Container" },
    { value: "miss", label: "Miss", disabled: false },
    { value: "mrs", label: "Mrs", disabled: false },
    { value: "ms", label: "Ms", disabled: false },
    { value: "dr", label: "Dr", disabled: false },
    { value: "other", label: "Other", disabled: false }
  ];
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid className={css(GridStylesForTableHeader.grid_bottom_border)}>
        <GridItem
          span={4}
          className={css(GridStylesForTableHeader.filter_left_margin)}>
          <InputGroup>
            {/** Add the logic for select for filter and dropdown */}
            <FilterDropdown
              value="Container"
              onSelect={() => {}}
              options={filterOptions}
            />
            <InputGroup>
              <TextInput
                name="search name"
                id="searchName"
                type="search"
                placeholder="Filter by container ID..."
                aria-label="search input container"
              />
              <Button
                variant={ButtonVariant.control}
                aria-label="search button for search input"
                onClick={() => {
                  console.log("search icon clicked");
                }}>
                <SearchIcon />
              </Button>
            </InputGroup>
          </InputGroup>
        </GridItem>
        <GridItem span={8}>
          {connections.Total === 0 ? (
            ""
          ) : (
            <Pagination
              itemCount={523}
              perPage={10}
              page={1}
              onSetPage={() => {}}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={() => {}}
            />
          )}
        </GridItem>
      </Grid>
      <ConnectionList rows={connectionList ? connectionList : []} />
      {connections.Total === 0 ? (
        <EmptyConnection />
      ) : (
        <Pagination
          itemCount={523}
          perPage={10}
          page={1}
          onSetPage={() => {}}
          widgetId="pagination-options-menu-top"
          onPerPageSelect={() => {}}
        />
      )}
    </PageSection>
  );
}

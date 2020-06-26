/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams, useLocation } from "react-router";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { Divider } from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import {
  ConnectionContainer,
  ConnectionToolbarContainer
} from "modules/connection/containers";
import { TablePagination } from "components";
import { IConnection } from "./components";
import { compareObject } from "utils";
import { useStoreContext, MODAL_TYPES, types } from "context-state-reducer";
import {
  getFilteredConnectionNames,
  getHeaderTextForCloseAll,
  getDetailTextForCloseAll
} from "./utils";
import { useMutationQuery } from "hooks";
import { CLOSE_CONNECTION } from "graphql-module";

export default function ConnectionPage() {
  const { dispatch } = useStoreContext();
  useDocumentTitle("Connection List");
  useA11yRouteChange();
  const [hostnames, setHostnames] = useState<Array<string>>([]);
  const [containerIds, setContainerIds] = useState<Array<string>>([]);
  const [totalConnections, setTotalConnections] = useState<number>(0);
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();
  const { name, namespace, type } = useParams();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const [selectedConnections, setSelectedConnections] = useState<IConnection[]>(
    []
  );

  const refetchQueries: string[] = ["all_connections_for_addressspace_view"];

  const [setCloseConnectionQueryVariables] = useMutationQuery(
    CLOSE_CONNECTION,
    refetchQueries
  );
  const renderPagination = (page: number, perPage: number) => {
    return (
      <TablePagination
        itemCount={totalConnections}
        perPage={perPage}
        page={page}
        variant="top"
      />
    );
  };

  const onSelectConnection = (data: IConnection, isSelected: boolean) => {
    if (isSelected === true && selectedConnections.indexOf(data) === -1) {
      setSelectedConnections(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedConnections(prevState =>
        prevState.filter(
          connection =>
            !compareObject(
              {
                name: connection.name,
                containerId: connection.containerId
              },
              { name: data.name, containerId: data.containerId }
            )
        )
      );
    }
  };

  const onSelectAllConnection = (
    dataList: IConnection[],
    isSelected: boolean
  ) => {
    if (isSelected === true) {
      setSelectedConnections(dataList);
    } else if (isSelected === false) {
      setSelectedConnections([]);
    }
  };

  const onCloseAll = () => {
    let queryConnectionVariable: Array<{
      name: string;
      namespace: string;
    }> = [];
    namespace &&
      selectedConnections.map((connection: IConnection) =>
        queryConnectionVariable.push({
          name: connection.name,
          namespace: namespace
        })
      );
    if (queryConnectionVariable.length > 0) {
      setCloseConnectionQueryVariables({ cons: queryConnectionVariable });
    }
    setSelectedConnections([]);
  };

  const onCloseAllConnections = async () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CLOSE_CONNECTIONS,
      modalProps: {
        option: "Close",
        header: getHeaderTextForCloseAll(selectedConnections),
        onConfirm: onCloseAll,
        selectedItems: getFilteredConnectionNames(selectedConnections),
        detail: getDetailTextForCloseAll(selectedConnections)
      }
    });
  };

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem span={6}>
          <ConnectionToolbarContainer
            selectedHostnames={hostnames}
            setSelectedHostnames={setHostnames}
            selectedContainers={containerIds}
            setSelectedContainers={setContainerIds}
            totalRecords={totalConnections}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            namespace={namespace || ""}
            addressSpaceName={name || ""}
            selectedConnections={selectedConnections}
            onCloseAllConnections={onCloseAllConnections}
          />
        </GridItem>
        <GridItem span={6}>{renderPagination(page, perPage)}</GridItem>
      </Grid>
      <Divider />
      <ConnectionContainer
        name={name}
        namespace={namespace}
        hostnames={hostnames}
        containerIds={containerIds}
        setTotalConnections={setTotalConnections}
        page={page}
        perPage={perPage}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
        addressSpaceType={type}
        selectedConnections={selectedConnections}
        onSelectAllConnection={onSelectAllConnection}
        onSelectConnection={onSelectConnection}
      />
      {renderPagination(page, perPage)}
    </PageSection>
  );
}

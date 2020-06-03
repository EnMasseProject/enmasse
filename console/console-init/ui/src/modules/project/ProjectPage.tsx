/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useLocation } from "react-router";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem,
  Page
} from "@patternfly/react-core";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { ISortBy } from "@patternfly/react-table";
import { DELETE_ADDRESS_SPACE } from "graphql-module/queries";
import { compareObject } from "utils";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { TablePagination, IProjectCount, ProjectHeaderCard } from "components";
import { useMutationQuery } from "hooks";
import { IProject } from "./components";
import {
  getDetailForDeleteDialog,
  getHeaderForDeleteDialog
} from "modules/address-space";
import { ProjectListContainer, ProjectToolbarContainer } from "./containers";
import { initialiseFilterForProject } from "./utils";

export interface ISelectSearchOption {
  value: string;
  isExact: boolean;
}
export interface IProjectFilter {
  filterType: string;
  names: ISelectSearchOption[];
  namespaces: ISelectSearchOption[];
  type?: string;
  status?: string;
  projectType?: string;
}
export default function ProjectPage() {
  const { dispatch } = useStoreContext();
  useDocumentTitle("Project List");
  useA11yRouteChange();

  const [filter, setFilter] = useState<IProjectFilter>(
    initialiseFilterForProject()
  );
  const [totalProjects, setTotalProjects] = useState<number>(0);
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [selectedProjects, setSelectedProjects] = useState<IProject[]>([]);
  const [isAllSelected, setIsAllSelected] = useState<boolean>(false);
  const refetchQueries: string[] = ["all_address_spaces"];
  const [setDeleteProjectQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    refetchQueries
  );

  const onDeleteAll = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_ADDRESS_SPACE,
      modalProps: {
        onConfirm: onConfirmDeleteAll,
        selectedItems: selectedProjects.map(as => as.name),
        option: "Delete",
        detail: getDetailForDeleteDialog(selectedProjects),
        header: getHeaderForDeleteDialog(selectedProjects)
      }
    });
  };

  const onConfirmDeleteAll = async () => {
    if (selectedProjects && selectedProjects.length > 0) {
      let queryVariables: Array<{ name: string; namespace: string }> = [];
      selectedProjects.forEach(
        (project: IProject) =>
          project.name &&
          project.namespace &&
          queryVariables.push({
            name: project.name,
            namespace: project.namespace
          })
      );
      if (queryVariables.length > 0) {
        const queryVariable = {
          as: queryVariables
        };
        await setDeleteProjectQueryVariables(queryVariable);
      }
      setSelectedProjects([]);
    }
  };

  const onSelectProject = (
    data: IProject,
    isSelected: boolean,
    isAllSelected?: boolean
  ) => {
    if (!isSelected) {
      setIsAllSelected(false);
    }
    if (isSelected === true && selectedProjects.indexOf(data) === -1) {
      setSelectedProjects(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedProjects(prevState =>
        prevState.filter(
          project =>
            !compareObject(
              {
                name: project.name,
                nameSpace: project.namespace
              },
              {
                name: data.name,
                nameSpace: data.namespace
              }
            )
        )
      );
    }
    if (isAllSelected) {
      setIsAllSelected(true);
    }
  };

  const onSelectAllProject = (isSelected: boolean) => {
    if (isSelected === true) {
      setIsAllSelected(true);
    } else if (isSelected === false) {
      setIsAllSelected(false);
      setSelectedProjects([]);
    }
  };

  const selectAllProjects = (projects: IProject[]) => {
    setSelectedProjects(projects);
  };

  const isDeleteAllOptionDisabled = () => {
    if (selectedProjects && selectedProjects.length > 0) {
      return false;
    }
    return true;
  };

  const renderPagination = () => {
    return (
      <TablePagination
        itemCount={totalProjects}
        variant={"top"}
        page={page}
        perPage={perPage}
      />
    );
  };

  const ioTCount: IProjectCount = {
    total: 13,
    failed: 0,
    active: 0,
    pending: 0,
    configuring: 0
  };
  const msgCount: IProjectCount = {
    total: 12,
    failed: 1,
    active: 8,
    pending: 2,
    configuring: 1
  };

  return (
    <>
      <Page>
        <PageSection>
          <ProjectHeaderCard
            totalProject={totalProjects}
            ioTCount={ioTCount}
            msgCount={msgCount}
          />
        </PageSection>
        <PageSection variant={PageSectionVariants.light}>
          <Grid>
            <GridItem span={7}>
              <ProjectToolbarContainer
                filter={filter}
                setFilter={setFilter}
                totalProjects={totalProjects}
                sortValue={sortDropDownValue}
                setSortValue={setSortDropdownValue}
                onDeleteAll={onDeleteAll}
                isDeleteAllDisabled={isDeleteAllOptionDisabled()}
                onSelectAllProjects={onSelectAllProject}
                isAllProjectSelected={isAllSelected}
              />
            </GridItem>
            <GridItem span={5}>{renderPagination()}</GridItem>
          </Grid>
          <Divider />
          <ProjectListContainer
            page={page}
            perPage={perPage}
            setTotalProjects={setTotalProjects}
            filter={filter}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            selectedProjects={selectedProjects}
            onSelectProject={onSelectProject}
            selectAllProjects={selectAllProjects}
            isAllProjectSelected={isAllSelected}
          />
          {renderPagination()}
        </PageSection>
      </Page>
    </>
  );
}

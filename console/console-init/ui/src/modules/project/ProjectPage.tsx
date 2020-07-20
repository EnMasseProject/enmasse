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
  Page,
  Divider
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import {
  DELETE_ADDRESS_SPACE,
  DELETE_IOT_PROJECT
} from "graphql-module/queries";
import { compareObject } from "utils";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { TablePagination } from "components";
import { IProjectCount, ProjectHeaderCard, IProject } from "./components";
import { useMutationQuery } from "hooks";
import {
  initialiseFilterForProject,
  setInitialProjcetCount,
  ProjectType,
  getDetailForDeleteDialog,
  getHeaderForDeleteDialog
} from "./utils";
import { ProjectToolbarContainer, ProjectListContainer } from "./containers";

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
  const [msgCount, setMsgCount] = useState<IProjectCount>(
    setInitialProjcetCount()
  );
  const [ioTCount, setIoTCount] = useState<IProjectCount>(
    setInitialProjcetCount()
  );
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [selectedProjects, setSelectedProjects] = useState<IProject[]>([]);
  const [isAllSelected, setIsAllSelected] = useState<boolean>(false);

  const resetFormState = () => {
    setIsAllSelected(false);
    setSelectedProjects([]);
  };

  const refetchQueries: string[] = ["all_address_spaces"];
  const [setDeleteProjectQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    refetchQueries,
    undefined,
    resetFormState
  );

  const [setDeleteIoTProjectQueryVariables] = useMutationQuery(
    DELETE_IOT_PROJECT,
    ["allProjects"],
    undefined,
    resetFormState
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
        header: getHeaderForDeleteDialog(selectedProjects),
        iconType: "danger",
        confirmButtonLabel: "Delete"
      }
    });
  };

  const onConfirmDeleteAll = async () => {
    /**
     * Todo: projectType is temporary check for demo. it will remove later;
     */
    const projectType = "iot";
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
        if (projectType === "iot") {
          await setDeleteIoTProjectQueryVariables(queryVariable);
        } else {
          await setDeleteProjectQueryVariables(queryVariable);
        }
      }
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
        id="project-page-table-pagination"
        itemCount={totalProjects}
        variant={"top"}
        page={page}
        perPage={perPage}
      />
    );
  };

  const setCount = (count: IProjectCount, type: ProjectType) => {
    if (type === ProjectType.IOT_PROJECT && !compareObject(ioTCount, count)) {
      setIoTCount(count);
    } else if (
      type === ProjectType.MESSAGING_PROJECT &&
      !compareObject(msgCount, count)
    ) {
      setMsgCount(count);
    }
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
            setCount={setCount}
          />
          {renderPagination()}
        </PageSection>
      </Page>
    </>
  );
}

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import { ISortBy, SortByDirection } from "@patternfly/react-table";
import { useQuery } from "@apollo/react-hooks";
import { compareObject } from "utils";
import {
  IProject,
  ProjectList,
  IProjectCount
} from "modules/project/components";
import { IProjectFilter } from "modules/project/ProjectPage";
import {
  ProjectTypes,
  StatusTypes,
  ProjectType,
  getFilteredProjectsCount
} from "modules/project/utils";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import {
  DELETE_ADDRESS_SPACE,
  RETURN_IOT_PROJECTS,
  DELETE_IOT_PROJECT
} from "graphql-module";
import { IAddressSpace } from "modules/address-space";
import { IIoTProjectsResponse } from "schema/iot_project";

export interface IProjectListContainerProps {
  page: number;
  perPage: number;
  setTotalProjects: (value: number) => void;
  filter: IProjectFilter;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onSelectProject: (
    data: IProject,
    isSelected: boolean,
    isAllSelected?: boolean
  ) => void;
  selectedProjects: IProject[];
  isAllProjectSelected: boolean;
  selectAllProjects: (projects: IProject[]) => void;
  setCount: (count: IProjectCount, type: ProjectType) => void;
}

export const ProjectListContainer: React.FC<IProjectListContainerProps> = ({
  page,
  perPage,
  setTotalProjects,
  filter,
  sortValue,
  setSortValue,
  onSelectProject,
  selectedProjects,
  isAllProjectSelected,
  selectAllProjects,
  setCount
}) => {
  useDocumentTitle("Addressspace List");
  useA11yRouteChange();
  const { dispatch } = useStoreContext();
  const [sortBy, setSortBy] = useState<ISortBy>();
  const refetchQueries: string[] = ["all_address_spaces"];
  const [setDeleteProjectQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    refetchQueries
  );

  // const { loading, data } = useQuery<IProjectsResponse>(
  //   RETURN_ALL_ADDRESS_SPACES(
  //     page,
  //     perPage,
  //     filterNames,
  //     filterNamespaces,
  //     filterType,
  //     sortBy
  //   ),
  //   { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  // );

  const [
    setDeleteIoTProjectQueryVariables
  ] = useMutationQuery(DELETE_IOT_PROJECT, ["allProjects"]);

  const queryResolver = `
  total
  iotProjects {
    metadata {
      name
      namespace
      creationTimestamp
    }
    enabled    
    status {
      phase
    }
  }`;

  const { loading, data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS(undefined, queryResolver)
  );

  if (loading) {
    return <Loading />;
  }

  const { allProjects } = data || {
    allProjects: { total: 0, iotProjects: [] }
  };

  const projects = allProjects?.iotProjects || [];

  setTotalProjects(allProjects?.total || 0);

  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }

  const onChangeEdit = (project: IProject) => {
    const {
      name,
      namespace,
      type,
      isReady,
      authService,
      plan,
      displayName,
      status
    } = project;
    if (
      name &&
      namespace &&
      type &&
      isReady !== undefined &&
      authService &&
      plan &&
      displayName &&
      status
    ) {
      const addressSpace: IAddressSpace = {
        name: name,
        nameSpace: namespace,
        type: type,
        isReady: false,
        authenticationService: authService,
        creationTimestamp: "",
        planValue: plan,
        displayName: displayName,
        messages: [],
        phase: status
      };
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.EDIT_ADDRESS_SPACE,
        modalProps: {
          addressSpace
        }
      });
    }
  };

  const onDeleteProject = (project: IProject) => {
    if (project && project.name && project.namespace) {
      if (project.projectType === ProjectTypes.MESSAGING) {
        const queryVariable = {
          as: [
            {
              name: project.name,
              namespace: project.namespace
            }
          ]
        };
        setDeleteProjectQueryVariables(queryVariable);
      }
      const queryVariable = {
        as: {
          name: project.name,
          namespace: project.namespace
        }
      };
      setDeleteIoTProjectQueryVariables(queryVariable);
    }
  };

  const onChangeDelete = (project: IProject) => {
    if (project.projectType === ProjectTypes.MESSAGING) {
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.DELETE_ADDRESS_SPACE,
        modalProps: {
          selectedItems: [project.name],
          data: project,
          onConfirm: onDeleteProject,
          option: "Delete",
          detail: `Are you sure you want to delete this messaging project: ${project.name} ?`,
          header: "Delete this Messaging Project ?",
          confirmButtonLabel: "Delete",
          iconType: "danger"
        }
      });
    } else if (project.projectType === ProjectTypes.IOT) {
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.DELETE_PROJECT,
        modalProps: {
          selectedItems: [project.name],
          data: project,
          onConfirm: onDeleteProject,
          option: "Delete",
          detail: `Are you sure you want to delete this iot project: ${project.name} ?`,
          header: "Delete this IoT Project ?",
          confirmButtonLabel: "Delete",
          iconType: "danger"
        }
      });
    }
  };

  //Download the certificate function
  const onDownloadCertificate = async (project: IProject) => {
    // const dataToDownload = await client.query({
    //   query: DOWNLOAD_CERTIFICATE,
    //   variables: {
    //     as: {
    //       name: data.name,
    //       namespace: data.namespace,
    //     },
    //   },
    //   fetchPolicy: FetchPolicy.NETWORK_ONLY,
    // });
    // if (dataToDownload.errors) {
    //   console.log("Error while download", dataToDownload.errors);
    // }
    // const url = window.URL.createObjectURL(
    //   new Blob([dataToDownload.data.messagingCertificateChain])
    // );
    // const link = document.createElement("a");
    // link.href = url;
    // link.setAttribute("download", `${data.name}.crt`);
    // document.body.appendChild(link);
    // link.click();
    // if (link.parentNode) link.parentNode.removeChild(link);
  };

  const onEnable = (project: IProject) => {
    console.log("enable the project", project);
  };

  const onDisable = (project: IProject) => {
    console.log("disable the project", project);
  };

  const getProjects = () => {
    return projects?.map((project: any) => {
      const { metadata, status } = project || {};
      return {
        projectType: ProjectTypes.IOT,
        name: metadata?.name,
        displayName: metadata?.name,
        type: "",
        namespace: metadata?.namespace,
        plan: "",
        status: status?.phase,
        authService: "none",
        isReady: true,
        creationTimestamp: metadata?.creationTimestamp,
        errorMessageRate: 3,
        addressCount: 15,
        connectionCount: 3
      };
    });
  };

  const projectList: IProject[] = getProjects();

  setTotalProjects(projectList.length);

  const ioTCount: IProjectCount = {
    total: getFilteredProjectsCount(ProjectTypes.IOT, projectList),
    failed: getFilteredProjectsCount(
      ProjectTypes.IOT,
      projectList,
      StatusTypes.FAILED
    ),
    active: getFilteredProjectsCount(
      ProjectTypes.IOT,
      projectList,
      StatusTypes.ACTIVE
    ),
    pending: getFilteredProjectsCount(
      ProjectTypes.IOT,
      projectList,
      StatusTypes.PENDING
    ),
    configuring: getFilteredProjectsCount(
      ProjectTypes.IOT,
      projectList,
      StatusTypes.CONFIGURING
    )
  };

  const msgCount: IProjectCount = {
    total: getFilteredProjectsCount(ProjectTypes.MESSAGING, projectList),
    failed: getFilteredProjectsCount(
      ProjectTypes.MESSAGING,
      projectList,
      StatusTypes.FAILED
    ),
    active: getFilteredProjectsCount(
      ProjectTypes.MESSAGING,
      projectList,
      StatusTypes.ACTIVE
    ),
    pending: getFilteredProjectsCount(
      ProjectTypes.MESSAGING,
      projectList,
      StatusTypes.PENDING
    ),
    configuring: getFilteredProjectsCount(
      ProjectTypes.MESSAGING,
      projectList,
      StatusTypes.CONFIGURING
    )
  };

  setCount(ioTCount, ProjectType.IOT_PROJECT);
  setCount(msgCount, ProjectType.MESSAGING_PROJECT);

  const onSort = (_event: any, index: number, direction: SortByDirection) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  if (isAllProjectSelected && selectedProjects.length !== projectList.length) {
    selectAllProjects(projectList);
  }

  const onSelect = (project: IProject, isSelected: boolean) => {
    if (!isAllProjectSelected && isSelected) {
      if (selectedProjects.length === projectList.length - 1) {
        let allSelected = true;
        for (let prj of projectList) {
          for (let selectedProject of selectedProjects) {
            if (compareObject(prj.name, selectedProject.name)) {
              if (project.name === prj.name) {
                allSelected = true;
              } else if (!prj.selected) allSelected = false;
              break;
            }
          }
        }
        if (allSelected) {
          onSelectProject(project, isSelected, true);
        }
      }
    }
    onSelectProject(project, isSelected);
  };

  //TODO: logic will be removed after implementation of query
  for (let project of projectList) {
    project.selected =
      selectedProjects.filter(({ name, namespace }) => {
        const areProjectsEqual = compareObject(
          { name, namespace },
          {
            name: project.name,
            namespace: project.namespace
          }
        );
        return areProjectsEqual;
      }).length === 1;
  }

  return (
    <>
      <ProjectList
        onSort={onSort}
        projects={projectList}
        sortBy={sortBy}
        onEdit={onChangeEdit}
        onDelete={onChangeDelete}
        onDownload={onDownloadCertificate}
        onEnable={onEnable}
        onDisable={onDisable}
        onSelectProject={onSelect}
      />
    </>
  );
};

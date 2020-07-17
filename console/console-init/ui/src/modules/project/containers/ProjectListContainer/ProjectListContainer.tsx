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
  DELETE_IOT_PROJECT,
  TOGGLE_IOT_PROJECTS_STATUS,
  RETURN_ALL_PROJECTS
} from "graphql-module";
import { IAddressSpace } from "modules/address-space";
import { IAllProjectsResponse } from "schema/iot_project";
import { POLL_INTERVAL, FetchPolicy } from "constant";

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
  const refetchQueries: string[] = ["allProjects"];
  const [setDeleteProjectQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    refetchQueries
  );
  const [
    setToggleIoTProjectQueryVariables
  ] = useMutationQuery(TOGGLE_IOT_PROJECTS_STATUS, ["allProjects"]);
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
    objects{
      ... on IoTProject_iot_enmasse_io_v1alpha1 {
        kind
        metadata {
          name
          namespace
          creationTimestamp
        }
        iotStatus: status{
          phase
          phaseReason 
        }
        enabled
      }
    }`;

  const { loading, data } = useQuery<IAllProjectsResponse>(
    RETURN_ALL_PROJECTS(undefined, queryResolver),
    {
      fetchPolicy: FetchPolicy.NETWORK_ONLY,
      pollInterval: POLL_INTERVAL
    }
  );

  if (loading) {
    return <Loading />;
  }

  const { allProjects } = data || {
    allProjects: { total: 0, objects: [] }
  };

  const projects = allProjects?.objects || [];

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
      const queryVariable = {
        as: [
          {
            name: project.name,
            namespace: project.namespace
          }
        ]
      };
      if (project.projectType === ProjectTypes.MESSAGING) {
        setDeleteProjectQueryVariables(queryVariable);
      }
      setDeleteIoTProjectQueryVariables(queryVariable);
    }
  };

  const onChangeDelete = (project: IProject) => {
    if (project.projectType === ProjectTypes.MESSAGING) {
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.DELETE_PROJECT,
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

  const handleOnChangeEnable = (project: IProject) => {
    const queryVariable = {
      a: [
        {
          name: project.name,
          namespace: project.namespace
        }
      ],
      status: !project.isEnabled
    };
    setToggleIoTProjectQueryVariables(queryVariable);
  };

  const onEnable = (project: IProject) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_PROJECT,
      modalProps: {
        selectedItems: [project.name],
        data: project,
        onConfirm: () => handleOnChangeEnable(project),
        option: "Enable",
        detail: `Are you sure you want to enable this iot project: ${project.name} ?`,
        header: "Enable this IoT Project ?",
        confirmButtonLabel: "Enable",
        iconType: "danger"
      }
    });
  };

  const onDisable = (project: IProject) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_PROJECT,
      modalProps: {
        selectedItems: [project.name],
        data: project,
        onConfirm: () => handleOnChangeEnable(project),
        option: "Disable",
        detail: `Are you sure you want to disable this iot project: ${project.name} ?`,
        header: "Disable this IoT Project ?",
        confirmButtonLabel: "Disable",
        iconType: "danger"
      }
    });
  };

  const getProjects = () => {
    return projects?.map((project: any) => {
      if (project.kind === "IoTProject") {
        const { metadata, iotStatus, enabled } = project || {};
        return {
          projectType: ProjectTypes.IOT,
          name: metadata?.name,
          displayName: metadata?.name,
          isEnabled: enabled,
          namespace: metadata?.namespace,
          status: iotStatus?.phase,
          creationTimestamp: metadata?.creationTimestamp,
          errorMessageRate: 3
        };
      } else return { projectType: ProjectTypes.MESSAGING };
      // return {
      //   projectType: ProjectTypes.IOT,
      //   name: metadata?.name,
      //   displayName: metadata?.name,
      //   isEnabled: enabled,
      //   type: "",
      //   namespace: metadata?.namespace,
      //   plan: "",
      //   status: status?.phase,
      //   authService: "none",
      //   isReady: true,
      //   creationTimestamp: metadata?.creationTimestamp,
      //   errorMessageRate: 3,
      //   addressCount: 15,
      //   connectionCount: 3
      // };
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

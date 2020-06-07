/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useA11yRouteChange, useDocumentTitle } from "use-patternfly";
import { ISortBy, SortByDirection } from "@patternfly/react-table";
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
  // const client = useApolloClient();
  // const { dispatch } = useStoreContext();
  const [sortBy, setSortBy] = useState<ISortBy>();
  // const refetchQueries: string[] = ["all_address_spaces"];
  // const [setDeleteProjectQueryVariables] = useMutationQuery(
  //   DELETE_ADDRESS_SPACE,
  //   refetchQueries
  // );

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

  // if (loading) {
  //   return <Loading />;
  // }

  const { projects } = {
    projects: { total: 0, projects: [] }
  };

  setTotalProjects(projects && projects.total);

  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }

  const onChangeEdit = (project: IProject) => {
    // dispatch({
    //   type: types.SHOW_MODAL,
    //   modalType: MODAL_TYPES.EDIT_ADDRESS_SPACE,
    //   modalProps: {
    //     project,
    //   },
    // });
  };

  const onDeleteProject = (project: IProject) => {
    if (project && project.name && project.namespace) {
      //   const queryVariable = {
      //     as: [
      //       {
      //         name: project.name,
      //         namespace: project.namespace,
      //       },
      //     ],
      //   };
      //   setDeleteProjectQueryVariables(queryVariable);
    }
  };

  const onChangeDelete = (project: IProject) => {
    // dispatch({
    //   type: types.SHOW_MODAL,
    //   modalType: MODAL_TYPES.DELETE_ADDRESS_SPACE,
    //   modalProps: {
    //     selectedItems: [project.name],
    //     data: project,
    //     onConfirm: onDeleteProject,
    //     option: "Delete",
    //     detail: `Are you sure you want to delete this addressspace: ${project.name} ?`,
    //     header: "Delete this Address Space ?",
    //   },
    // });
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

  const projectList: IProject[] = [
    {
      projectType: ProjectTypes.MESSAGING,
      name: "namespace_test1.new_space1",
      displayName: "new_space",
      namespace: "namespace_test1",
      plan: "Standard",
      status: StatusTypes.FAILED,
      creationTimestamp: "2020-01-20T11:44:28.607Z",
      errorMessageRate: 3,
      addressCount: 15,
      connectionCount: 3
    },
    {
      projectType: ProjectTypes.IOT,
      name: "devops_jbosstest1.k8s_iot1",
      displayName: "k8s_iot",
      namespace: "devops_jbosstest1",
      status: StatusTypes.ACTIVE,
      creationTimestamp: "2020-05-21T14:40:28.607Z",
      errorMessageRate: 25,
      deviceCount: 10500,
      activeCount: 7100
    },
    {
      projectType: ProjectTypes.MESSAGING,
      name: "namespace_test1.new_space2",
      displayName: "new_space",
      namespace: "namespace_test1",
      plan: "Brokered",
      status: StatusTypes.PENDING,
      creationTimestamp: "2020-01-20T05:44:28.607Z",
      addressCount: 27,
      connectionCount: 3
    },
    {
      projectType: ProjectTypes.IOT,
      name: "namespace_test1.k8s_iot2",
      displayName: "k8s_iot",
      namespace: "namespace_test1",
      status: StatusTypes.ACTIVE,
      creationTimestamp: "2020-01-20T05:44:28.607Z",
      addressCount: 27,
      connectionCount: 3,
      errorMessages: ["error message", "pod is not ready."]
    },
    {
      projectType: ProjectTypes.MESSAGING,
      name: "namespace_test1.new_space3",
      displayName: "new_space",
      namespace: "namespace_test1",
      plan: "Brokered",
      status: StatusTypes.FAILED,
      creationTimestamp: "2020-05-21T08:44:28.607Z",
      addressCount: 27,
      connectionCount: 3,
      errorMessageRate: 98,
      errorMessages: ["error message", "issue with operator"]
    },
    {
      projectType: ProjectTypes.MESSAGING,
      name: "namespace_test1.new_space4",
      displayName: "new_space",
      namespace: "namespace_test1",
      plan: "Brokered",
      status: StatusTypes.CONFIGURING,
      creationTimestamp: "2020-05-21T08:44:28.607Z",
      addressCount: 27,
      connectionCount: 3,
      errorMessageRate: 98,
      errorMessages: ["error message", "issue with operator"]
    }
  ];

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
              } else if (prj.selected === false) allSelected = false;
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
        onSelectProject={onSelect}
      />
    </>
  );
};

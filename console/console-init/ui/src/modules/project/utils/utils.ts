/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import {
  IMessagingProject,
  IIoTProjectInput,
  IProjectCount,
  IProject
} from "modules/project";
import { IProjectFilter } from "modules/project/ProjectPage";
import { ISelectOption } from "utils";
import {
  ProjectTypes,
  StatusTypes,
  TlsCertificateType,
  EndPointProtocol
} from "./constant";
import { IExposeMessagingProject, IExposeEndPoint } from "../dailogs";

enum ProjectType {
  IOT_PROJECT = "IoT",
  MESSAGING_PROJECT = "Messaging"
}

const sortMenuItems = [
  { key: "name", value: "Name", index: 1 },
  { key: "type/plan", value: "Type/Plan", index: 2 },
  { key: "status", value: "Status", index: 3 },
  { key: "creationTimestamp", value: "Time Created", index: 4 }
];

const filterMenuItems = [
  { key: "name", value: "Name", id: "dropdown-filter-name" },
  { key: "namespace", value: "Namespace", id: "dropdown-filter-namespace" },
  { key: "type", value: "Type", id: "dropdown-filter-type" }
];

const typeOptions: ISelectOption[] = [
  {
    key: "iot",
    value: "IoTProject",
    label: "IoT Project",
    isDisabled: false
  },
  {
    key: "messaging",
    value: "AddressSpace",
    label: "Messaging Project",
    isDisabled: false
  }
];

const isIoTProjectValid = (projectDetail?: IIoTProjectInput) => {
  if (
    projectDetail &&
    projectDetail.iotProjectName &&
    projectDetail.iotProjectName.trim() !== "" &&
    projectDetail.namespace &&
    projectDetail.namespace.trim() !== "" &&
    projectDetail.isNameValid
  ) {
    return true;
  }
  return false;
};

const initialiseFilterForProject = () => {
  const filter: IProjectFilter = {
    filterType: "Name",
    names: [],
    namespaces: []
  };
  return filter;
};

const setInitialProjcetCount = () => {
  const count: IProjectCount = {
    total: 0,
    active: 0,
    pending: 0,
    configuring: 0,
    failed: 0
  };
  return count;
};
const getFilteredProjectsCount = (
  type: ProjectTypes,
  projectList: IProject[],
  status?: StatusTypes
) => {
  let list: IProject[] = [];
  if (!status) {
    list = projectList.filter(project => project.projectType === type);
  } else {
    list = projectList.filter(
      project => project.projectType === type && project.status === status
    );
  }
  return list.length;
};

const getDetailForDeleteDialog = (selectedItems: IProject[]) => {
  const iotProjects = getFilteredProjectsCount(ProjectTypes.IOT, selectedItems);
  const msgProjects = getFilteredProjectsCount(
    ProjectTypes.MESSAGING,
    selectedItems
  );
  let projectTypeValue = "";
  if (iotProjects > 0 && msgProjects > 0) {
    projectTypeValue = "IoT and Messaging";
  } else if (iotProjects === 0 && msgProjects > 0) {
    projectTypeValue = "Messaging";
  } else {
    projectTypeValue = "IoT";
  }
  const detail =
    selectedItems.length > 1
      ? `Are you sure you want to delete all of these ${projectTypeValue} projects: ${selectedItems.map(
          as => " " + as.name
        )} ?`
      : `Are you sure you want to delete this ${projectTypeValue} project: ${selectedItems[0].name} ?`;
  return detail;
};

const getHeaderForDeleteDialog = (selectedItems: any[]) => {
  const header =
    selectedItems.length > 1
      ? "Delete these Projects ?"
      : "Delete this Project ?";
  return header;
};

const isMessagingProjectConfigurationValid = (
  messagingProject: IMessagingProject
) => {
  if (messagingProject) {
    const { name, namespace, type, plan, authService } = messagingProject;
    if (
      name &&
      name.trim() !== "" &&
      namespace &&
      namespace.trim() !== "" &&
      type &&
      type.trim() !== "" &&
      plan &&
      plan.trim() !== "" &&
      authService &&
      authService.trim() !== ""
    ) {
      return true;
    }
    return false;
  }
};

const isEnabledCertificateStep = (messagingProject: IMessagingProject) => {
  if (
    messagingProject.customizeEndpoint === true &&
    (messagingProject.tlsCertificate !== TlsCertificateType.UPLOAD_CERT ||
      (messagingProject.privateKey &&
        messagingProject.privateKey.trim() !== "" &&
        messagingProject.certValue &&
        messagingProject.certValue?.trim() !== ""))
  ) {
    return true;
  }
  return false;
};

const isMessagingProjectValid = (messagingProject: IMessagingProject) => {
  if (
    isMessagingProjectConfigurationValid(messagingProject) &&
    (messagingProject.customizeEndpoint === false ||
      (messagingProject.protocols !== undefined &&
        messagingProject.protocols.length > 0 &&
        messagingProject.tlsCertificate !== undefined &&
        messagingProject.tlsCertificate.trim() !== ""))
  ) {
    return true;
  }
  return false;
};

const isRouteStepValid = (messagingProject: IMessagingProject) => {
  const { routesConf } = messagingProject;
  if (routesConf && routesConf.length > 0) {
    let isValid = true;
    routesConf.forEach(route => {
      if (!route.tlsTermination || route.tlsTermination.trim() === "") {
        isValid = false;
      }
    });
    return isValid;
  } else {
    return false;
  }
};

const getQueryVariableForCreateMessagingProject = (
  messagingProject: IMessagingProject
) => {
  const {
    name,
    namespace,
    type,
    plan,
    authService,
    customizeEndpoint,
    tlsCertificate,
    certValue,
    privateKey,
    protocols,
    addRoutes,
    routesConf
  } = messagingProject;
  const queryVariables: IExposeMessagingProject = {
    as: {
      metadata: {
        name: name,
        namespace: namespace
      },
      spec: {
        type: type?.toLowerCase(),
        plan: plan?.toLowerCase(),
        authenticationService: {
          name: authService
        }
      }
    }
  };
  if (customizeEndpoint) {
    const endpoints: IExposeEndPoint[] = [];
    if (protocols && protocols.length > 0) {
      protocols.map((protocol: string) => {
        const endpoint: IExposeEndPoint = { service: "messaging" };
        if (protocol === EndPointProtocol.AMQPS) {
          endpoint.name = "messaging";
        } else if (protocol === EndPointProtocol.AMQP_WSS) {
          endpoint.name = "messaging-wss";
        }
        if (tlsCertificate) {
          endpoint.certificate = {
            provider: tlsCertificate
          };
          if (
            tlsCertificate === TlsCertificateType.UPLOAD_CERT &&
            certValue &&
            certValue.trim() !== "" &&
            privateKey &&
            privateKey.trim() !== ""
          ) {
            endpoint.certificate = {
              ...endpoint.certificate,
              tlsKey: btoa(privateKey?.trim()),
              tlsCert: btoa(certValue?.trim())
            };
          }
        }
        if (addRoutes) {
          endpoint.expose = { type: "route", routeServicePort: protocol };
          const routeConf = routesConf?.filter(
            conf => conf.protocol === protocol
          );
          if (routeConf && routeConf.length > 0) {
            if (routeConf[0].hostname && routeConf[0].hostname.trim() !== "") {
              endpoint.expose.routeHost = routeConf[0].hostname.trim();
            }
            if (
              routeConf[0].tlsTermination &&
              routeConf[0].tlsTermination.trim() !== ""
            ) {
              endpoint.expose.routeTlsTermination = routeConf[0].tlsTermination;
            }
          }
        }
        endpoints.push(endpoint);
        return endpoint;
      });
    }
    Object.assign(queryVariables.as.spec, { endpoints: endpoints });
  }
  return queryVariables;
};

export {
  isMessagingProjectValid,
  isIoTProjectValid,
  initialiseFilterForProject,
  sortMenuItems,
  typeOptions,
  filterMenuItems,
  ProjectType,
  setInitialProjcetCount,
  getFilteredProjectsCount,
  getDetailForDeleteDialog,
  getHeaderForDeleteDialog,
  isEnabledCertificateStep,
  isRouteStepValid,
  isMessagingProjectConfigurationValid,
  getQueryVariableForCreateMessagingProject
};

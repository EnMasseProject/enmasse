/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import {
  IMessagingProject,
  IExposeMessagingProject,
  IExposeEndPoint
} from "modules/address-space/dialogs/CreateMessagingProject";
import { TlsCertificateType, EndPointProtocol } from "./constant";

const getDetailForDeleteDialog = (selectedItems: any[]) => {
  const detail =
    selectedItems.length > 1
      ? `Are you sure you want to delete all of these address spaces: ${selectedItems.map(
          as => " " + as.name
        )} ?`
      : `Are you sure you want to delete this address space: ${selectedItems[0].name} ?`;
  return detail;
};

const getHeaderForDeleteDialog = (selectedItems: any[]) => {
  const header =
    selectedItems.length > 1
      ? "Delete these Address Spaces ?"
      : "Delete this Address Space ?";
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
      (messagingProject.protocols &&
        messagingProject.protocols.length > 0 &&
        messagingProject.tlsCertificate &&
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

const getQueryVariableForCreateAddressSpace = (
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
  getDetailForDeleteDialog,
  getHeaderForDeleteDialog,
  isMessagingProjectValid,
  isMessagingProjectConfigurationValid,
  isEnabledCertificateStep,
  isRouteStepValid,
  getQueryVariableForCreateAddressSpace
};

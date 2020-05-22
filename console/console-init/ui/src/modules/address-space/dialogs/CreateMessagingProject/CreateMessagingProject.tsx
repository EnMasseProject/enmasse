/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Wizard } from "@patternfly/react-core";
import { MessagingProjectConfiguration } from "./MessagingProjectConfiguration";
import { useMutationQuery } from "hooks";
import {
  CREATE_ADDRESS_SPACE,
  RETURN_ADDRESS_SPACE_SCHEMAS
} from "graphql-module";
import { MessagingProjectReview } from "./MessagingProjectReview";
import { useStoreContext, types } from "context-state-reducer";
import {
  isMessagingProjectValid,
  isMessagingProjectConfigurationValid,
  isRouteStepValid,
  isEnabledCertificateStep,
  TlsCertificateType,
  EndPointProtocol
} from "modules/address-space/utils";
import { ConfiguringCertificates } from "./ConfiguringCertificates";
import { ConfiguringRoutes } from "./ConfiguringRoutes";
import { EndpointConfiguration } from "modules/address-space/components";
import { IAddressSpaceSchema } from "schema/ResponseTypes";
import { useQuery } from "@apollo/react-hooks";

export interface IMessagingProject {
  namespace: string;
  name: string;
  type?: string;
  plan?: string;
  authService?: string;
  customizeEndpoint: boolean;
  isNameValid: boolean;
  certValue?: string;
  addCertificate: boolean;
  tlsCertificate?: string;
  protocols?: string[];
  privateKey?: string;
  hostname?: string;
  tlsTermination?: string;
  addRoutes: boolean;
}

interface IExposeEndPoint {
  name?: string;
  service?: string;
  certificate?: {
    provider: string;
    tlsKey?: string;
    tlsCert?: string;
  };
  expose?: IExposeRoute;
}
interface IExposeRoute {
  routeHost?: string;
  type?: string;
  routeServicePort?: string;
  routeTlsTermination?: string;
}
interface IExposeMessagingProject {
  as: {
    metadata: {
      name: string;
      namespace: string;
    };
    spec: {
      type?: string;
      plan?: string;
      authenticationService: {
        name?: string;
      };
      endpoints?: IExposeEndPoint[];
    };
  };
}

interface ICreateMessagingProjectProps {}

const initialMessageProject: IMessagingProject = {
  name: "",
  namespace: "",
  type: "",
  isNameValid: true,
  addCertificate: false,
  customizeEndpoint: false,
  addRoutes: false
};

const CreateMessagingProject: React.FunctionComponent<ICreateMessagingProjectProps> = () => {
  const [messagingProject, setMessagingProject] = useState<IMessagingProject>(
    initialMessageProject
  );
  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose } = modalProps || {};
  const resetFormState = () => {
    setMessagingProject(initialMessageProject);
  };

  const refetchQueries: string[] = ["all_address_spaces"];
  const [setQueryVariables] = useMutationQuery(
    CREATE_ADDRESS_SPACE,
    refetchQueries,
    resetFormState,
    resetFormState
  );

  const { data: addressSpaceSchema } = useQuery<IAddressSpaceSchema>(
    RETURN_ADDRESS_SPACE_SCHEMAS
  ) || { data: { addressSpaceSchema: [] } };

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const handleSave = async () => {
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
      tlsTermination,
      protocols,
      addRoutes,
      hostname
    } = messagingProject;
    if (isMessagingProjectValid(messagingProject)) {
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
                  tlsKey: privateKey?.trim(),
                  tlsCert: certValue?.trim()
                };
              }
            }
            if (addRoutes) {
              endpoint.expose = { type: "route", routeServicePort: protocol };
              if (hostname && hostname.trim() !== "") {
                endpoint.expose.routeHost = hostname.trim();
              }
              if (tlsTermination && tlsTermination.trim() !== "") {
                endpoint.expose.routeTlsTermination = tlsTermination;
              }
            }
            endpoints.push(endpoint);
            return endpoint;
          });
        }
        Object.assign(queryVariables.as.spec, { endpoints: endpoints });
      }
      await setQueryVariables(queryVariables);

      onCloseDialog();
      if (onConfirm) {
        onConfirm();
      }
    }
  };

  const messagingConfigurationStep = {
    name: "Configuration",
    component: (
      <MessagingProjectConfiguration
        projectDetail={messagingProject}
        addressSpaceSchema={addressSpaceSchema}
        setProjectDetail={setMessagingProject}
      />
    ),
    enableegxt: isMessagingProjectConfigurationValid(messagingProject)
  };
  const endpointConfiguringStep = {
    name: "Configuring",
    component: (
      <EndpointConfiguration
        setProjectDetail={setMessagingProject}
        addressSpaceSchema={addressSpaceSchema}
        projectDetail={messagingProject}
      />
    ),
    enableNext: isMessagingProjectValid(messagingProject)
  };
  const endpointCertificatesStep = {
    name: "Certificates",
    component: (
      <ConfiguringCertificates
        projectDetail={messagingProject}
        setProjectDetail={setMessagingProject}
      />
    ),
    enableNext:
      isMessagingProjectValid(messagingProject) &&
      isEnabledCertificateStep(messagingProject)
  };

  const endpointRoutesStep = {
    name: "Routes",
    component: (
      <ConfiguringRoutes
        projectDetail={messagingProject}
        addressSpaceSchema={addressSpaceSchema}
        setProjectDetail={setMessagingProject}
      />
    ),
    enableNext:
      isMessagingProjectValid(messagingProject) &&
      isRouteStepValid(messagingProject)
  };

  const endpointCustomizationStep = {
    name: "Endpoint customization",
    steps: [
      ...[endpointConfiguringStep],
      ...(messagingProject.addCertificate ? [endpointCertificatesStep] : []),
      ...(messagingProject.addRoutes ? [endpointRoutesStep] : [])
    ],
    enableNext: isMessagingProjectValid(messagingProject),
    canJumpTo: isMessagingProjectValid(messagingProject)
  };

  const messagingReviewStep = {
    name: "Review",
    component: <MessagingProjectReview projectDetail={messagingProject} />,
    enableNext: isMessagingProjectValid(messagingProject),
    canJumpTo: isMessagingProjectValid(messagingProject),
    nextButtonText: "Finish"
  };

  const steps = [
    messagingConfigurationStep,
    ...(messagingProject.customizeEndpoint ? [endpointCustomizationStep] : []),
    ...[messagingReviewStep]
  ];

  return (
    <Wizard
      id="create-as-wizard"
      isOpen={true}
      isFullHeight={true}
      isFullWidth={true}
      onClose={onCloseDialog}
      title="Create an Instance"
      steps={steps}
      onNext={() => {}}
      onSave={handleSave}
    />
  );
};

export { CreateMessagingProject };

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
  getQueryVariableForCreateAddressSpace
} from "modules/address-space/utils";
import { ConfiguringCertificates } from "./ConfiguringCertificates";
import { ConfiguringRoutes } from "./ConfiguringRoutes";
import { EndpointConfiguration } from "modules/address-space/components";
import { IAddressSpaceSchema } from "schema/ResponseTypes";
import { useQuery } from "@apollo/react-hooks";
export interface IRouteConf {
  protocol: string;
  hostname?: string;
  tlsTermination?: string;
}
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
  addRoutes: boolean;
  routesConf?: IRouteConf[];
}

export interface IExposeEndPoint {
  name?: string;
  service?: string;
  certificate?: {
    provider: string;
    tlsKey?: string;
    tlsCert?: string;
  };
  expose?: IExposeRoute;
}
export interface IExposeRoute {
  routeHost?: string;
  type?: string;
  routeServicePort?: string;
  routeTlsTermination?: string;
}
export interface IExposeMessagingProject {
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
    if (isMessagingProjectValid(messagingProject)) {
      await setQueryVariables(
        getQueryVariableForCreateAddressSpace(messagingProject)
      );

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
    enableNext: isMessagingProjectConfigurationValid(messagingProject)
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
      isEnabledCertificateStep(messagingProject) &&
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
      onClose={onCloseDialog}
      title="Create an Instance"
      steps={steps}
      onSave={handleSave}
    />
  );
};

export { CreateMessagingProject };

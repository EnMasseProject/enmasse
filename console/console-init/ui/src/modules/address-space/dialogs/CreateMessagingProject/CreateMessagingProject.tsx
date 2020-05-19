/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Wizard } from "@patternfly/react-core";
import { MessagingProjectConfiguration } from "./MessagingProjectConfiguration";
import { useMutationQuery } from "hooks";
import { CREATE_ADDRESS_SPACE } from "graphql-module";
import { MessagingProjectReview } from "./MessagingProjectReview";
import { useStoreContext, types } from "context-state-reducer";
import {
  isMessgaingProjectValid,
  isMessgaingProjectConfigurationValid
} from "modules/address-space/utils";
import { ConfiguringCertificates } from "./ConfiguringCertificates";
import { ConfiguringRoutes } from "./ConfiguringRoutes";
import { EndpointConfiguration } from "modules/address-space/components";

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
  protocols?: Array<string>;
  privateKey?: string;
  hostname?: string;
  tlsTermination?: string;
  addRoutes: boolean;
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

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const handleSave = async () => {
    const { name, namespace, type, plan, authService } = messagingProject;
    if (isMessgaingProjectValid(messagingProject)) {
      const variables = {
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
      await setQueryVariables(variables);

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
        setProjectDetail={setMessagingProject}
      />
    ),
    enableNext: isMessgaingProjectConfigurationValid(messagingProject)
  };
  const endpointConfiguringStep = {
    name: "Configuring",
    component: (
      <EndpointConfiguration
        setProjectDetail={setMessagingProject}
        projectDetail={messagingProject}
      />
    ),
    enableNext: isMessgaingProjectValid(messagingProject)
  };
  const endpointCertificatesStep = {
    name: "Certificates",
    component: (
      <ConfiguringCertificates
        projectDetail={messagingProject}
        setProjectDetail={setMessagingProject}
      />
    ),
    enableNext: isMessgaingProjectValid(messagingProject)
  };
  const endpointRoutesStep = {
    name: "Routes",
    component: (
      <ConfiguringRoutes
        projectDetail={messagingProject}
        setProjectDetail={setMessagingProject}
      />
    ),
    enableNext: isMessgaingProjectValid(messagingProject)
  };

  const endpointCustomizationStep = {
    name: "Endpoint customization",
    steps: [
      ...[endpointConfiguringStep],
      ...(messagingProject.addCertificate ? [endpointCertificatesStep] : []),
      ...(messagingProject.addRoutes ? [endpointRoutesStep] : [])
    ],
    enableNext: isMessgaingProjectValid(messagingProject),
    canJumpTo: isMessgaingProjectValid(messagingProject)
  };

  const messagingReviewStep = {
    name: "Review",
    component: <MessagingProjectReview projectDetail={messagingProject} />,
    enableNext: isMessgaingProjectValid(messagingProject),
    canJumpTo: isMessgaingProjectValid(messagingProject),
    nextButtonText: "Finish"
  };

  const steps = [
    messagingConfigurationStep,
    ...(messagingProject.customizeEndpoint ? [endpointCustomizationStep] : []),
    ...[messagingReviewStep]
  ];
  console.log(messagingProject);

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

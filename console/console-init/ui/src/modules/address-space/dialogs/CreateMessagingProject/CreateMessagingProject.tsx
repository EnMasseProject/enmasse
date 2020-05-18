/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { MessagingProjectConfiguration } from "./MessagingProjectConfiguration";

import { useMutationQuery } from "hooks";
import { CREATE_ADDRESS_SPACE } from "graphql-module";
import { MessagingProjectReview } from "./MessagingProjectReview";
import { Wizard } from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";
import { isMessgaingProjectValid } from "modules/address-space/utils";
import { ConfiguringEndpoint } from "./ConfiguringEndpoint";
import { ConfiguringCertificates } from "./ConfiguringCertificates";
import { ConfiguringRoutes } from "./ConfiguringRoutes";

export interface IMessagingProject {
  namespace: string;
  name: string;
  type?: string;
  plan?: string;
  authService?: string;
  customizeEndpoint: boolean;
  isNameValid: boolean;
}

interface ICreateMessagingProjectProps {}

const initialMessageProject: IMessagingProject = {
  name: "",
  namespace: "",
  type: "",
  isNameValid: true,
  customizeEndpoint: false
};

interface IWizardSteps {
  showCustomize: boolean;
  showCertificate: boolean;
  showRoutes: boolean;
}
const CreateMessagingProject: React.FunctionComponent<ICreateMessagingProjectProps> = () => {
  const [messagingProject, setMessagingProject] = useState<IMessagingProject>(
    initialMessageProject
  );
  const [stepStatus, setStepStatus] = useState<IWizardSteps>({
    showCertificate: false,
    showCustomize: false,
    showRoutes: true
  });

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

  const getInitialStep = () => {
    const step = {
      name: "Configuration",
      component: (
        <MessagingProjectConfiguration
          projectDetail={messagingProject}
          setProjectDetail={setMessagingProject}
          setShowConfigurationStep={() => {
            setStepStatus({ ...stepStatus, showCustomize: true });
          }}
        />
      ),
      enableNext: isMessgaingProjectValid(messagingProject)
    };
    return step;
  };
  const getConfigurationStep = () => {
    const getConfiguringStep = () => {
      const step = {
        name: "Configuring",
        component: (
          <ConfiguringEndpoint
            projectDetail={messagingProject}
            setProjectDetail={setMessagingProject}
          />
        ),
        enableNext: isMessgaingProjectValid(messagingProject)
      };
      return step;
    };
    const getCertificatesStep = () => {
      const step = {
        name: "Certificates",
        component: (
          <ConfiguringCertificates
            projectDetail={messagingProject}
            setProjectDetail={setMessagingProject}
          />
        ),
        enableNext: isMessgaingProjectValid(messagingProject)
      };
      return step;
    };
    const getRoutesStep = () => {
      const step = {
        name: "Routes",
        component: (
          <ConfiguringRoutes
            projectDetail={messagingProject}
            setProjectDetail={setMessagingProject}
          />
        ),
        enableNext: isMessgaingProjectValid(messagingProject)
      };
      return step;
    };

    const step = {
      name: "Endpoint customization",
      steps: [
        ...[getConfiguringStep()],
        ...(stepStatus.showCertificate ? [getCertificatesStep()] : []),
        ...(stepStatus.showRoutes ? [getRoutesStep()] : [])
      ],
      enableNext: isMessgaingProjectValid(messagingProject),
      canJumpTo: isMessgaingProjectValid(messagingProject)
    };
    return step;
  };

  const getReviewStep = () => {
    const step = {
      name: "Review",
      component: <MessagingProjectReview projectDetail={messagingProject} />,
      enableNext: isMessgaingProjectValid(messagingProject),
      canJumpTo: isMessgaingProjectValid(messagingProject),
      nextButtonText: "Finish"
    };
    return step;
  };

  const steps = [
    getInitialStep(),
    ...(stepStatus.showCustomize ? [getConfigurationStep()] : []),
    ...[getReviewStep()]
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

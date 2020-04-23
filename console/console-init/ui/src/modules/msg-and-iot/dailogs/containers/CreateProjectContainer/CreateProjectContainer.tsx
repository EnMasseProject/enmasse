/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  WizardFooter,
  WizardContextConsumer,
  Button,
  Wizard
} from "@patternfly/react-core";
import {
  ProjectTypeConfiguration,
  IIoTProjectInput,
  IoTProjectConfiguration,
  IoTProjectReview
} from "modules/msg-and-iot/dailogs/components";
import {
  IMessagingProjectInput,
  MessagingProjectConfiguration
} from "modules/msg-and-iot/dailogs/components/MessagingProjectConfiguration";
import { useMutationQuery } from "hooks";
import { CREATE_ADDRESS_SPACE, RETURN_NAMESPACES } from "graphql-module";
import {
  isMessagingProjectValid,
  isIoTProjectValid
} from "modules/msg-and-iot/dailogs/utils";
import { MessagingProjectReview } from "modules/msg-and-iot/dailogs/components";
import { FinishedStep, IDropdownOption } from "components";
import { useQuery } from "@apollo/react-hooks";
import { INamespaces } from "modules/address-space";
import { Loading } from "use-patternfly";
const CreateProjectContainer: React.FunctionComponent = () => {
  const [isWizardOpen, setIsWizardOpen] = useState<boolean>(false);
  const [messagingProjectDetail, setMessagingProjectDetail] = useState<
    IMessagingProjectInput
  >({ isNameValid: true });
  const [iotProjectDetail, setiotProjectDetail] = useState<IIoTProjectInput>({
    isNameValid: true,
    isEnabled: true
  });
  const [firstSelectedStep, setFirstSelectedStep] = useState<string>();
  const [isCreatedSuccessfully, setIsCreatedSuccessfully] = useState<boolean>(
    false
  );
  const onToggle = () => {
    setIsWizardOpen(!isWizardOpen);
    resetForm();
  };
  const resetForm = () => {
    setMessagingProjectDetail({ isNameValid: true });
    setFirstSelectedStep(undefined);
  };
  const refetchQueries: string[] = ["all_address_spaces"];
  const resetFormState = () => {
    console.log("success");
    setMessagingProjectDetail({ isNameValid: true });
    setIsCreatedSuccessfully(true);
  };
  const [setQueryVariables] = useMutationQuery(
    CREATE_ADDRESS_SPACE,
    refetchQueries,
    resetForm,
    resetFormState
  );
  const handleMessagingProjectSave = () => {
    if (
      messagingProjectDetail &&
      isMessagingProjectValid(messagingProjectDetail)
    ) {
      const variables = {
        as: {
          metadata: {
            name: messagingProjectDetail.messagingProjectName,
            namespace: messagingProjectDetail.namespace
          },
          spec: {
            type:
              messagingProjectDetail.messagingProjectType &&
              messagingProjectDetail.messagingProjectType.toLowerCase(),
            plan:
              messagingProjectDetail.messagingProjectPlan &&
              messagingProjectDetail.messagingProjectPlan.toLowerCase(),
            authenticationService: {
              name: messagingProjectDetail.authenticationService
            }
          }
        }
      };
      setQueryVariables(variables);
      resetForm();
    }
  };

  const { loading, data } = useQuery<INamespaces>(RETURN_NAMESPACES);
  if (loading) return <Loading />;

  const { namespaces } = data || {
    namespaces: []
  };

  const namespaceOptions = namespaces.map(namespace => {
    return {
      value: namespace.metadata.name,
      label: namespace.metadata.name
    };
  });

  const handleIoTProjectSave = async () => {
    console.log("iot created");
    resetForm();
  };

  const step1 = {
    name: "Project Type",
    component: (
      <ProjectTypeConfiguration
        selectedStep={firstSelectedStep}
        setSelectedStep={setFirstSelectedStep}
      />
    )
  };

  const configurationStepForIot = IoTProjectConfiguration(
    setiotProjectDetail,
    namespaceOptions,
    iotProjectDetail
  );

  const finalStepForIot = IoTProjectReview(iotProjectDetail);

  const finishedStep = {
    name: "Finish",
    component: <FinishedStep onClose={onToggle} sucess={true} />,
    isFinishedStep: true
  };
  const configurationStepForMessaging = MessagingProjectConfiguration(
    setMessagingProjectDetail,
    messagingProjectDetail
  );
  const finalStepForMessaging = MessagingProjectReview(messagingProjectDetail);
  const steps = [step1];
  if (firstSelectedStep) {
    if (firstSelectedStep === "iot") {
      steps.push(configurationStepForIot);
      steps.push(finalStepForIot);
    } else {
      steps.push(configurationStepForMessaging);
      steps.push(finalStepForMessaging);
    }
  }
  steps.push(finishedStep);
  const handleNextIsEnabled = () => {
    if (firstSelectedStep) {
      if (firstSelectedStep === "messaging") {
        if (isMessagingProjectValid(messagingProjectDetail)) {
          return true;
        } else {
          return false;
        }
      } else if (firstSelectedStep === "iot") {
        if (isIoTProjectValid(iotProjectDetail)) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  };

  const CustomFooter = (
    <WizardFooter>
      <WizardContextConsumer>
        {({ activeStep, onNext, onBack, onClose }) => {
          if (
            activeStep.name === "Project Type" ||
            activeStep.name === "Finish"
          ) {
            return (
              <>
                <Button
                  variant="primary"
                  type="submit"
                  onClick={onNext}
                  className={!firstSelectedStep ? "pf-m-disabled" : ""}
                >
                  Next
                </Button>
                <Button
                  variant="secondary"
                  onClick={onBack}
                  className={
                    activeStep.name === "Project Type" ? "pf-m-disabled" : ""
                  }
                >
                  Back
                </Button>
                <Button variant="link" onClick={onClose}>
                  Cancel
                </Button>
              </>
            );
          }
          if (activeStep.name !== "Review") {
            return (
              <>
                <Button
                  variant="primary"
                  type="submit"
                  onClick={onNext}
                  className={handleNextIsEnabled() ? "" : "pf-m-disabled"}
                >
                  Next
                </Button>
                <Button variant="secondary" onClick={onBack}>
                  Back
                </Button>
                <Button variant="link" onClick={onClose}>
                  Cancel
                </Button>
              </>
            );
          }

          return (
            <>
              <Button
                variant="primary"
                type="submit"
                onClick={
                  firstSelectedStep && firstSelectedStep === "messaging"
                    ? handleMessagingProjectSave
                    : handleIoTProjectSave
                }
              >
                Finish
              </Button>
              <Button onClick={onBack} variant="secondary">
                Back
              </Button>
              <Button variant="link" onClick={onClose}>
                Cancel
              </Button>
            </>
          );
        }}
      </WizardContextConsumer>
    </WizardFooter>
  );

  return (
    <>
      <Button variant="primary" onClick={onToggle}>
        Create
      </Button>
      {isWizardOpen && (
        <Wizard
          isOpen={isWizardOpen}
          onClose={onToggle}
          footer={CustomFooter}
          title="Create"
          description="Following three steps to create new project"
          steps={steps}
        />
      )}
    </>
  );
};

export { CreateProjectContainer };

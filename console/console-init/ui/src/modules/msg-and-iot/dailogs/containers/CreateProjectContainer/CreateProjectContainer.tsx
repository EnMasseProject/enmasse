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
import { ProjectTypeConfiguration } from "modules/msg-and-iot/dailogs/components";
import {
  IMessagingProjectInput,
  MessagingProjectConfiguration
} from "modules/msg-and-iot/dailogs/components/MessagingProjectConfiguration";
import { useMutationQuery } from "hooks";
import { CREATE_ADDRESS_SPACE } from "graphql-module";
import { isMessagingProjectValid } from "modules/msg-and-iot/dailogs/utils";
import { MessagingProjectReview } from "modules/msg-and-iot/dailogs/components";
import { FinishedStep } from "components";
const CreateProjectContainer: React.FunctionComponent = () => {
  const [isWizardOpen, setIsWizardOpen] = useState<boolean>(true);
  const [messagingProjectDetail, setMessagingProjectDetail] = useState<
    IMessagingProjectInput
  >();
  const [firstSelectedStep, setFirstSelectedStep] = useState<string>();
  const [isCreatedSuccessfully, setIsCreatedSuccessfully] = useState<boolean>(
    false
  );
  const onToggle = () => {
    setIsWizardOpen(!isWizardOpen);
    resetForm();
  };
  const resetForm = () => {
    setMessagingProjectDetail(undefined);
    setFirstSelectedStep(undefined);
  };
  const isMessagingFinishEnabled = () =>
    isMessagingProjectValid(messagingProjectDetail);
  const refetchQueries: string[] = ["all_address_spaces"];
  const resetFormState = () => {
    console.log("success");
    setMessagingProjectDetail(undefined);
    setIsCreatedSuccessfully(true);
  };
  const [setQueryVariables] = useMutationQuery(
    CREATE_ADDRESS_SPACE,
    refetchQueries,
    resetForm,
    resetFormState
  );
  const handleMessagingProjectSave = async () => {
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
      const data: any = await setQueryVariables(variables);
      console.log(data);
      resetForm();
    }
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

  const configurationStepForIot = {
    name: "Configuration",
    component: <>iot</>
  };

  const finalStepForIot = {
    name: "Review",
    component: <>Iot review</>,
    nextButtonText: "Finish"
  };
  const finishedStep = {
    name: "Finish",
    component: (
      <FinishedStep onClose={onToggle} sucess={isCreatedSuccessfully} />
    ),
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

  const CustomFooter = (
    <WizardFooter>
      <WizardContextConsumer>
        {({ activeStep, onNext, onBack, onClose }) => {
          if (activeStep.name === "Project Type" || "Finish") {
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
                  className={
                    firstSelectedStep === "messaging" &&
                    !isMessagingFinishEnabled()
                      ? "pf-m-disabled"
                      : ""
                  }
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
          // Final step buttons
          let confirmButton;
          if (firstSelectedStep && firstSelectedStep === "iot") {
            confirmButton = (
              <Button
                variant="primary"
                type="submit"
                onClick={() => {
                  console.log("iot created");
                }}
              >
                Finish
              </Button>
            );
          } else {
            confirmButton = (
              <Button
                variant="primary"
                type="submit"
                onClick={handleMessagingProjectSave}
              >
                Finish
              </Button>
            );
          }
          return (
            <>
              {confirmButton}
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
        Show Wizard
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

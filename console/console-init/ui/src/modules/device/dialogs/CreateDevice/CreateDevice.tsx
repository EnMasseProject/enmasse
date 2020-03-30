/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Wizard,
  WizardFooter,
  WizardContextConsumer,
  Button,
  SelectOptionObject
} from "@patternfly/react-core";
import { DeviceInformation } from "./DeviceInformation";
import { ConnectionType } from "modules/device/components/ConnectionTypeStep";

export interface IDeviceInfo {
  onPropertySelect: (e: any, selection: SelectOptionObject) => void;
  onChangePropertyInput?: (value: string) => Promise<any>;
  onPropertyClear: () => void;
  propertySelected?: string;
  propertyInput?: string;
  setPropertyInput?: (value: string) => void;
}

const CreateDevice: React.FunctionComponent<IDeviceInfo> = ({
  onPropertySelect,
  onChangePropertyInput,
  onPropertyClear,
  propertySelected,
  propertyInput,
  setPropertyInput
}) => {
  const [isWizardOpen, setIsWizardOpen] = useState<boolean>(true);
  const [connectionType, setConnectionType] = useState<string>();
  const onToggle = () => {
    setIsWizardOpen(!isWizardOpen);
    resetForm();
  };
  const resetForm = () => {};
  const addGateway = {
    name: "Add gateways",
    component: <p>Add gateway</p>
  };
  const addCredentials = {
    name: "Add credentials",
    component: <p>Add credentials</p>
  };
  const reviewForm = {
    name: "Review",
    component: <p>Review</p>
  };
  const onConnectionChange = (_: boolean, event: any) => {
    const data = event.target.value;
    if (data) {
      setConnectionType(data);
    }
  };
  const steps = [
    {
      name: "Device information",
      component: (
        <DeviceInformation
          onPropertySelect={onPropertySelect}
          onChangePropertyInput={onChangePropertyInput}
          onPropertyClear={onPropertyClear}
          propertySelected={propertySelected}
          propertyInput={propertyInput}
          setPropertyInput={setPropertyInput}
        />
      )
    },
    {
      name: "Connection Type",
      component: (
        <ConnectionType
          connectionType={connectionType}
          onConnectionChange={onConnectionChange}
        />
      )
    }
  ];
  if (connectionType) {
    if (connectionType === "directly") {
      steps.push(addCredentials);
    } else {
      steps.push(addGateway);
    }
    steps.push(reviewForm);
  }

  const handleNextIsEnabled = () => {
    return false;
  };

  const CustomFooter = (
    <WizardFooter>
      <WizardContextConsumer>
        {({ activeStep, onNext, onBack, onClose }) => {
          if (
            activeStep.name === "Device information" ||
            activeStep.name === "Connection Type"
          ) {
            return (
              <>
                <Button
                  variant="primary"
                  type="submit"
                  onClick={onNext}
                  className={
                    activeStep.name === "Connection Type" && !connectionType
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
                    activeStep.name === "Device information"
                      ? "pf-m-disabled"
                      : ""
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
                // onClick={
                //   firstSelectedStep && firstSelectedStep === "messaging"
                //     ? handleMessagingProjectSave
                //     : handleIoTProjectSave
                // }
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
      {isWizardOpen ? (
        <>
          <Wizard
            isInPage
            isOpen={isWizardOpen}
            onClose={onToggle}
            footer={CustomFooter}
            steps={steps}
          />
        </>
      ) : (
        <Button variant="primary" onClick={onToggle}>
          Create
        </Button>
      )}
    </>
  );
};

export { CreateDevice };

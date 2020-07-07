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
import { ConnectionType } from "modules/iot-device/components/ConnectionTypeStep";
import { AddGateways } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";
import { AddCredentials } from "stories/AddCredential.stories";

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
  const { dispatch, state } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onConfirm, onClose } = modalProps || {};
  const [connectionType, setConnectionType] = useState<string>();
  const [addedGateways, setAddedGateways] = useState<string[]>([]);

  const getGateways = (gateways: string[]) => {
    setAddedGateways(gateways);
  };

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    onClose && onClose();
  };

  const addGateway = {
    name: "Add gateways",
    component: <AddGateways returnGateways={getGateways} />
  };

  const addCredentials = {
    name: "Add credentials",
    component: <AddCredentials />
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

  const handleSave = async () => {
    // if (name) {
    //   const getVariables = () => {
    // let variable: any = {
    //   metadata: {
    //     namespace: namespace
    //   },
    //   spec: {
    //     type: addressType.toLowerCase(),
    //     plan: plan,
    //     address: addressName
    //   }
    // };
    // if (addressType && addressType.trim().toLowerCase() === "subscription")
    //   variable.spec.topic = topic;
    // return variable;
    // };
    // const variables = {
    // a: getVariables(),
    // as: name
    // };
    // await setAddressQueryVariables(variables);
    // }

    onCloseDialog();
    onConfirm && onConfirm();
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
    <Wizard
      onClose={onCloseDialog}
      onSave={handleSave}
      footer={CustomFooter}
      steps={steps}
    />
  );
};

export { CreateDevice };

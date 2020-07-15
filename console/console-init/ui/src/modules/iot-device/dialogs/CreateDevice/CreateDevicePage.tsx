/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useMemo } from "react";
import {
  Wizard,
  WizardFooter,
  WizardContextConsumer,
  Button,
  Breadcrumb,
  BreadcrumbItem,
  PageSection,
  Page,
  PageSectionVariants,
  Divider,
  Title,
  Flex,
  FlexItem,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { DeviceInformation } from "./DeviceInformation";
import { ConnectionType } from "modules/iot-device/components/ConnectionTypeStep";
import { AddGateways, AddCredential } from "modules/iot-device/components";
import { useHistory, useParams } from "react-router";
import { Link } from "react-router-dom";
import { useBreadcrumb } from "use-patternfly";
import { SwitchWithToggle } from "components";

export default function CreateDevicePage() {
  const history = useHistory();
  const [connectionType, setConnectionType] = useState<string>();
  const [addedGateways, setAddedGateways] = useState<string[]>([]);
  const { projectname, namespace } = useParams();
  const deviceListRouteLink = `/iot-projects/${namespace}/${projectname}/devices`;
  const breadcrumb = useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="cdetail-link-home" to={"/"}>
            Home
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem isActive={true}>{projectname}</BreadcrumbItem>
      </Breadcrumb>
    ),
    [projectname]
  );

  useBreadcrumb(breadcrumb);

  const getGateways = (gateways: string[]) => {
    setAddedGateways(gateways);
  };

  const onCloseDialog = () => {
    history.push(deviceListRouteLink);
  };

  const addGateway = {
    name: "Add gateways",
    component: <AddGateways returnGateways={getGateways} />
  };

  const AddCredentialWrapper = () => (
    <Grid hasGutter>
      <GridItem span={8}>
        <Title size="2xl" headingLevel="h1">
          Add credentials to this new device{" "}
        </Title>
        <br />
      </GridItem>
      <GridItem span={8}>
        <AddCredential />
      </GridItem>
    </Grid>
  );

  const addCredentials = {
    name: "Add credentials",
    component: <AddCredentialWrapper />
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
    history.push(deviceListRouteLink);
  };

  const steps = [
    {
      name: "Device information",
      component: <DeviceInformation />
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
    return true;
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
              <Button variant="primary" onClick={handleSave} type="submit">
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
    <Page>
      <PageSection variant={PageSectionVariants.light}>
        <Flex>
          <FlexItem>
            <Title size={"2xl"} headingLevel="h1">
              Add a device
            </Title>
          </FlexItem>
          <FlexItem align={{ default: "alignRight" }}>
            <SwitchWithToggle
              id={"add-device-view-in-json"}
              labelOff={"View JSON Format"}
              onChange={() => {
                console.log("View in JSON");
              }}
              label={"View Form Format"}
            />
          </FlexItem>
        </Flex>
        <br />
        <Divider />
      </PageSection>
      <Wizard
        onClose={onCloseDialog}
        onSave={handleSave}
        footer={CustomFooter}
        steps={steps}
      />
    </Page>
  );
}

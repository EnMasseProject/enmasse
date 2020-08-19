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
import {
  AddGateways,
  AddCredential,
  AddGatewayGroupMembership
} from "modules/iot-device/components";
import { useHistory, useParams } from "react-router";
import { Link } from "react-router-dom";
import { useBreadcrumb } from "use-patternfly";
import { SwitchWithToggle } from "components";
import {
  ReviewDeviceContainer,
  IDeviceProp
} from "modules/iot-device/containers";
import { StyleSheet, css } from "aphrodite";

const getInitialDeviceForm = () => {
  const device: IDeviceProp = {
    connectionType: "directly",
    deviceInformation: {},
    gateways: {}
  };
  return device;
};

export default function CreateDevicePage() {
  const history = useHistory();
  // const [connectionType, setConnectionType] = useState<string>("directly");
  const [gatewayDevices, setGatewayDevices] = useState<string[]>([]);
  const [gatewayGroups, setGatewayGroups] = useState<string[]>([]);
  const [memberOf, setMemberOf] = useState<string[]>([]);
  const { projectname, namespace } = useParams();

  const styles = StyleSheet.create({
    lighter_text: {
      fontWeight: "lighter"
    }
  });

  const deviceListRouteLink = `/iot-projects/${namespace}/${projectname}/devices`;
  const [device, setDevice] = useState<IDeviceProp>(getInitialDeviceForm());
  const breadcrumb = useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="home-link" to={"/"}>
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
    setGatewayDevices(gateways);
  };

  const getGatewayGroups = (groups: string[]) => {
    setGatewayGroups(groups);
  };

  const onCloseDialog = () => {
    history.push(deviceListRouteLink);
  };

  const addGateway = {
    name: "Add gateways",
    component: (
      <AddGateways
        gatewayDevices={gatewayDevices}
        gatewayGroups={gatewayGroups}
        returnGatewayDevices={getGateways}
        returnGatewayGroups={getGatewayGroups}
      />
    )
  };

  const AddGatewayGroupMembershipWrapper = () => (
    <>
      <Title size="2xl" headingLevel="h1">
        Assign to gateway groups{" "}
        <small className={css(styles.lighter_text)}>(Optional)</small>
      </Title>
      <br />
      <AddGatewayGroupMembership
        id="some-random-id"
        gatewayGroups={memberOf}
        returnGatewayGroups={setMemberOf}
      />
    </>
  );

  const assignGroups = {
    name: "Gateway groups",
    component: <AddGatewayGroupMembershipWrapper />
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
    component: (
      <ReviewDeviceContainer
        device={device}
        title={"Verify that the following information is correct before done"}
      />
    )
  };

  const onChangeConnection = (_: boolean, event: any) => {
    const connectionType = event.target.value;
    if (connectionType) {
      setDevice({ ...device, connectionType });
    }
  };

  const handleSave = async () => {
    // Add query to add device
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
          connectionType={device.connectionType}
          onChangeConnection={onChangeConnection}
        />
      )
    }
  ];

  if (device.connectionType) {
    if (device.connectionType === "directly") {
      steps.push(addCredentials);
    } else {
      steps.push(addGateway);
    }
    steps.push(assignGroups);
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
                  id="create-device-page-next-button"
                  variant="primary"
                  type="submit"
                  onClick={onNext}
                  className={
                    activeStep.name === "Connection Type" &&
                    !device.connectionType
                      ? "pf-m-disabled"
                      : ""
                  }
                >
                  Next
                </Button>
                <Button
                  id="create-device-page-back-button"
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
                <Button
                  id="create-device-page-cancel-button"
                  variant="link"
                  onClick={onClose}
                >
                  Cancel
                </Button>
              </>
            );
          }
          if (activeStep.name !== "Review") {
            return (
              <>
                <Button
                  id="create-device-page-next-button"
                  variant="primary"
                  type="submit"
                  onClick={onNext}
                  className={handleNextIsEnabled() ? "" : "pf-m-disabled"}
                >
                  Next
                </Button>
                <Button
                  id="create-device-page-back-button"
                  variant="secondary"
                  onClick={onBack}
                >
                  Back
                </Button>
                <Button
                  id="create-device-page-cancel-button"
                  variant="link"
                  onClick={onClose}
                >
                  Cancel
                </Button>
              </>
            );
          }

          return (
            <>
              <Button
                id="create-device-page-finish-button"
                variant="primary"
                onClick={handleSave}
                type="submit"
              >
                Finish
              </Button>
              <Button
                id="create-device-page-back-button"
                onClick={onBack}
                variant="secondary"
              >
                Back
              </Button>
              <Button
                id="create-device-page-cancel-button"
                variant="link"
                onClick={onClose}
              >
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
              id={"create-device-page-view-json-switchtoggle"}
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
        id="create-device-page-wizard"
        onClose={onCloseDialog}
        onSave={handleSave}
        footer={CustomFooter}
        steps={steps}
      />
    </Page>
  );
}

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { select, text, number } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import {
  ProjectNavigation,
  GeneralInfo,
  IIoTMessagingObject
} from "modules/iot-project-detail/components";
import { Page, Grid, GridItem } from "@patternfly/react-core";
import {
  DeviceRegistationManagement,
  AccessCredentials
} from "modules/iot-project-detail/components";
import { IAdapter } from "components";
import { action } from "@storybook/addon-actions";
import { Protocols } from "constant";

export default {
  title: "Project Detail"
};

export const projectDetailHeaderNavigation = () => {
  const options = ["detail", "devices", "certificates"];
  return (
    <MemoryRouter>
      <ProjectNavigation
        activeItem={select("Active Nav Item", options, "devices")}
      />
    </MemoryRouter>
  );
};
const eventAddress: string = "qpid-jms:sender";
const telemetryAddress: string = "qpid-jms:sender";
const commandAddresses: Array<string> = ["qpid-jms:sender", "Reciever-156458"];

export const projectGeneralInfo = () => {
  return (
    <MemoryRouter>
      <Page>
        <Grid>
          <GridItem span={6}>
            <GeneralInfo
              addressSpace={text("addressSpace", "devops_iot")}
              eventAddress={eventAddress}
              namespace={""}
              telemetryAddress={telemetryAddress}
              commandAddresses={commandAddresses}
              maxConnection={number("Max connections", 50000)}
              dataVolume={number("Data volume", 50000)}
              startDate={text("Start Date", "start Date")}
              endDate={text("End Date", "end Date")}
            />
          </GridItem>
        </Grid>
      </Page>
    </MemoryRouter>
  );
};
export const projectDetailRegistryManagement = () => {
  const registrationApi: IAdapter = {
    url: "https://http.bosch-iot-hub.com",
    host: "mange.bosh-iot-hub.com",
    port: 5647
  };
  const credentialApi: IAdapter = {
    url: "https://http.bosch-iot-hub.com",
    host: "mange.bosh-iot-hub.com",
    port: 268
  };
  return (
    <MemoryRouter>
      <Page>
        <Grid>
          <GridItem span={6}>
            <DeviceRegistationManagement
              username={text("username", "username")}
              password={text("password", "password")}
              registrationApi={registrationApi}
              credentialApi={credentialApi}
            />
          </GridItem>
        </Grid>
      </Page>
    </MemoryRouter>
  );
};

export const projectAccessCredentials = () => {
  const messaging: IIoTMessagingObject = {
    url: "https://http.bosch-iot-hub.com",
    username: text("username", "username"),
    password: text("password", "password"),
    addressSpace: text("Address space", "devops-iottest"),
    eventAddress: eventAddress,
    telemetryAddress: telemetryAddress,
    commandAddresses: commandAddresses
  };
  const httpAdapter: IAdapter = {
    name: Protocols.HTTP,
    url: "https://http.bosch-iot-hub.com"
  };
  const mqttAdapter: IAdapter = {
    name: Protocols.MQTT,
    tls: true,
    host: "mange.bosh-iot-hub.com",
    port: 8883
  };
  const amqpAdapter: IAdapter = {
    name: Protocols.AMQP,
    url: "https://http.bosch-iot-hub.com"
  };
  const coapAdapter: IAdapter = {
    name: Protocols.COAP,
    url: "https://http.bosch-iot-hub.com"
  };
  const adapters: IAdapter[] = [
    httpAdapter,
    mqttAdapter,
    amqpAdapter,
    coapAdapter
  ];

  return (
    <MemoryRouter>
      <Page>
        <Grid>
          <GridItem span={6}>
            <AccessCredentials
              tenantId={text("Tenenant Id", "FNDSKB5GSD58EGWAW6663RWfsaf8")}
              messaging={messaging}
              adapters={adapters}
              onDownloadCertificate={action("onDownload triggered")}
            />
          </GridItem>
        </Grid>
      </Page>
    </MemoryRouter>
  );
};

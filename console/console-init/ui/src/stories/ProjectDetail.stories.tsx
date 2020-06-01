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
  IMessagingObject
} from "modules/iot-project-detail/components";
import { Page, Grid, GridItem } from "@patternfly/react-core";
import {
  DeviceRegistationManagement,
  AccessCredentials
} from "modules/iot-project-detail/components";
import { IAdapter, IAdapterConfig } from "components";
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
const eventAddresses: Array<string> = ["qpid-jms:sender"];
const telemetryAddresses: Array<string> = ["qpid-jms:sender"];
const commandAddresses: Array<string> = ["qpid-jms:sender", "Reciever-156458"];

export const projectGeneralInfo = () => {
  return (
    <MemoryRouter>
      <Page>
        <Grid>
          <GridItem span={6}>
            <GeneralInfo
              addressSpace={text("addressSpace", "devops_iot")}
              eventAddresses={eventAddresses}
              namespace={""}
              telemetryAddresses={telemetryAddresses}
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
  const registrationApi: IAdapterConfig = {
    url: "https://http.bosch-iot-hub.com",
    host: "mange.bosh-iot-hub.com",
    port: 5647
  };
  const credentialApi: IAdapterConfig = {
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
  const messaging: IMessagingObject = {
    url: "https://http.bosch-iot-hub.com",
    username: text("username", "username"),
    password: text("password", "password"),
    addressSpace: text("Address space", "devops-iottest"),
    eventsAddresses: eventAddresses,
    telemetryAddresses: telemetryAddresses,
    commandAddresses: commandAddresses
  };
  const httpAdapter: IAdapterConfig = {
    url: "https://http.bosch-iot-hub.com"
  };
  const mqttAdapter: IAdapterConfig = {
    tlsEnabled: true,
    host: "mange.bosh-iot-hub.com",
    port: 8883
  };
  const amqpAdapter: IAdapterConfig = {
    url: "https://http.bosch-iot-hub.com"
  };
  const coapAdapter: IAdapterConfig = {
    url: "https://http.bosch-iot-hub.com"
  };
  const adapters: IAdapter[] = [
    { type: Protocols.HTTP, value: httpAdapter },
    { type: Protocols.MQTT, value: mqttAdapter },
    { type: Protocols.AMQP, value: amqpAdapter },
    { type: Protocols.COAP, value: coapAdapter }
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

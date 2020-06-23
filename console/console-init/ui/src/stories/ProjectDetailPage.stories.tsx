/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { text, number } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import {
  DeviceRegistationManagement,
  AccessCredentials,
  IIoTMessagingObject,
  GeneralInfo
} from "modules/iot-project-detail/components";
import { action } from "@storybook/addon-actions";
import {
  Grid,
  GridItem,
  Page,
  PageSection,
  PageSectionVariants
} from "@patternfly/react-core";
import { IAdapterConfig, IAdapter } from "components";
import { Protocols } from "constant";

export default {
  title: "Project Detail Page"
};

export const IoTProjectDetailInfoPage = () => {
  const eventAddresses: Array<string> = [
    text("Event Address Name", "event_address"),
    text("Event Address Name 1", "event_address1")
  ];
  const telemetryAddresses: Array<string> = [
    text("Telemetry Address Name", "telemetry_address")
  ];
  const commandAddresses: Array<string> = [
    text("Command Address Name", "command_address")
  ];

  const messaging: IIoTMessagingObject = {
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

  const Data = (
    <Grid>
      <GridItem span={6}>
        <GeneralInfo
          addressSpace={text("addressSpace", "devops_iot")}
          eventAddresses={eventAddresses}
          telemetryAddresses={telemetryAddresses}
          commandAddresses={commandAddresses}
          maxConnection={number("Max connections", 50000)}
          dataVolume={number("Data volume", 50000)}
          startDate={text("Start Date", "start Date")}
          endDate={text("End Date", "end Date")}
          namespace={text("Namespace", "namespace")}
        />
        <DeviceRegistationManagement
          token={text("token", "username")}
          endpoiuntUrl={text("url", "https://http.bosch-iot-hub.com")}
        />
      </GridItem>
      <GridItem span={6}>
        <AccessCredentials
          tenantId={text("Tenenant Id", "FNDSKB5GSD58EGWAW6663RWfsaf8")}
          messaging={messaging}
          adapters={adapters}
          onDownloadCertificate={action("onDownload triggered")}
        />
      </GridItem>
    </Grid>
  );
  return (
    <MemoryRouter>
      <Page>
        <PageSection variant={PageSectionVariants.default}>{Data}</PageSection>
      </Page>
    </MemoryRouter>
  );
};

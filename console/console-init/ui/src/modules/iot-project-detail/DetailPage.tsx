import React from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import {
  GeneralInfo,
  DeviceRegistationManagement,
  AccessCredentials,
  IMessagingObject
} from ".";
import { action } from "@storybook/addon-actions";
import { IAdapterConfig, IAdapter } from "components/AdapterList";
import { Protocols } from "constant";

export default function DetailPage() {
  const eventAddresses: Array<string> = ["event_address", "event_address1"];
  const telemetryAddresses: Array<string> = ["telemetry_address"];
  const commandAddresses: Array<string> = ["command_address"];

  const messaging: IMessagingObject = {
    url: "https://http.bosch-iot-hub.com",
    username: "username",
    password: "password",
    addressSpace: "devops-iottest",
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
    <Grid>
      <GridItem span={6}>
        <GeneralInfo
          addressSpace={"devops_iot"}
          eventAddresses={eventAddresses}
          telemetryAddresses={telemetryAddresses}
          commandAddresses={commandAddresses}
          maxConnection={50000}
          dataVolume={50000}
          startDate={"start Date"}
          endDate={"end Date"}
          namespace={"namespace"}
        />
        <DeviceRegistationManagement
          token={"username"}
          endpoiuntUrl={"https://http.bosch-iot-hub.com"}
        />
      </GridItem>
      <GridItem span={6}>
        <AccessCredentials
          tenantId={"FNDSKB5GSD58EGWAW6663RWfsaf8"}
          messaging={messaging}
          adapters={adapters}
          onDownloadCertificate={action("onDownload triggered")}
        />
      </GridItem>
    </Grid>
  );
}

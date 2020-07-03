import React from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import {
  GeneralInfo,
  DeviceRegistationManagement,
  AccessCredentials,
  IIoTMessagingObject
} from "modules/iot-project-detail/components";
import { action } from "@storybook/addon-actions";
import { IAdapter } from "components/AdapterList";
import { Protocols } from "constant";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_PROJECTS } from "graphql-module/queries/iot_project";
import { IIoTProjectsResponse } from "schema/iot_project";

export default function IoTProjectDetailInfoPage() {
  const { projectname } = useParams();

  const queryResolver = `
  iotProjects {
    metadata{
      namespace
    }
    spec {
      downstreamStrategy {
        ... on ManagedDownstreamStrategy_iot_enmasse_io_v1alpha1 {
          addressSpace {
            name
          }
          addresses {
            Telemetry {
              name
            }
            Event {
              name
            }
            Command {
              name
            }
          }
        }
      }
    }
    status {
      tenantName
      downstreamEndpoint {
        host
        port
        credentials {
          username
          password
        }
      }
    }
    endpoints {
      name
      url
      host
    }
  }
`;

  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectname }, queryResolver)
  );

  const { allProjects } = data || {
    allProjects: {
      iotProjects : []
    }
  };

  const { spec, metadata, status } = allProjects?.iotProjects?.[0] || {};
  const { name: addressSpace } = spec?.downstreamStrategy?.addressSpace || { name : "" };
  const { username, password } = status?.downstreamEndpoint?.credentials || {};

  const { Telemetry, Event, Command } = spec?.downstreamStrategy?.addresses || {};

  const telemetryAddress = Telemetry?.name || "";
  const eventAddress = Event?.name || "";
  const commandAddresses = Command || [];

  const messaging: IIoTMessagingObject = {
    url: "https://http.bosch-iot-hub.com",
    username,
    password,
    addressSpace,
    eventAddress,
    telemetryAddress,
    commandAddresses: []
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
    <Grid>
      <GridItem span={6}>
        <GeneralInfo
          addressSpace={addressSpace}
          eventAddress={eventAddress}
          telemetryAddress={telemetryAddress}
          commandAddresses={commandAddresses.map((as: any) => as.name)}
          maxConnection={50000}
          dataVolume={50000}
          startDate={"start Date"}
          endDate={"end Date"}
          namespace={metadata?.namespace}
        />
        <DeviceRegistationManagement
          // To be replaced with the OAuth token
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

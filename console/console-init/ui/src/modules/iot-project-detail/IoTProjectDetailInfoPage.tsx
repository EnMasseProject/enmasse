import React from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import {
  GeneralInfo,
  DeviceRegistationManagement,
  AccessCredentials,
  IIoTMessagingObject
} from "modules/iot-project-detail/components";
import { IAdapter } from "components/AdapterList";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_PROJECTS } from "graphql-module/queries/iot_project";
import { IIoTProjectsResponse } from "schema/iot_project";

export default function IoTProjectDetailInfoPage() {
  const { projectname } = useParams();

  const queryResolver = `
  total
    objects{
      ... on IoTProject_iot_enmasse_io_v1alpha1 {
        kind
        metadata {
          name
          namespace
          creationTimestamp
        }
        iotStatus: status{
          phase
          phaseReason 
        }
        enabled
        spec{
          tenantId
          configuration
          addresses{
                Telemetry{
                  name
                }
                Event{
                  name
                }
                Command{
                  name
                }
          }
        }
        status{
          phase
          phaseReason
        }
        endpoints {
          name
          url
          host
          port
          tls
        }
      }
    }
`;

  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectname }, queryResolver)
  );

  const { allProjects } = data || {
    allProjects: {
      objects: []
    }
  };

  const { spec, metadata, endpoints } = allProjects?.objects?.[0] || {};

  //TODO: add username and password to be updated from the server response
  //TODO: Remove addressSpace as it is not proesent in shared infra

  const { name: addressSpace } = { name: "addressspace_dummy" };
  const { username, password } = {
    username: "dummy_username",
    password: "dummy_password"
  };

  const { Telemetry, Event, Command } = spec?.addresses || {};

  const telemetryAddress = Telemetry?.name || "";
  const eventAddress = Event?.name || "";
  const commandAddresses = Command
    ? Command.map(add => (add && add.name ? add.name : ""))
    : [];

  const messaging: IIoTMessagingObject = {
    username,
    password,
    addressSpace,
    eventAddress,
    telemetryAddress,
    commandAddresses: commandAddresses
  };

  const adapters: IAdapter[] = endpoints || [];
  return (
    <Grid>
      <GridItem span={6}>
        <GeneralInfo
          addressSpace={addressSpace}
          eventAddress={eventAddress}
          telemetryAddress={telemetryAddress}
          commandAddresses={commandAddresses}
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
          tenantId={spec?.tenantId}
          messaging={messaging}
          adapters={adapters}
          onDownloadCertificate={() => console.log("onDownload triggered")}
        />
      </GridItem>
    </Grid>
  );
}

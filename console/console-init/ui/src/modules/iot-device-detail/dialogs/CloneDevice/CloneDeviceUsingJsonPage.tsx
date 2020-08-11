/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useMemo } from "react";
import { AddDeviceWithJson } from "modules/iot-device/components";
import { useParams, useHistory } from "react-router";
import {
  PageSection,
  PageSectionVariants,
  Title,
  Page,
  Divider,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import { useBreadcrumb, Loading } from "use-patternfly";
import { Link } from "react-router-dom";
import { useQuery } from "@apollo/react-hooks";
import { IDeviceDetailResponse, ICreateDeviceResponse } from "schema";
import { RETURN_IOT_DEVICE_DETAIL, CREATE_IOT_DEVICE } from "graphql-module";
import { FetchPolicy } from "constant";
import { useMutationQuery } from "hooks";
import { getDeviceFromDeviceString } from "modules/iot-device/utils";

export default function CreateDeviceUsingJsonPage() {
  const history = useHistory();
  const { projectname, namespace, deviceid } = useParams();
  // const [deviceDetail, setDeviceDetail] = useState<string>();
  const deviceListRouteLink = `/iot-projects/${namespace}/${projectname}/devices`;
  const previousDeviceLink = `/iot-projects/${namespace}/${projectname}/devices/${deviceid}/device-info`;

  const onSuccess = () => {
    history.push(deviceListRouteLink);
  };

  const [setCreateDeviceQueryVariables] = useMutationQuery(
    CREATE_IOT_DEVICE,
    undefined,
    undefined,
    onSuccess
  );

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

  const onSave = async (detail: string) => {
    //TODO: Add query to save iot device
    if (detail) {
      const device: ICreateDeviceResponse = getDeviceFromDeviceString(detail);
      const variable = {
        iotproject: { name: projectname, namespace },
        device: device
      };
      await setCreateDeviceQueryVariables(variable);
    }
  };

  const onLeave = () => {
    history.push(previousDeviceLink);
  };

  const queryResolver = `
    devices{
      registration{
        enabled
        via
        memberOf
        viaGroups
        ext
        defaults
      }
    }
  `;

  const { loading, data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, namespace, deviceid, queryResolver),
    { fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading) {
    return <Loading />;
  }

  const { devices } = data || {
    devices: { total: 0, devices: [] }
  };

  const device = devices.devices[0];

  return (
    <Page>
      <PageSection variant={PageSectionVariants.light}>
        <br />
        <Title size={"2xl"} headingLevel="h1">
          Clone a device
        </Title>
        <br />
        <Divider />
      </PageSection>
      <AddDeviceWithJson
        deviceDetail={device}
        onLeave={onLeave}
        onSave={onSave}
        allowTemplate={false}
      />
    </Page>
  );
}

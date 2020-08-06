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
import { useBreadcrumb } from "use-patternfly";
import { Link } from "react-router-dom";
import { getDeviceFromDeviceString } from "modules/iot-device/utils";
import { useMutationQuery } from "hooks";
import { CREATE_IOT_DEVICE } from "graphql-module";
import { ICreateDeviceResponse } from "schema";

export default function AddDeviceUsingJsonPage() {
  const history = useHistory();
  const { projectname, namespace } = useParams();
  const [deviceDetail, setDeviceDetail] = useState<string>();
  const deviceListRouteLink = `/iot-projects/${namespace}/${projectname}/devices`;

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

  const onSave = async (_detail: string) => {
    //TODO: Add query to save iot device
    if (deviceDetail) {
      const device: ICreateDeviceResponse = getDeviceFromDeviceString(
        deviceDetail
      );
      const variable = {
        iotproject: { name: projectname, namespace },
        device: device
      };
      await setCreateDeviceQueryVariables(variable);
    }
  };

  const onLeave = () => {
    history.push(deviceListRouteLink);
  };

  return (
    <Page>
      <PageSection variant={PageSectionVariants.light}>
        <br />
        <Title size={"2xl"} headingLevel="h1">
          Add a device
        </Title>
        <br />
        <Divider />
      </PageSection>
      <AddDeviceWithJson
        deviceDetail={deviceDetail}
        setDeviceDetail={setDeviceDetail}
        onLeave={onLeave}
        onSave={onSave}
      />
    </Page>
  );
}

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

export default function CreateDeviceUsingJsonPage() {
  const history = useHistory();
  const { projectname, namespace, deviceid } = useParams();
  const [deviceDetail, setDeviceDetail] = useState<string>();
  const deviceListRouteLink = `/iot-projects/${namespace}/${projectname}/devices`;
  const previousDeviceLink = `/iot-projects/${namespace}/${projectname}/devices/${deviceid}/device-info`;
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

  const onSave = (detail: string) => {
    //TODO: Add query to save iot device
    history.push(deviceListRouteLink);
  };

  const onLeave = () => {
    history.push(previousDeviceLink);
  };

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
        deviceDetail={deviceDetail}
        setDeviceDetail={setDeviceDetail}
        onLeave={onLeave}
        onSave={onSave}
        allowTemplate={false}
      />
    </Page>
  );
}

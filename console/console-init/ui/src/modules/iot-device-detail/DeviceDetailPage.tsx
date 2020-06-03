/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useMemo } from "react";
import { useParams } from "react-router";
import { Link } from "react-router-dom";
import { useQuery } from "@apollo/react-hooks";
import {
  PageSection,
  Breadcrumb,
  BreadcrumbItem,
  PageSectionVariants
} from "@patternfly/react-core";
import {
  Loading,
  useA11yRouteChange,
  useBreadcrumb,
  useDocumentTitle
} from "use-patternfly";
import {
  DeviceDetailHeader,
  DeviceDetailNavigation
} from "modules/iot-device-detail/components";
import { RETURN_IOT_DEVICE_DETAIL } from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { Routes } from "./Routes";

export default function DeviceDetailPage() {
  const { projectname, namespace, deviceid, subList } = useParams();
  useDocumentTitle("Device Details");
  useA11yRouteChange();
  /**
   * TODO: change link path
   */

  const breadcrumb = useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="cdetail-link-home" to={"/"}>
            Home
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link
            id="device-detail-link"
            to={`/iot-project/${namespace}/${projectname}/devices`}
          >
            {projectname}
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem isActive={true}>{deviceid}</BreadcrumbItem>
      </Breadcrumb>
    ),
    [projectname, namespace, deviceid]
  );

  useBreadcrumb(breadcrumb);

  const { loading, data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, deviceid)
  );
  const { devices } = data || {};
  const { enabled, deviceId } = devices?.devices[0] || {};

  if (loading) return <Loading />;

  const onEditMetadata = () => {
    /**
     * TODO: implement edit metadata logic
     */
  };

  const onEditDeviceInJson = () => {
    /**
     * TODO: edit device in json
     */
  };

  const onDeleteDevice = () => {
    /**
     * TODO: implement delete device logic
     */
  };

  const onCloneDevice = () => {
    /**
     * TODO: implement clone device logic
     */
  };

  const onChangeDeviceStatus = () => {
    /**
     * TODO: implement change device status logic
     */
  };

  return (
    <>
      <PageSection variant={PageSectionVariants.light}>
        <DeviceDetailHeader
          deviceName={deviceId}
          addedDate="2019-11-25T05:24:05.755Z"
          lastTimeSeen="2019-11-25T05:24:05.755Z"
          onEditMetadata={onEditMetadata}
          onEditDeviceInJson={onEditDeviceInJson}
          onChange={onChangeDeviceStatus}
          onDelete={onDeleteDevice}
          onClone={onCloneDevice}
          deviceStatus={enabled}
        />
        <DeviceDetailNavigation activeItem={subList || "deviceinfo"} />
      </PageSection>
      <PageSection>
        <Routes />
      </PageSection>
    </>
  );
}

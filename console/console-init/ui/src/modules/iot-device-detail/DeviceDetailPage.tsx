/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useMemo } from "react";
import { useParams } from "react-router";
import { Link } from "react-router-dom";
import { useHistory } from "react-router-dom";
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
import {
  RETURN_IOT_DEVICE_DETAIL,
  DELETE_IOT_DEVICE
} from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { Routes } from "./Routes";
import { useStoreContext, MODAL_TYPES, types } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import { NoDataFound } from "components";
import { ActionManager } from "modules/iot-device-detail/components";
import "./pf-overrides.css";

export default function DeviceDetailPage() {
  const { projectname, namespace, deviceid, subList } = useParams();
  const routeLink = `/iot-projects/${namespace}/${projectname}/devices`;
  const { dispatch, state } = useStoreContext();
  const { actionType } = state?.device || {};
  const history = useHistory();
  useDocumentTitle("Device Details");
  useA11yRouteChange();

  const { loading, data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, deviceid)
  );

  const changePageState = (deleteIotDevice: boolean) => {
    if (deleteIotDevice) {
      history.push(routeLink);
    }
  };
  /**
   * TODO: add refetchQueries
   */
  const refetchQueries: string[] = [""];
  const [setIotDeviceQueryVariables] = useMutationQuery(
    DELETE_IOT_DEVICE,
    refetchQueries,
    undefined,
    changePageState
  );

  const { devices } = data || {
    devices: { total: 0, devices: [] }
  };
  const { enabled, deviceId, viaGateway, credentials } =
    devices?.devices[0] || {};
  const parseCredentials = credentials && JSON.parse(credentials);

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
            to={`/iot-projects/${namespace}/${projectname}/devices`}
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

  if (loading) return <Loading />;

  const NoRecordFound = () => {
    return (
      <NoDataFound type={"Device"} name={deviceid} routeLink={routeLink} />
    );
  };

  /**
   * render NoRecordFound component in case current device don't exist
   */
  if (devices?.total <= 0 || devices?.devices?.length <= 0) {
    return <NoRecordFound />;
  }

  const onConfirmDeleteDevice = async () => {
    const variable = {
      iotproject: projectname,
      deviceId: [deviceid]
    };
    await setIotDeviceQueryVariables(variable);
  };

  const onDeleteDevice = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_IOT_DEVICE,
      modalProps: {
        onConfirm: onConfirmDeleteDevice,
        selectedItems: [deviceId],
        option: "Delete",
        detail: `Are you sure you want to delete this device: ${deviceId} ?`,
        header: "Delete this device ?"
      }
    });
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
      {actionType ? (
        <PageSection variant={PageSectionVariants.light}>
          <ActionManager actionType={actionType} viaGateway={viaGateway} />
        </PageSection>
      ) : (
        <>
          <PageSection variant={PageSectionVariants.light}>
            <DeviceDetailHeader
              deviceName={deviceId}
              addedDate="2019-11-25T05:24:05.755Z"
              lastTimeSeen="2019-11-25T05:24:05.755Z"
              onChange={onChangeDeviceStatus}
              onDelete={onDeleteDevice}
              onClone={onCloneDevice}
              deviceStatus={enabled}
              credentials={parseCredentials}
              viaGateway={viaGateway}
            />
            <DeviceDetailNavigation activeItem={subList || "device-info"} />
          </PageSection>
          <PageSection>
            <Routes />
          </PageSection>
        </>
      )}
    </>
  );
}

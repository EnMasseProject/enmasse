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
  DELETE_IOT_DEVICE,
  TOGGLE_IOT_DEVICE_STATUS
} from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { Routes } from "./Routes";
import { useStoreContext, MODAL_TYPES, types } from "context-state-reducer";
import { useMutationQuery } from "hooks";
import { NoDataFound } from "components";
import { ActionManager } from "modules/iot-device-detail/components";
import { DialogTypes, FetchPolicy } from "constant";
import {
  getDetailForDialog,
  getHeaderForDialog
} from "modules/iot-device/utils";
import "./pf-overrides.css";
import { StyleSheet, css } from "aphrodite";
import { getDeviceConnectionType } from "utils";

const styles = StyleSheet.create({
  no_bottom_padding: {
    paddingBottom: 0
  }
});

export default function DeviceDetailPage() {
  const { projectname, namespace, deviceid, subList } = useParams();
  const routeLink = `/iot-projects/${namespace}/${projectname}/devices`;
  const { dispatch, state } = useStoreContext();
  const { actionType } = state?.device || {};
  const history = useHistory();
  useDocumentTitle("Device Details");
  useA11yRouteChange();

  const { loading, data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, namespace, deviceid),
    { fetchPolicy: FetchPolicy.NETWORK_ONLY }
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

  const [
    setUpdateDeviceQueryVariables
  ] = useMutationQuery(TOGGLE_IOT_DEVICE_STATUS, ["iot_device_detail"]);

  const { devices } = data || {
    devices: { total: 0, devices: [] }
  };
  const { registration, deviceId, credentials, status } =
    devices?.devices[0] || {};
  const { enabled, via = [], viaGroups = [], memberOf = [] } = registration || {
    memberOf: [],
    viaGroups: [],
    via: []
  };
  const parseCredentials = credentials && JSON.parse(credentials);
  const viaGateway = via?.length > 0 || viaGroups?.length > 0;

  const breadcrumb = useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="home-link" to={"/"}>
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
      iotproject: {
        name: projectname,
        namespace
      },
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
        header: "Delete this device ?",
        confirmButtonLabel: "Delete",
        iconType: "danger"
      }
    });
  };

  const onCloneDevice = () => {
    /**
     * TODO: implement clone device logic
     */
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CLONE_DEVICE,
      modalProps: {
        projectname: projectname,
        namespace: namespace,
        deviceid: deviceId
      }
    });
  };

  const onConfirmToggleDeviceStatus = async (data: any) => {
    const variable = {
      a: { name: projectname, namespace },
      b: [data.deviceId],
      status: data.status
    };
    await setUpdateDeviceQueryVariables(variable);
  };

  const onChangeDeviceStatus = (status: boolean) => {
    const dialogType: string = status
      ? DialogTypes.ENABLE
      : DialogTypes.DISABLE;
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.UPDATE_DEVICE_STATUS,
      modalProps: {
        onConfirm: onConfirmToggleDeviceStatus,
        selectedItems: [deviceId],
        option: dialogType,
        data: { deviceId, status },
        detail: getDetailForDialog([{ deviceId }], dialogType),
        header: getHeaderForDialog([{ deviceId }], dialogType),
        confirmButtonLabel: dialogType
      }
    });
  };

  return (
    <>
      {actionType ? (
        <PageSection variant={PageSectionVariants.light}>
          <ActionManager actionType={actionType} viaGateway={viaGateway} />
        </PageSection>
      ) : (
        <>
          <PageSection
            variant={PageSectionVariants.light}
            className={css(styles.no_bottom_padding)}
          >
            <DeviceDetailHeader
              deviceName={deviceId}
              addedDate={status?.created || ""}
              lastSeen={status?.lastSeen || ""}
              onChange={onChangeDeviceStatus}
              onDelete={onDeleteDevice}
              onClone={onCloneDevice}
              deviceStatus={enabled}
              credentials={parseCredentials}
              viaGateway={viaGateway}
              connectiontype={getDeviceConnectionType(
                viaGateway,
                parseCredentials
              )}
              memberOf={memberOf}
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

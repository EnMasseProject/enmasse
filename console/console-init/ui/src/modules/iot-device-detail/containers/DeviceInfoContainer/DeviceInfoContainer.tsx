/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_DEVICE_DETAIL } from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { DeviceInfo } from "modules/iot-device-detail/components";
//import { useStoreContext } from "context-state-reducer";
import { ErrorState } from "modules/iot-device-detail/components";
import { useMutationQuery } from "hooks";
import { DELETE_CREDENTIALS_FOR_IOT_DEVICE } from "graphql-module/queries";

export interface IDeviceInfoContainerProps {
  id: string;
}

export const DeviceInfoContainer: React.FC<IDeviceInfoContainerProps> = ({
  id
}) => {
  // const { dispatch } = useStoreContext();
  const { projectname, deviceid, namespace } = useParams();

  const { data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, namespace, deviceid)
  );

  //const [setCredentialStatusQueryVaribles]=useMutationQuery();
  //const [setUpdatePasswordQueryVaribles]=useMutationQuery();
  const refetchQueries = ["iot_device_detail"];
  const [setDeleteCredentialsQueryVariables] = useMutationQuery(
    DELETE_CREDENTIALS_FOR_IOT_DEVICE,
    refetchQueries
  );

  const { credentials, jsonData, viaGateway } = data?.devices?.devices[0] || {};
  const credentialsJson = credentials && JSON.parse(credentials);
  const deviceJson = jsonData && JSON.parse(jsonData);

  const metadetaJson = {
    default: deviceJson?.default,
    ext: deviceJson?.ext
  };

  const onChangeCredentialStatus = async (authId: string) => {
    /**
     * TODO: add query for update credential status i.e. enabled/disabled
     */
    //await setCredentialStatusQueryVaribles("");
  };

  const onConfirmSecretPassword = async (formdata: any, secretId: string) => {
    /**
     * TODO: add query for update password
     */
    //await setUpdatePasswordQueryVaribles("");
  };

  const deleteGateways = () => {
    /**
     * TODO: add delete gataways query
     */
  };

  const deleteCredentials = async () => {
    const variable = {
      iotproject: {
        name: projectname,
        namespace
      },
      deviceId: deviceid
    };
    await setDeleteCredentialsQueryVariables(variable);
  };

  const getErrorState = () => {
    let errorState = "";
    if (
      Array.isArray(credentialsJson) &&
      credentialsJson?.length > 0 &&
      viaGateway
    ) {
      errorState = ErrorState.CONFLICTING;
    } else if (!(credentialsJson?.length > 0) && !viaGateway) {
      errorState = ErrorState.MISSING;
    }
    return errorState;
  };

  return (
    <DeviceInfo
      id={id}
      deviceList={deviceJson?.via}
      metadataList={metadetaJson}
      credentials={credentialsJson}
      onChangeStatus={onChangeCredentialStatus}
      onConfirmPassword={onConfirmSecretPassword}
      errorState={getErrorState()}
      deleteGateways={deleteGateways}
      deleteCredentials={deleteCredentials}
    />
  );
};

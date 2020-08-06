/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_IOT_DEVICE_DETAIL,
  SET_IOT_CREDENTIAL_FOR_DEVICE
} from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { DeviceInfo } from "modules/iot-device-detail/components";
import { ErrorState } from "modules/iot-device-detail/components";
import { useMutationQuery } from "hooks";
import { DELETE_CREDENTIALS_FOR_IOT_DEVICE } from "graphql-module/queries";
import { ICredential } from "modules/iot-device/components";
import { FetchPolicy } from "constant";

export interface IDeviceInfoContainerProps {
  id: string;
}

export const DeviceInfoContainer: React.FC<IDeviceInfoContainerProps> = ({
  id
}) => {
  const { projectname, deviceid, namespace } = useParams();
  const queryResolver = `
    devices{
      registration{
        via
        ext
        defaults
      } 
      credentials           
    }
  `;

  const { data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, namespace, deviceid, queryResolver),
    {
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    }
  );

  const refetchQueries = ["iot_device_detail"];
  const [setDeleteCredentialsQueryVariables] = useMutationQuery(
    DELETE_CREDENTIALS_FOR_IOT_DEVICE,
    refetchQueries
  );

  const [
    setUpdateCredentialQueryVariable
  ] = useMutationQuery(SET_IOT_CREDENTIAL_FOR_DEVICE, ["iot_device_detail"]);

  const { credentials, registration } = data?.devices?.devices[0] || {};
  const { ext: extString, via, defaults } = registration || {};
  const parsecredentials = credentials && JSON.parse(credentials);

  const ext = extString && JSON.parse(extString);
  const defaultObject = defaults && JSON.parse(defaults);

  const viaGateway = via && via?.length > 0;

  const metadetaJson = {
    default: defaultObject,
    ext
  };

  const onConfirmCredentialsStatus = async (data: any) => {
    const { authId, status, credentialType } = data || {};
    const newCredentials = [...parsecredentials];
    const crdIndex: number = newCredentials?.findIndex(
      (crd: ICredential) =>
        crd["auth-id"] === authId && crd?.type === credentialType
    );
    if (crdIndex >= 0) {
      newCredentials[crdIndex]["enabled"] = status;
      const variable = {
        iotproject: { name: projectname, namespace },
        deviceId: deviceid,
        jsonData: JSON.stringify(newCredentials)
      };
      await setUpdateCredentialQueryVariable(variable);
    }
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
      Array.isArray(parsecredentials) &&
      parsecredentials?.length > 0 &&
      viaGateway
    ) {
      errorState = ErrorState.CONFLICTING;
    } else if (!(parsecredentials?.length > 0) && viaGateway === false) {
      errorState = ErrorState.MISSING;
    }
    return errorState;
  };

  return (
    <DeviceInfo
      id={id}
      deviceList={via}
      metadataList={metadetaJson}
      credentials={parsecredentials}
      errorState={getErrorState()}
      deleteGateways={deleteGateways}
      deleteCredentials={deleteCredentials}
      onConfirmCredentialsStatus={onConfirmCredentialsStatus}
    />
  );
};

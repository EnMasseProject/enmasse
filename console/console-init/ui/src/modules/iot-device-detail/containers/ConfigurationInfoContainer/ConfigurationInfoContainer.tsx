/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_IOT_CREDENTIALS,
  SET_IOT_CREDENTIAL_FOR_DEVICE
} from "graphql-module/queries";
import { ICredentialsReponse } from "schema";
import { ConfigurationInfo } from "modules/iot-device-detail/components";
import {
  getCredentialFilterType,
  getCredentialFilterValue
} from "modules/iot-device-detail/utils";
import { FetchPolicy } from "constant";
import { ICredential } from "modules/iot-device/components";
import { useMutationQuery } from "hooks";

export const ConfigurationInfoContainer: React.FC<{ id: string }> = ({
  id
}) => {
  const { projectname, deviceid, namespace } = useParams();
  const [filterType, setFilterType] = useState<string>("enabled");
  const [filterValue, setFilterValue] = useState<string>("");

  const propertyName = getCredentialFilterType(filterType, filterValue);
  const value = getCredentialFilterValue(filterType, filterValue);

  const { data } = useQuery<ICredentialsReponse>(
    RETURN_IOT_CREDENTIALS(
      projectname,
      namespace,
      deviceid,
      propertyName,
      filterType,
      value
    ),
    {
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    }
  );

  const [
    setUpdateCredentialQueryVariable
  ] = useMutationQuery(SET_IOT_CREDENTIAL_FOR_DEVICE, ["iot_credentials"]);

  const { credentials } = data?.credentials || {};
  const parsecredentials = credentials && JSON.parse(credentials);

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

  return (
    <ConfigurationInfo
      id={id}
      credentials={parsecredentials}
      setFilterType={setFilterType}
      setFilterValue={setFilterValue}
      filterType={filterType}
      filterValue={filterValue}
      onConfirmCredentialsStatus={onConfirmCredentialsStatus}
    />
  );
};

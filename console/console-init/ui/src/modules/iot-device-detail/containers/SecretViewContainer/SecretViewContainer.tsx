/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { useApolloClient } from "@apollo/react-hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import {
  SecretsView,
  ISecretsViewProps
} from "modules/iot-device-detail/components";
import { ICredential } from "modules/iot-device/components";
import {
  RETURN_IOT_CREDENTIALS,
  SET_IOT_CREDENTIAL_FOR_DEVICE
} from "graphql-module/queries";
import { FetchPolicy } from "constant";
import { useMutationQuery } from "hooks";
import { ICredentialsReponse } from "schema";

export const SecretsViewContainer: React.FC<ISecretsViewProps> = ({
  id,
  secrets,
  heading,
  authId,
  credentialType,
  enableActions
}) => {
  const { dispatch } = useStoreContext();
  const client = useApolloClient();
  const { projectname, deviceid, namespace } = useParams();

  const [setUpdateCredentialQueryVariable] = useMutationQuery(
    SET_IOT_CREDENTIAL_FOR_DEVICE
  );

  const getCredentials = async () => {
    const response = await client.query<ICredentialsReponse>({
      query: RETURN_IOT_CREDENTIALS(projectname, namespace, deviceid),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    const credentials = response?.data?.credentials?.credentials;
    return credentials;
  };

  const updateSecretPassword = async (formData: any) => {
    const { authId, credentialType, secretId, password } = formData || {};
    const credentials = await getCredentials();
    const parseCredentials = credentials && JSON.parse(credentials);
    const crdIndex = parseCredentials?.findIndex(
      (crd: ICredential) =>
        crd["auth-id"] === authId && crd?.type === credentialType
    );
    if (crdIndex >= 0) {
      const secretIndex = parseCredentials[crdIndex]["secrets"]?.findIndex(
        (scrt: any) => scrt?.id === secretId
      );
      if (secretIndex >= 0) {
        parseCredentials[crdIndex]["secrets"][secretIndex][
          "pwd-hash"
        ] = password;
      }
    }
    return parseCredentials;
  };

  const onConfirmPassword = async (formData: any) => {
    const credentials = await updateSecretPassword(formData);
    const variable = {
      iotproject: { name: projectname, namespace },
      deviceId: deviceid,
      jsonData: JSON.stringify(credentials)
    };
    await setUpdateCredentialQueryVariable(variable);
  };

  const onOpenPasswordDialog = (formData: any) => {
    const { authId, credentialType, secretId } = formData || {};
    dispatch &&
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.UPDATE_PASSWORD,
        modalProps: {
          credentialProps: { authId, credentialType, secretId },
          onConfirm: onConfirmPassword
        }
      });
  };

  return (
    <SecretsView
      id={id}
      secrets={secrets}
      heading={heading}
      authId={authId}
      credentialType={credentialType}
      onOpenUpdatePasswordDialog={onOpenPasswordDialog}
      enableActions={enableActions}
    />
  );
};

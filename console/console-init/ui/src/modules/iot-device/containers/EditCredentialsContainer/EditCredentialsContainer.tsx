/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Title,
  Flex,
  FlexItem,
  Button,
  ButtonVariant,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { AddCredential, ICredential } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";
import { RETURN_IOT_CREDENTIALS } from "graphql-module/queries";
import { ICredentialsReponse } from "schema";
import { Loading } from "use-patternfly";
import { OperationType, FetchPolicy } from "constant";
import {
  deserializeCredentials,
  serializeCredentials
} from "modules/iot-device/utils";
import { useMutationQuery } from "hooks";
import { SET_IOT_CREDENTIAL_FOR_DEVICE } from "graphql-module/queries";
import styles from "./edit-credentials.module.css";

export const EditCredentialsContainer = () => {
  const { dispatch } = useStoreContext();
  const { projectname, deviceid, namespace } = useParams();
  const [credentialList, setCredentialList] = useState<ICredential[]>();

  const { data, loading } = useQuery<ICredentialsReponse>(
    RETURN_IOT_CREDENTIALS(projectname, namespace, deviceid),
    {
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    }
  );
  const { credentials } = data?.credentials || {};
  const parsecredentials = credentials && JSON.parse(credentials);
  const [setCredentialQueryVariable] = useMutationQuery(
    SET_IOT_CREDENTIAL_FOR_DEVICE,
    ["iot_device_detail"],
    undefined,
    resetActionType
  );

  if (loading) {
    return <Loading />;
  }

  function resetActionType() {
    dispatch({ type: types.RESET_DEVICE_ACTION_TYPE });
  }

  const onSave = async () => {
    const variable = {
      iotproject: { name: projectname, namespace },
      deviceId: deviceid,
      jsonData: serializeCredentials(credentialList)
    };
    await setCredentialQueryVariable(variable);
  };

  const onCancel = () => {
    resetActionType();
  };

  const deserializeCredentialList = deserializeCredentials(parsecredentials);

  return (
    <Grid>
      <GridItem span={6}>
        <Title headingLevel="h2" size="xl" className={styles.marginLeft}>
          Edit credentials
        </Title>
        <br />
        <AddCredential
          credentials={deserializeCredentialList}
          operation={OperationType.EDIT}
          setCredentialList={setCredentialList}
        />
        <br />
        <Flex className={styles.marginLeft}>
          <FlexItem>
            <Button
              id="edit-credentials-save-button"
              variant={ButtonVariant.primary}
              onClick={onSave}
            >
              Save
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              id="edit-credentials-cancel-button"
              variant={ButtonVariant.secondary}
              onClick={onCancel}
            >
              Cancel
            </Button>
          </FlexItem>
        </Flex>
      </GridItem>
    </Grid>
  );
};

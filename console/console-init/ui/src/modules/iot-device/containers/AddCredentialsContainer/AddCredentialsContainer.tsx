/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import { StyleSheet, css } from "aphrodite";
import {
  Title,
  Flex,
  FlexItem,
  Button,
  ButtonVariant,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { AddCredential, ICredential } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";
import { SET_IOT_CREDENTIAL_FOR_DEVICE } from "graphql-module/queries";
import { useMutationQuery } from "hooks";
import { serializeCredentials } from "modules/iot-device/utils";

const styles = StyleSheet.create({
  header: {
    marginLeft: 25
  },
  button_alignment: {
    marginLeft: 25
  }
});

export const AddCredentialsContainer = () => {
  const { projectname, deviceid, namespace } = useParams();
  const { dispatch } = useStoreContext();
  const [credentials, setCredentials] = useState<ICredential[]>();
  const [setCredentialQueryVariable] = useMutationQuery(
    SET_IOT_CREDENTIAL_FOR_DEVICE,
    ["iot_device_detail"],
    undefined,
    resetActionType
  );

  function resetActionType() {
    dispatch({ type: types.RESET_DEVICE_ACTION_TYPE });
  }

  const onSave = async () => {
    const variable = {
      iotproject: { name: projectname, namespace },
      deviceId: deviceid,
      jsonData: serializeCredentials(credentials)
    };
    await setCredentialQueryVariable(variable);
  };

  const onCancel = () => {
    resetActionType();
  };

  return (
    <Grid hasGutter>
      <GridItem span={6}>
        <Title headingLevel="h2" size="xl" className={css(styles.header)}>
          Add credentials
        </Title>
        <br />
        <AddCredential setCredentialList={setCredentials} />
        <br />
        <br />
        <Flex className={css(styles.button_alignment)}>
          <FlexItem>
            <Button
              id="ac-save-credentials-button"
              variant={ButtonVariant.primary}
              onClick={onSave}
            >
              Save
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              id="ac-cancel-credentials-button"
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

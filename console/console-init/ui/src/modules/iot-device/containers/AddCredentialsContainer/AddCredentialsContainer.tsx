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

export const AddCredentialsContainer: React.FC<{
  onCancel: () => void;
}> = ({ onCancel }) => {
  const { projectname, deviceid, namespace } = useParams();
  const [credentials, setCredentials] = useState<ICredential[]>();
  const [setCredentialQueryVariable] = useMutationQuery(
    SET_IOT_CREDENTIAL_FOR_DEVICE,
    ["iot_device_detail"],
    undefined,
    onCancel
  );

  const onSave = async () => {
    const variable = {
      iotproject: { name: projectname, namespace },
      deviceId: deviceid,
      jsonData: serializeCredentials(credentials)
    };
    await setCredentialQueryVariable(variable);
  };

  const shouldDisabledSaveButton = () => {
    const credential = credentials?.[0];
    if (
      credential?.["auth-id"]?.trim() &&
      credential?.secrets?.[0]?.["pwd-hash"]?.trim()
    ) {
      return false;
    }
    return true;
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
              id="add-credentials-container-save-button"
              variant={ButtonVariant.primary}
              onClick={onSave}
              isDisabled={shouldDisabledSaveButton()}
            >
              Save
            </Button>
          </FlexItem>
          <FlexItem>
            <Button
              id="add-credentials-container-cancel-button"
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

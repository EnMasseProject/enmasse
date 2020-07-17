/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Title,
  Flex,
  FlexItem,
  Button,
  ButtonVariant,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { AddCredential } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";

export const EditCredentialsContainer = () => {
  const { dispatch } = useStoreContext();
  /**
   * TODO: write query to get credetials
   */

  const resetActionType = () => {
    dispatch({ type: types.RESET_DEVICE_ACTION_TYPE });
  };

  const onSave = () => {
    /**
     * TODO: implement save query
     */
    resetActionType();
  };

  const onCancel = () => {
    resetActionType();
  };

  return (
    <>
      <Title headingLevel="h2" size="xl">
        Edit credentials
      </Title>
      <br />
      <Grid>
        <GridItem span={6}>
          <AddCredential />
          <br />
          <br />
          <Flex>
            <FlexItem>
              <Button
                id="ec-save-credentials-button"
                variant={ButtonVariant.primary}
                onClick={onSave}
              >
                Save
              </Button>
            </FlexItem>
            <FlexItem>
              <Button
                id="ec-cancel-credentials-button"
                variant={ButtonVariant.secondary}
                onClick={onCancel}
              >
                Cancel
              </Button>
            </FlexItem>
          </Flex>
        </GridItem>
      </Grid>
    </>
  );
};

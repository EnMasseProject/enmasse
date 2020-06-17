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
  ButtonVariant
} from "@patternfly/react-core";
import { AddCredential } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";

export const AddCredentialsContainer = () => {
  const { dispatch } = useStoreContext();

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
      <Title size={"2xl"}>Add credentials</Title>
      <br />
      <AddCredential />
      <br />
      <br />
      <Flex>
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
    </>
  );
};

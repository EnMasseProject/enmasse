/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
//import {useQuery} from "@apollo/react-hooks";
import { Flex, FlexItem, Button, ButtonVariant } from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { AddGateways } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";

const styles = StyleSheet.create({
  button_padding: {
    paddingTop: 100
  }
});

export const AddGatewaysContainer = () => {
  const { deviceid } = useParams();
  const { dispatch } = useStoreContext();
  const [gateways, addGateways] = useState<string[]>([]);

  const getGateways = (gateway: string[]) => {
    addGateways(gateway);
  };

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
      <AddGateways
        header={`Add gateways of device ${deviceid}`}
        returnGateways={getGateways}
      />
      <Flex className={css(styles.button_padding)}>
        <FlexItem>
          <Button
            id="ag-save-gateways-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
          >
            Save
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="ad-cancel-gateways-button"
            variant={ButtonVariant.link}
            onClick={onCancel}
          >
            Cancel
          </Button>
        </FlexItem>
      </Flex>
    </>
  );
};

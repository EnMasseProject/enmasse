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

const styles = StyleSheet.create({
  min_height: {
    "min-height": "35rem"
  }
});

export const AddGatewaysContainer: React.FC<{
  onCancel: () => void;
}> = ({ onCancel }) => {
  const { deviceid } = useParams();
  const [gatewayDevices, addGatewayDevices] = useState<string[]>([]);
  const [gatewayGroups, addGatewayGroups] = useState<string[]>([]);

  const onSave = () => {
    /**
     * TODO: implement save query
     */
    onCancel();
  };

  const shouldDisabledSaveBUtton = () => {
    if (gatewayGroups?.length > 0 || gatewayDevices?.length > 0) {
      return false;
    }
    return true;
  };

  return (
    <>
      <div className={css(styles.min_height)}>
        <AddGateways
          header={`Add gateways of device ${deviceid}`}
          returnGatewayDevices={addGatewayDevices}
          returnGatewayGroups={addGatewayGroups}
        />
      </div>
      <Flex>
        <FlexItem>
          <Button
            id="add-gateways-container-save-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
            isDisabled={shouldDisabledSaveBUtton()}
          >
            Save
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="add-gateways-container-cancel-button"
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

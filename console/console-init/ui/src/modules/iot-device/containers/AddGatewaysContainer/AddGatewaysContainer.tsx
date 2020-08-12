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
  button_padding: {
    paddingTop: 100
  }
});

export const AddGatewaysContainer: React.FC<{
  onCancel: () => void;
}> = ({ onCancel }) => {
  const { deviceid } = useParams();
  const [gatewayDevices, addGatewayDevices] = useState<string[]>([]);
  const [gatewayGroups, addGatewayGroups] = useState<string[]>([]);

  const getGatewayDevices = (gateway: string[]) => {
    addGatewayDevices(gateway);
  };

  const getGatewayGroups = (gateway: string[]) => {
    addGatewayGroups(gateway);
  };

  const onSave = () => {
    /**
     * TODO: implement save query
     */
    onCancel();
  };

  return (
    <>
      <AddGateways
        header={`Add gateways of device ${deviceid}`}
        returnGatewayDevices={getGatewayDevices}
        returnGatewayGroups={getGatewayGroups}
      />
      <Flex className={css(styles.button_padding)}>
        <FlexItem>
          <Button
            id="add-gateways-container-save-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
          >
            Save
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="add-gateways-container-cancel-button"
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

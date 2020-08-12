/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { Flex, FlexItem, Button, ButtonVariant } from "@patternfly/react-core";
import { AddGatewayGroupMembership } from "modules/iot-device/components";

export const EditGatewayGroupMembershipContainer: React.FC<{
  onCancel: () => void;
}> = ({ onCancel }) => {
  const onSave = () => {};

  return (
    <>
      <AddGatewayGroupMembership id="edit-gateway-group-membership" />
      <Flex>
        <FlexItem>
          <Button
            id="connected-directly-next-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
          >
            Save
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="connected-directly-cancel-button"
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

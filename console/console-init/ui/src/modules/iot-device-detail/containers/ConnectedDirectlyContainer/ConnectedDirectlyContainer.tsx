/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Flex,
  FlexItem,
  Button,
  ButtonVariant,
  Grid,
  GridItem
} from "@patternfly/react-core";
import {
  AddGatewayGroupMembership,
  IAddGatewayGroupMembershipProps
} from "modules/iot-device/components";
import { AddCredential } from "modules/iot-device/components";
import { useStoreContext, types } from "context-state-reducer";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  grid_alignment: {
    "padding-left": "3rem"
  },
  popover_alignment: {
    "padding-left": "0px"
  },
  button_alignment: {
    "padding-left": "1rem"
  }
});

interface IAddGatewayGroupWrapperProps
  extends Pick<
    IAddGatewayGroupMembershipProps,
    "returnGatewayGroups" | "gatewayGroups"
  > {
  onCancel: () => void;
  onNext: () => void;
}

enum Steps {
  AddGroup,
  AddCredential
}

const AddGatewayGroupWrapper: React.FC<IAddGatewayGroupWrapperProps> = ({
  onCancel,
  onNext,
  returnGatewayGroups,
  gatewayGroups
}) => {
  return (
    <>
      <AddGatewayGroupMembership
        id="connected-directly-add-gateway-group"
        returnGatewayGroups={returnGatewayGroups}
        gatewayGroups={gatewayGroups}
      />
      <Flex>
        <FlexItem>
          <Button
            id="connected-directly-next-button"
            variant={ButtonVariant.primary}
            onClick={onNext}
          >
            Next
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

interface IAddCredentialWrapperProps
  extends Pick<IAddGatewayGroupWrapperProps, "onCancel"> {
  onSave: () => void;
  onBack: () => void;
}

const AddCredentialWrapper: React.FC<IAddCredentialWrapperProps> = ({
  onSave,
  onCancel,
  onBack
}) => {
  return (
    <>
      <Grid>
        <GridItem span={6}>
          <AddCredential id="connected-directly-add-credentials" />
        </GridItem>
      </Grid>
      <br />
      <br />
      <Flex className={css(styles.button_alignment)}>
        <FlexItem>
          <Button
            id="connected-directly-back-button"
            variant={ButtonVariant.secondary}
            onClick={onBack}
          >
            Back
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="connected-directly-save-button"
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

export const ConnectedDirectlyContainer = () => {
  const { dispatch } = useStoreContext();
  const [step, setStep] = useState(Steps.AddGroup);
  const [gatewayGroups, setGatewayGroups] = useState<string[]>([]);

  const onCancel = () => {
    dispatch({
      type: types.RESET_DEVICE_ACTION_TYPE
    });
  };

  const onNext = () => {
    setStep(Steps.AddCredential);
  };

  const onBack = () => {
    setStep(Steps.AddGroup);
  };

  const onSave = () => {
    /**
     * Todo: integrate save query
     */
  };

  return (
    <div className={css(styles.grid_alignment)}>
      {step === Steps.AddGroup && (
        <AddGatewayGroupWrapper
          onCancel={onCancel}
          onNext={onNext}
          returnGatewayGroups={setGatewayGroups}
          gatewayGroups={gatewayGroups}
        />
      )}
      {step === Steps.AddCredential && (
        <AddCredentialWrapper
          onBack={onBack}
          onSave={onSave}
          onCancel={onCancel}
        />
      )}
    </div>
  );
};

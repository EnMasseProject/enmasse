/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
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
import { StyleSheet, css } from "aphrodite";
import { PageJourney } from "constant";

const styles = StyleSheet.create({
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
  onBack: () => void;
  onSave: () => void;
}

const AddGatewayGroupWrapper: React.FC<IAddGatewayGroupWrapperProps> = ({
  onCancel,
  onBack,
  onSave,
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
            id="connected-directly-save-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
          >
            Save
          </Button>
        </FlexItem>
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
  onNext: () => void;
}

const AddCredentialWrapper: React.FC<IAddCredentialWrapperProps> = ({
  onCancel,
  onNext
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

interface IConnectedDirectlyContainerProps {
  setPageJourney: (
    journey: PageJourney.AddCredential | PageJourney.AddGatewayGroupMembership
  ) => void;
  onCancel: () => void;
}

export const ConnectedDirectlyContainer: React.FC<IConnectedDirectlyContainerProps> = ({
  setPageJourney,
  onCancel
}) => {
  const [step, setStep] = useState(PageJourney.AddCredential);
  const [gatewayGroups, setGatewayGroups] = useState<string[]>([]);

  useEffect(() => {
    setPageJourney(step);
  }, [step]);

  const onNext = () => {
    setStep(PageJourney.AddGatewayGroupMembership);
  };

  const onBack = () => {
    setStep(PageJourney.AddCredential);
  };

  const onSave = () => {
    /**
     * Todo: integrate save query
     */
  };

  return (
    <>
      {step === PageJourney.AddGatewayGroupMembership && (
        <AddGatewayGroupWrapper
          onCancel={onCancel}
          onBack={onBack}
          onSave={onSave}
          returnGatewayGroups={setGatewayGroups}
          gatewayGroups={gatewayGroups}
        />
      )}
      {step === PageJourney.AddCredential && (
        <AddCredentialWrapper onNext={onNext} onCancel={onCancel} />
      )}
    </>
  );
};

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Title,
  TextInput,
  FormGroup,
  Form,
  Button,
  FlexItem,
  Flex,
  Chip,
  ChipGroup,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { DeviceListAlert } from "modules/device/components";
import { deviceIDRegExp } from "types/Configs";

export interface IGatewayProps {
  addedGateways: string[];
  removeGateway: (id: string) => void;
  appendGateway: (deviceId: string) => void;
}

export const AddGateway: React.FunctionComponent<IGatewayProps> = ({
  addedGateways,
  removeGateway,
  appendGateway
}) => {
  const [inputID, setInputID] = useState<string>();
  const [isValid, setIsValid] = useState<boolean>(false);

  const onClickAddGatewayBtn = () => {
    if (inputID) {
      appendGateway(inputID);
      setInputID("");
    }
  };

  const alertDescription = `In AMQ IoT, any existing device directly connected to cloud can turn
    to be a gateway by been added to other devices for their connections. Multiple gateways are supported.`;

  const onChangeID = (id: string) => {
    deviceIDRegExp.test(id) ? setIsValid(true) : setIsValid(false);
    setInputID(id);
  };

  return (
    <Grid>
      <GridItem span={8}>
        <Title id="ag-device-info-title" headingLevel="h3" size="2xl">
          Add permitted devices as gateways to this new device
        </Title>
        <br />
        <DeviceListAlert
          visible={true}
          isInline={true}
          variant={"info"}
          title="The concept of AMQ IoT gateway"
          description={alertDescription}
        />
        <br />
        <Form>
          <FormGroup
            label="Device ID of gateway"
            type="number"
            id="ag-form-group"
            isRequired
            helperTextInvalid="Age has to be a number"
            fieldId="device-id"
            isValid={true}
          >
            <Flex>
              <FlexItem>
                <TextInput
                  isValid={true}
                  value={inputID}
                  id="ag-text-input-id"
                  aria-label="Input device ID"
                  aria-describedby="Input device ID"
                  onChange={onChangeID}
                />
              </FlexItem>
              <FlexItem>
                <Button
                  isDisabled={!isValid}
                  id="ag-btn-add-device-gateway"
                  variant="secondary"
                  onClick={onClickAddGatewayBtn}
                >
                  Add
                </Button>
              </FlexItem>
            </Flex>
          </FormGroup>
        </Form>
        <br />
        {addedGateways.length > 0 && (
          <>
            <Title id="ag-title-added" headingLevel="h6" size="md">
              Added Gateways
            </Title>
            <br />
            <ChipGroup id="ag-chip-group">
              {addedGateways.map((gateway: string) => (
                <Chip
                  key={gateway}
                  id={`ag-device-chip-${gateway}`}
                  value={gateway}
                  onClick={() => removeGateway(gateway)}
                >
                  {gateway}
                </Chip>
              ))}
            </ChipGroup>
          </>
        )}
      </GridItem>
    </Grid>
  );
};

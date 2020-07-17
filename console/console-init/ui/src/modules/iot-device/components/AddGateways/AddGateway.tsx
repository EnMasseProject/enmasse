/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
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
import { DeviceListAlert } from "modules/iot-device/components";
import { deviceIDRegExp } from "types/Configs";

export interface IGatewaysProps {
  header?: string;
  gateways?: string[];
  returnGateways?: (gateways: string[]) => void;
}

export const AddGateways: React.FunctionComponent<IGatewaysProps> = ({
  header = "Add permitted devices as gateways to this new device",
  gateways: gatewayList = [],
  returnGateways
}) => {
  const [inputID, setInputID] = useState<string>();
  const [isValid, setIsValid] = useState<boolean>(false);
  const [gateways, setGateways] = useState<string[]>(gatewayList);

  useEffect(() => {
    returnGateways && returnGateways(gateways);
  }, [gateways]);

  const addGateway = () => {
    if (inputID) {
      setGateways([...gateways, inputID]);
      setInputID("");
    }
  };

  const removeGateway = (id: string) => {
    const idIndex = gateways.indexOf(id);
    gateways.splice(idIndex, 1);
    setGateways([...gateways]);
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
          {header}
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
            validated="default"
          >
            <Flex>
              <FlexItem>
                <TextInput
                  validated="default"
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
                  onClick={addGateway}
                >
                  Add
                </Button>
              </FlexItem>
            </Flex>
          </FormGroup>
        </Form>
        <br />
        {gateways.length > 0 && (
          <>
            <Title id="ag-title-added" headingLevel="h6" size="md">
              Added Gateways
            </Title>
            <br />
            <ChipGroup id="ag-chip-group">
              {gateways.map((gateway: string) => (
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

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
  Grid,
  Popover,
  DropdownPosition,
  ValidatedOptions
} from "@patternfly/react-core";
import { deviceIDRegExp } from "types/Configs";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { DropdownWithToggle, TypeAheadSelect } from "components";
import { ISelectOption } from "utils";
import "./pf-overrides.css";

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
  const [inputType, setInputType] = useState<string>("Device ID");
  const [selectedGateways, setSelectedGateways] = useState<any[]>([]);

  const typeOptions: ISelectOption[] = [
    {
      key: "deviceid",
      value: "Device ID",
      isDisabled: false,
      id: "add-gateway-type-dropdown-item-id"
    },
    {
      key: "gatewaygroup",
      value: "Gateway group name",
      isDisabled: false,
      id: "add-gateway-type-dropdown-item-group"
    }
  ];

  useEffect(() => {
    returnGateways && returnGateways(gateways);
  }, [gateways]);

  const addGateway = () => {
    if (inputID) {
      setGateways([...gateways, inputID]);
      setInputID("");
    }
    if (selectedGateways?.length) {
      setGateways([...gateways, ...selectedGateways]);
      setSelectedGateways([]);
    }
  };

  const removeGateway = (id: string) => {
    const idIndex = gateways.indexOf(id);
    gateways.splice(idIndex, 1);
    setGateways([...gateways]);
  };

  const onTypeSelect = (val: string) => {
    setInputType(val);
    setSelectedGateways([]);
    setInputID("");
  };

  const deviceIDValidationState = () => {
    if (inputID && inputID.length > 1 && !isValid)
      return ValidatedOptions.error;
    return ValidatedOptions.default;
  };

  const disableAddGatewayBtn = () => {
    if (
      selectedGateways?.length > 0 ||
      (inputID && inputID?.trim() !== "" && isValid)
    ) {
      return false;
    }
    return true;
  };

  const onSelect = (_: any, selection: any) => {
    if (selectedGateways.includes(selection)) {
      setSelectedGateways(selectedGateways.filter(item => item !== selection));
    } else {
      setSelectedGateways([...selectedGateways, selection]);
    }
  };

  const onChangeInput = async (value: string) => {
    // TODO: Hit the proper queries and remove the existing array being returned.
    return [
      {
        id: "id-juno:57966",
        isDisabled: false,
        key: "key-juno:57966",
        value: "juno:57966"
      }
    ];
  };

  const clearSelection = () => {
    setSelectedGateways([]);
  };

  const onChangeID = (id: string) => {
    deviceIDRegExp.test(id) ? setIsValid(true) : setIsValid(false);
    setInputID(id);
  };

  return (
    <Grid>
      <GridItem span={8}>
        <Title id="add-gateway-device-info-title" headingLevel="h4" size="xl">
          {header}
        </Title>
        <br />
        <Popover
          bodyContent={
            <div>
              In AMQ IoT, gateway devices are represented in Hono in the same
              way as any other devices.
            </div>
          }
          aria-label="Add gateway devices info popover"
          closeBtnAriaLabel="Close Gateway Devices info popover"
        >
          <Button
            variant="link"
            id="add-gateway-info-popover"
            icon={<InfoCircleIcon />}
          >
            How AMQ IoT handles gateways?
          </Button>
        </Popover>
        <br />
        <br />
        <Form>
          <FormGroup
            label="Device ID of gateway"
            type="number"
            id="add-gateway-form-group"
            isRequired
            helperTextInvalid="Age has to be a number"
            fieldId="device-id"
            validated="default"
          >
            <Flex>
              <FlexItem>
                <DropdownWithToggle
                  id="add-gateway-dropdown-type"
                  position={DropdownPosition.left}
                  dropdownItems={typeOptions}
                  onSelectItem={onTypeSelect}
                  value={inputType}
                />
              </FlexItem>
              <FlexItem>
                {inputType === "Device ID" ? (
                  <TextInput
                    validated={deviceIDValidationState()}
                    value={inputID}
                    id="add-gateway-text-input-id"
                    aria-label="Input device ID"
                    aria-describedby="Input device ID"
                    onChange={onChangeID}
                    placeholder="Input a device ID"
                  />
                ) : (
                  <TypeAheadSelect
                    id="add-gateway-group-type-ahead"
                    onSelect={onSelect}
                    onClear={clearSelection}
                    selected={selectedGateways}
                    typeAheadAriaLabel={"Typeahead to select gateway groups"}
                    isMultiple={true}
                    onChangeInput={onChangeInput}
                  />
                )}
              </FlexItem>
              <FlexItem>
                <Button
                  isDisabled={disableAddGatewayBtn()}
                  id="add-gateway-add-device-gateway-btn"
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
            <Title id="add-gateway-title-added" headingLevel="h6" size="md">
              Added Gateways
            </Title>
            <br />
            <ChipGroup id="add-gateway-chip-group">
              {gateways.map((gateway: string) => (
                <Chip
                  key={gateway}
                  id={`add-gateway-device-chip-${gateway}`}
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

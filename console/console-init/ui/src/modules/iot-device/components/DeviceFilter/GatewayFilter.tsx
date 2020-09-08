/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { useState } from "react";
import {
  FormGroup,
  Grid,
  GridItem,
  TextInput,
  Button,
  ButtonVariant,
  DropdownPosition
} from "@patternfly/react-core";
import { DropdownWithToggle } from "components";
import { css, StyleSheet } from "aphrodite";
import { deviceGatewayConnectionOptions } from "modules/iot-device/utils";
import { IDeviceFilter } from "./DeviceFilter";
import { ChipGroupsWithTitle } from "components/ChipGroupsWithTitle";

const styles = StyleSheet.create({
  dropdown_align: { display: "flex" },
  dropdown_toggle_align: { flex: "1" },
  grid_margin: { marginLeft: 10 }
});

interface IGatewayFilter {
  filter: IDeviceFilter;
  setFilter: (filter: IDeviceFilter) => void;
}

const GatewayFilter: React.FunctionComponent<IGatewayFilter> = ({
  filter,
  setFilter
}) => {
  const [gatewayGroupConnection, setGatewayGroupConnection] = useState<string>(
    deviceGatewayConnectionOptions[0].value
  );
  const [gatewayInput, setGatewayInput] = useState<string>("");

  const onChangeGatewayConnection = (value: string) => {
    setGatewayInput(value);
  };

  const onSelectGatewayConnection = (value: string) => {
    setGatewayGroupConnection(value);
  };

  const setSelectedGroupConnections = (connections: string[]) => {
    const filterObj = { ...filter };
    filterObj.gatewayConnections = connections;
    setFilter(filterObj);
  };

  const deleteGatewayConnection = (chip: string) => {
    const { gatewayConnections } = filter;
    if (gatewayConnections?.length > 0 && chip.trim() !== "") {
      let index = gatewayConnections.indexOf(chip.toString());
      if (index >= 0) gatewayConnections.splice(index, 1);
      setSelectedGroupConnections([...gatewayConnections]);
    }
  };

  const addGatewayConnection = () => {
    const { gatewayConnections } = filter;
    if (gatewayInput && gatewayInput.trim() !== "") {
      setSelectedGroupConnections([...gatewayConnections, gatewayInput]);
      setGatewayInput("");
    }
  };

  const { gatewayConnections } = filter;

  return (
    <FormGroup
      label="Gateway connections"
      fieldId="device-filter-gateway-conn-dropdown"
    >
      <Grid>
        <GridItem span={4}>
          <DropdownWithToggle
            id={"device-filter-gateway-conn-dropdown"}
            toggleId="device-filter-gateway-conn-dropdowntoggle"
            name="Device Connections"
            className={css(styles.dropdown_align)}
            toggleClass={css(styles.dropdown_toggle_align)}
            position={DropdownPosition.left}
            onSelectItem={onSelectGatewayConnection}
            dropdownItems={deviceGatewayConnectionOptions}
            value={gatewayGroupConnection}
            isLabelAndValueNotSame={true}
          />
        </GridItem>
        <GridItem span={5} className={css(styles.grid_margin)}>
          <TextInput
            isRequired
            type="text"
            id="device-filter-gateway-connection-input"
            placeholder="Input gateway"
            name="gateway-connection"
            aria-describedby="gateway connection for filter"
            value={gatewayInput}
            onChange={onChangeGatewayConnection}
          />
        </GridItem>
        <GridItem span={3} className={css(styles.grid_margin)}>
          <Button
            variant={ButtonVariant.primary}
            id="device-filter-add-gateway-conn-btn"
            isDisabled={
              gatewayGroupConnection.trim() === "" || gatewayInput.trim() === ""
            }
            onClick={addGatewayConnection}
          >
            Add
          </Button>
        </GridItem>
      </Grid>
      <br />
      <ChipGroupsWithTitle
        id={"gateway-filter-connections-chip-group"}
        items={gatewayConnections}
        removeItem={deleteGatewayConnection}
      />
    </FormGroup>
  );
};

export { GatewayFilter };

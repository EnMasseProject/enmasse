/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Grid,
  GridItem,
  Button,
  ButtonVariant,
  Popover,
  PopoverPosition
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";
import { GatewayGroupTypeAheadSelect } from "containers";
import { ChipGroupsWithTitle } from "components";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  popover_alignment: {
    "padding-left": "0"
  },
  groups_min_height: {
    "min-height": "25rem"
  }
});

export interface IAddGatewayGroupMembershipProps {
  id: string;
  onChangeInput?: (value: string) => Promise<any>;
  returnGatewayGroups?: (groups: string[]) => void;
  gatewayGroups?: string[];
}

export const AddGatewayGroupMembership: React.FC<IAddGatewayGroupMembershipProps> = ({
  id,
  returnGatewayGroups,
  gatewayGroups: groups = []
}) => {
  const [selectedGroups, setSelectedGroups] = useState<string[]>([]);
  const [gatewayGroups, setGatewayGroups] = useState<string[]>(groups);

  useEffect(() => {
    setGatewayGroups(groups);
  }, [groups]);

  useEffect(() => {
    returnGatewayGroups && returnGatewayGroups(gatewayGroups);
  }, [gatewayGroups]);

  const onSelectGatewayGroup = (_: any, selection: any) => {
    if (selectedGroups?.includes(selection)) {
      setSelectedGroups(
        selectedGroups.filter((item: string) => item !== selection)
      );
    } else {
      setSelectedGroups([...selectedGroups, selection]);
    }
  };

  const onClear = () => {
    setSelectedGroups([]);
  };

  const onAddGroup = () => {
    const newGroups = Array.from(
      new Set([...gatewayGroups, ...selectedGroups])
    );
    setGatewayGroups(newGroups);
    onClear();
  };

  const removeGatewayGroup = (group: string) => {
    const idIndex = gatewayGroups.indexOf(group);
    if (idIndex >= 0) {
      gatewayGroups.splice(idIndex, 1);
      setGatewayGroups([...gatewayGroups]);
    }
  };

  return (
    <Grid id={id} hasGutter>
      <GridItem>
        <Popover
          position={PopoverPosition.bottom}
          bodyContent={
            <div>
              Gateway groups are collections of gateways. When giving a device
              permission to connect to a gateway group, it can connect to AMQ
              online through any of the gateways in the group.
            </div>
          }
          aria-label="add gateway group info popover"
          closeBtnAriaLabel="close gateway group info popover"
        >
          <Button
            variant="link"
            id="add-gateway-info-popover-button"
            icon={<ExclamationCircleIcon />}
            className={css(styles.popover_alignment)}
          >
            What is an AMQ IoT gateway group?
          </Button>
        </Popover>
      </GridItem>
      <GridItem span={4}>
        <GatewayGroupTypeAheadSelect
          id="add-gateway-group-membership-typeahead-select"
          aria-label="gateway group membership dropdown"
          aria-describedby="multi typeahead for gateway groups membership"
          onSelect={onSelectGatewayGroup}
          onClear={onClear}
          selected={selectedGroups}
          typeAheadAriaLabel={"typeahead to select gateway group membership"}
          isMultiple={true}
          isCreatable={true}
          placeholderText={"Input gateway group name"}
        />
      </GridItem>
      <GridItem span={2}>
        <Button
          id="add-gateway-group-button"
          variant={ButtonVariant.secondary}
          aria-label="add gateway group button"
          isDisabled={selectedGroups?.length < 1}
          onClick={onAddGroup}
        >
          Add
        </Button>
      </GridItem>
      <GridItem className={css(styles.groups_min_height)}>
        <ChipGroupsWithTitle
          id="gateway-group-membership-chipgroups"
          titleId="gateway-group-membership-title"
          title={"Selected gateway groups"}
          items={gatewayGroups}
          removeItem={removeGatewayGroup}
        />
      </GridItem>
    </Grid>
  );
};

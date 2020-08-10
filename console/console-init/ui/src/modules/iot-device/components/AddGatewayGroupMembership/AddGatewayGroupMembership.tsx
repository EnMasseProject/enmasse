/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Grid,
  GridItem,
  Title,
  Text,
  Button,
  ButtonVariant,
  Popover,
  PopoverPosition,
  ChipGroup,
  Chip
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";
import { TypeAheadSelect } from "components";
import { StyleSheet, css } from "aphrodite";
import { mockGatewayGroups } from "mock-data";

const styles = StyleSheet.create({
  popover_alignment: {
    "padding-left": "0px"
  },
  groups_min_height: {
    "min-height": "25rem"
  },
  fontWeight: {
    "font-weight": "var(--pf-global--FontWeight--light)"
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

  const onChangeGroupInput = async (value: string) => {
    // TODO: integrate backend query and remove mock data
    const filtererGroups = mockGatewayGroups?.filter(
      item => item?.value.toLowerCase().indexOf(value.toLowerCase()) > -1
    );
    return filtererGroups;
  };

  const removeGatewayGroup = (group: string) => {
    const idIndex = gatewayGroups.indexOf(group);
    if (idIndex >= 0) {
      gatewayGroups.splice(idIndex, 1);
      setGatewayGroups([...gatewayGroups]);
    }
  };

  const renderGatewayGroups = () => {
    return (
      <div className={css(styles.groups_min_height)}>
        {gatewayGroups?.length > 0 && (
          <>
            <br />
            <Title size="md" headingLevel="h4">
              Selected gateway groups:
            </Title>
            <br />
            <ChipGroup>
              {gatewayGroups?.map((group: string) => (
                <Chip
                  key={group}
                  id={`add-gateway-group-remove-chip-${group}`}
                  value={group}
                  onClick={() => removeGatewayGroup(group)}
                >
                  {group}
                </Chip>
              ))}
            </ChipGroup>
          </>
        )}
      </div>
    );
  };

  return (
    <Grid id={id} hasGutter>
      <GridItem>
        <Title
          id="add-gateway-group-membership-title"
          headingLevel="h1"
          size="xl"
          aria-label="add gatewy group membership title"
        >
          Edit gateway groups membership
          <small className={css(styles.fontWeight)}> (optional)</small>
        </Title>
        <Text>
          If you are adding a gateway device, you can assign it to gateway
          groups
        </Text>
      </GridItem>
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
        <TypeAheadSelect
          id="add-gateway-group-typeahead"
          aria-label="gateway group dropdown"
          aria-describedby="multi typeahead for gateway groups"
          onSelect={onSelectGatewayGroup}
          onClear={onClear}
          selected={selectedGroups}
          typeAheadAriaLabel={"typeahead to select gateway groupps"}
          isMultiple={true}
          onChangeInput={onChangeGroupInput}
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
      <GridItem>{renderGatewayGroups()}</GridItem>
    </Grid>
  );
};

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { useState } from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { Flex, FlexItem, Button, ButtonVariant } from "@patternfly/react-core";
import { AddGatewayGroupMembership } from "modules/iot-device/components";
import { RETURN_IOT_DEVICE_DETAIL } from "graphql-module/queries";
import { FetchPolicy } from "constant";
import { IDeviceDetailResponse } from "schema";

export const EditGatewayGroupMembershipContainer: React.FC<{
  onCancel: () => void;
}> = ({ onCancel }) => {
  const { projectname, deviceid, namespace } = useParams();
  const [groupMembers, setGroupMembers] = useState<string[]>([]);
  const [isSaveDisabled, setIsSaveDisabled] = useState<boolean>(true);

  const queryResolver = `
    devices{
      registration{      
        memberOf      
      }                  
    }
  `;

  const { data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, namespace, deviceid, queryResolver),
    {
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    }
  );

  const { registration } = data?.devices?.devices[0] || {
    registration: { memberOf: [] }
  };

  const onSave = () => {
    /**
     * Todo: write save query
     */
  };

  const setGatewayGroups = (groups: string[]) => {
    if (isSaveDisabled && groupMembers?.length > 0) {
      setIsSaveDisabled(false);
    }
    setGroupMembers(groups);
  };

  return (
    <>
      <AddGatewayGroupMembership
        id="edit-gateway-group-membership"
        gatewayGroups={registration?.memberOf}
        placeholderText="Input new or existing gateway group name"
        returnGatewayGroups={setGatewayGroups}
      />
      <Flex>
        <FlexItem>
          <Button
            id="connected-directly-next-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
            isDisabled={isSaveDisabled}
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

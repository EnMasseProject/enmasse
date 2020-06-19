/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { Flex, FlexItem, Button, ButtonVariant } from "@patternfly/react-core";
import { Loading } from "use-patternfly";
// import { StyleSheet } from "@patternfly/react-styles";
import { useQuery } from "@apollo/react-hooks";
import { JsonEditor } from "components";
import { useStoreContext, types } from "context-state-reducer";
import { RETURN_IOT_DEVICE_DETAIL } from "graphql-module";
import { IDeviceDetailResponse } from "schema";

// const styles = StyleSheet.create({
//   editor_border: {
//     border: "1px solid var(--pf-global--BorderColor--100)"
//   }
// });

export const EditDeviceInJsonContainer = () => {
  const { dispatch } = useStoreContext();
  const { projectname, deviceid } = useParams();

  const { loading, data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, deviceid)
  );

  const { devices } = data || {
    devices: { total: 0, devices: [] }
  };

  const { enabled, viaGateway, jsonData, credentials } =
    devices?.devices[0] || {};

  if (loading) return <Loading />;

  const getDeviceJson = () => {
    const deviceinfo = {
      enabled,
      viaGateway
    };
    jsonData && Object.assign(deviceinfo, JSON.parse(jsonData));
    credentials &&
      Object.assign(deviceinfo, { credentials: JSON.parse(credentials) });

    return deviceinfo;
  };

  const resetActionType = () => {
    dispatch({ type: types.RESET_DEVICE_ACTION_TYPE });
  };

  const onSave = () => {
    /**
     * TODO: implement save query
     */
    resetActionType();
  };

  const onCancel = () => {
    resetActionType();
  };

  return (
    <>
      <JsonEditor
        readOnly={false}
        value={JSON.stringify(getDeviceJson(), undefined, 2)}
        // className={styles.editor_border}
      />
      <br />
      <br />
      <Flex>
        <FlexItem>
          <Button
            id="ed-save-device-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
          >
            Save
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="ed-cancel-device-button"
            variant={ButtonVariant.secondary}
            onClick={onCancel}
          >
            Cancel
          </Button>
        </FlexItem>
      </Flex>
    </>
  );
};

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import {
  Flex,
  FlexItem,
  Button,
  ButtonVariant,
  Alert,
  AlertVariant
} from "@patternfly/react-core";
import { Loading } from "use-patternfly";
import { StyleSheet, css } from "aphrodite";
import { useQuery } from "@apollo/react-hooks";
import { JsonEditor } from "components";
import {
  RETURN_IOT_DEVICE_DETAIL,
  UPDATE_IOT_DEVICE
} from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { Messages, FetchPolicy } from "constant";
import { serialize_IoT_Device } from "modules/iot-device/utils";
import { useMutationQuery } from "hooks";
import { convertStringToJsonAndValidate } from "utils";

const styles = StyleSheet.create({
  editor_border: {
    border: "1px solid var(--pf-global--BorderColor--100)"
  }
});

export const EditDeviceInJsonContainer: React.FC<{
  onCancel: () => void;
}> = ({ onCancel }) => {
  const { projectname, deviceid, namespace } = useParams();
  const [deviceJson, setDeviceJson] = useState<any>();
  const [hasError, setHasError] = useState<boolean>(false);

  const queryResolver = `
    devices{
      deviceId
      registration{
        enabled
        via
        memberOf
        viaGroups
        ext
        defaults
      }
      status{
        lastSeen
        updated
        created
      }  
      credentials 
    }
  `;

  const { loading, data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, namespace, deviceid, queryResolver),
    { fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const [setCreateIoTDeviceQueryVariable] = useMutationQuery(
    UPDATE_IOT_DEVICE,
    ["iot_device_detail"],
    undefined,
    onCancel
  );
  const { devices } = data || {
    devices: { total: 0, devices: [] }
  };

  const { credentials, registration } = devices?.devices[0] || {};
  const { enabled, via, ext, memberOf, viaGroups, defaults } =
    registration || {};

  if (loading) return <Loading />;

  const getDeviceJson = () => {
    const deviceinfo = {
      registration: {
        enabled,
        via,
        memberOf: memberOf || [],
        viaGroups: viaGroups || []
      }
    };
    if (defaults) {
      const { hasError, value } = convertStringToJsonAndValidate(defaults);
      Object.assign(deviceinfo.registration, { defaults: value });
      hasError && setHasError(hasError);
    }
    if (ext) {
      const { hasError, value } = convertStringToJsonAndValidate(ext);
      Object.assign(deviceinfo.registration, { ext: value });
      hasError && setHasError(hasError);
    }
    if (credentials) {
      const { hasError, value } = convertStringToJsonAndValidate(credentials);
      Object.assign(deviceinfo, { credentials: value });
      hasError && setHasError(hasError);
    }
    return deviceinfo;
  };

  const onSave = async () => {
    const { hasError, device } = serialize_IoT_Device(deviceJson, deviceid);
    if (hasError) {
      setHasError(true);
    } else {
      setHasError(false);
      const variable = {
        iotproject: { name: projectname, namespace },
        device
      };
      await setCreateIoTDeviceQueryVariable(variable);
    }
  };

  const setDeviceDetail = (value: string | undefined) => {
    setDeviceJson(value);
  };

  return (
    <>
      {hasError && (
        <>
          <Alert variant={AlertVariant.danger} title={"Error"} isInline>
            {Messages.InvalidJson}
          </Alert>
          <br />
        </>
      )}
      <JsonEditor
        readOnly={false}
        value={JSON.stringify(getDeviceJson(), undefined, 2)}
        className={css(styles.editor_border)}
        setDetail={setDeviceDetail}
      />
      <br />
      <br />
      <Flex>
        <FlexItem>
          <Button
            id="edit-device-json-container-save-button"
            variant={ButtonVariant.primary}
            onClick={onSave}
          >
            Save
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            id="edit-device-json-container-cancel-button"
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

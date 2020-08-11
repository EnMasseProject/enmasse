/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  PageSection,
  PageSectionVariants,
  Button,
  Grid,
  GridItem,
  Split,
  SplitItem
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { JsonEditor } from "components";
import {
  AddJsonUsingTemplate,
  connectedViaGatewayDeviceTemplate,
  directlyConnectedDeviceTemplate
} from "./AddJsonUsingTemplate";
import {
  compareObject,
  getFormattedJsonString,
  convertStringToJsonAndValidate
} from "utils";
import { types, MODAL_TYPES, useStoreContext } from "context-state-reducer";
import { DeviceListAlert } from "modules/iot-device";
import { TemplateType } from "constant";
import {
  IDeviceProp,
  ReviewDeviceContainer
} from "modules/iot-device/containers";
import { INVALID_JSON_ERROR } from "modules/iot-device/utils";
import { IDeviceResponse } from "schema";

const styles = StyleSheet.create({
  box_align_style: {
    minHeight: "40em",
    maxHeight: "40em",
    border: "1px solid",
    borderColor: "grey",
    padding: 20,
    marginRight: 20
  },
  padding_left: {
    paddingLeft: 30
  }
});

export interface IRegistration {
  enabled?: boolean;
  via?: string[];
  ext?: any;
  viaGroups?: string[];
  memberOf?: string[];
  defaults?: any;
}
interface IAddDeviceWithJsonProps {
  deviceDetail?: IDeviceResponse;
  onLeave: () => void;
  onSave: (detail: string) => void;
  allowTemplate?: boolean;
}
const getDeviceRegistrationString = (deviceDetail: IDeviceResponse) => {
  let device: any = { registration: {} };
  if (deviceDetail.registration) {
    const {
      enabled,
      defaults,
      memberOf,
      via,
      ext,
      viaGroups
    } = deviceDetail.registration;
    const deviceRegistration: IRegistration = {};
    if (enabled) {
      deviceRegistration.enabled = enabled;
    }
    if (defaults) {
      deviceRegistration.defaults = JSON.parse(defaults);
    }
    if (ext) {
      deviceRegistration.ext = JSON.parse(ext);
    }
    if ((!via || via.length === 0) && (!viaGroups || viaGroups?.length === 0)) {
      device.credentials = [];
    } else {
      if (via) {
        deviceRegistration.via = via;
      }
      if (viaGroups) {
        deviceRegistration.viaGroups = viaGroups;
      }
    }
    if (memberOf && memberOf.length > 0) {
      deviceRegistration.memberOf = memberOf;
    }
    if ((!via || via.length === 0) && (!viaGroups || viaGroups?.length === 0)) {
      device.credentials = [];
    }
    device.registration = deviceRegistration;
  }
  return JSON.stringify(device, undefined, 2);
};

const AddDeviceWithJson: React.FunctionComponent<IAddDeviceWithJsonProps> = ({
  deviceDetail,
  onLeave,
  onSave,
  allowTemplate = true
}) => {
  const { dispatch } = useStoreContext();
  const [deviceJsonString, setDeviceJsonString] = useState<string>();
  const [selectedTemplate, setSelectedTemplate] = useState<string>(
    TemplateType.DIRECTLY_CONNECTED
  );
  const [isPreviewEnabled, setIsPreviewEnabled] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>("");

  useEffect(() => {
    if (deviceDetail) {
      setDeviceJsonString(getDeviceRegistrationString(deviceDetail));
    }
  }, []);

  const setDeviceInfoInDetail = (value?: string) => {
    if (errorMessage !== "") {
      setErrorMessage("");
    }
    setDeviceJsonString(value);
  };

  const onCancel = () => {
    let isJsonPresent: boolean = false;
    if (deviceJsonString) {
      const { hasError, value } = convertStringToJsonAndValidate(
        deviceJsonString
      );
      if (value && selectedTemplate === TemplateType.DIRECTLY_CONNECTED) {
        if (!compareObject(value, directlyConnectedDeviceTemplate)) {
          isJsonPresent = true;
        }
      } else if (selectedTemplate === TemplateType.VIA_GATEWAY) {
        if (!compareObject(value, connectedViaGatewayDeviceTemplate)) {
          isJsonPresent = true;
        }
      }
      if (hasError && deviceJsonString) {
        isJsonPresent = true;
      }
    }

    if (isJsonPresent) {
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.LEAVE_CREATE_DEVICE,
        modalProps: {
          onConfirm: onLeave,
          option: "Leave",
          detail: `Do you want to leave this creation page without saving? All information will be lost.`,
          header: "Leave without saving ?",
          confirmButtonLabel: "Leave",
          iconType: "danger"
        }
      });
    } else {
      onLeave();
    }
  };

  const isJsonValid = () => {
    if (!deviceJsonString || deviceJsonString.trim() === "") {
      if (deviceJsonString !== "") {
        setDeviceInfoInDetail(undefined);
      }
      return true;
    }
    const { hasError, value } = convertStringToJsonAndValidate(
      deviceJsonString
    );
    if (value) {
      setDeviceInfoInDetail(getFormattedJsonString(value));
      return true;
    }
    if (hasError) {
      return false;
    }
  };

  const onFinish = () => {
    if (deviceJsonString)
      if (!isJsonValid()) {
        setErrorMessage(INVALID_JSON_ERROR);
      } else {
        onSave(deviceJsonString);
      }
  };

  const onPreview = () => {
    if (deviceJsonString) {
      if (!isJsonValid()) {
        setErrorMessage(INVALID_JSON_ERROR);
      } else {
        setIsPreviewEnabled(true);
      }
    }
  };

  const onBack = () => {
    setIsPreviewEnabled(false);
  };

  const getDeviceDetail = () => {
    const device: IDeviceProp = {
      deviceInformation: {
        metadata: []
      },
      connectionType: "",
      credentials: [],
      gateways: { gateways: [], gatewayGroups: [] }
    };
    if (deviceJsonString) {
      const parseDeviceDetail = JSON.parse(deviceJsonString);

      //add deviceId to device object from the parseDeviceDetail
      device.deviceInformation.deviceId = parseDeviceDetail.id;
      //add registration.enabled field to device object from the parseDeviceDetail
      if (parseDeviceDetail?.registration?.enabled !== undefined) {
        device.deviceInformation.status =
          parseDeviceDetail.registration.enabled;
      }
      //add credentials field to device object from the parseDeviceDetail
      if (device.credentials && parseDeviceDetail.credentials) {
        device.credentials = parseDeviceDetail.credentials;
      }
      //add registration.via field to device object from the parseDeviceDetail
      if (device.gateways?.gateways && parseDeviceDetail?.registration?.via) {
        device.gateways.gateways = parseDeviceDetail.registration.via;
      }
    }
    return device;
  };

  const getEditor = () => {
    return (
      <JsonEditor
        value={deviceJsonString}
        readOnly={false}
        name={"editor-add-device"}
        manageState={false}
        setDetail={setDeviceInfoInDetail}
        style={{
          minHeight: "39em"
        }}
        className={css(styles.box_align_style)}
      />
    );
  };

  return (
    <PageSection variant={PageSectionVariants.light}>
      <DeviceListAlert
        id="add-device-json-invalid-device-list-alert"
        variant="danger"
        isInline
        visible={errorMessage !== ""}
        title="Error"
        description={errorMessage}
      />
      <br />

      <Grid>
        {isPreviewEnabled ? (
          <div className={css(styles.padding_left)}>
            <ReviewDeviceContainer device={getDeviceDetail()} />
          </div>
        ) : (
          <>
            {allowTemplate ? (
              <>
                <GridItem span={9} className={css(styles.box_align_style)}>
                  {getEditor()}
                </GridItem>
                <GridItem span={3} className={css(styles.box_align_style)}>
                  <PageSection variant={PageSectionVariants.light}>
                    <AddJsonUsingTemplate
                      setDetail={setDeviceInfoInDetail}
                      selectedTemplate={selectedTemplate}
                      setSelectedTemplate={setSelectedTemplate}
                      setErrorMessage={setErrorMessage}
                    />
                  </PageSection>
                </GridItem>
              </>
            ) : (
              getEditor()
            )}
          </>
        )}
      </Grid>

      <br />
      <Split className={css(styles.padding_left)} hasGutter>
        <SplitItem>
          <Button
            id="add-device-json-finish-button"
            aria-label="finish button"
            variant="primary"
            onClick={onFinish}
          >
            Finish
          </Button>
        </SplitItem>
        <SplitItem>
          {isPreviewEnabled ? (
            <Button
              id="add-device-json-back-button"
              aria-label="Back button"
              variant="secondary"
              onClick={onBack}
            >
              Back
            </Button>
          ) : (
            <Button
              id="add-device-json-preview-button"
              aria-label="Preview button"
              variant="secondary"
              onClick={onPreview}
              disabled={!deviceJsonString || deviceJsonString?.trim() === ""}
            >
              Preview
            </Button>
          )}
        </SplitItem>
        <SplitItem>
          <Button
            id="add-device-json-cancel-button"
            aria-label="cancel button"
            variant="link"
            onClick={onCancel}
          >
            Cancel
          </Button>
        </SplitItem>
      </Split>
    </PageSection>
  );
};

export { AddDeviceWithJson };

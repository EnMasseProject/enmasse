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
const getDeviceRegistrationString = (
  deviceDetail: IDeviceResponse,
  setErrorMessage: (error: string) => void
) => {
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
      const { hasError, value } = convertStringToJsonAndValidate(defaults);
      if (hasError) {
        setErrorMessage(INVALID_JSON_ERROR);
      } else {
        deviceRegistration.defaults = value;
      }
    }
    if (ext) {
      const { hasError, value } = convertStringToJsonAndValidate(ext);
      if (hasError) {
        setErrorMessage(INVALID_JSON_ERROR);
      } else {
        deviceRegistration.defaults = value;
      }
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
  const [device, setDevice] = useState<string>();
  const [selectedTemplate, setSelectedTemplate] = useState<string>(
    TemplateType.DIRECTLY_CONNECTED
  );
  const [isPreviewEnabled, setIsPreviewEnabled] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>("");

  useEffect(() => {
    if (deviceDetail) {
      setDeviceJsonString(
        getDeviceRegistrationString(deviceDetail, setErrorMessage)
      );
    }
  }, []);

  const isJsonModified = (onCancel: boolean) => {
    let isModified: boolean = false;
    if (deviceJsonString) {
      const { hasError, value } = convertStringToJsonAndValidate(
        deviceJsonString
      );
      if (hasError && deviceJsonString) {
        isModified = true;
      }
      if (onCancel) {
        if (value && selectedTemplate === TemplateType.DIRECTLY_CONNECTED) {
          if (!compareObject(value, directlyConnectedDeviceTemplate)) {
            isModified = true;
          }
        } else if (selectedTemplate === TemplateType.VIA_GATEWAY) {
          if (!compareObject(value, connectedViaGatewayDeviceTemplate)) {
            isModified = true;
          }
        }
      } else {
        if (value && selectedTemplate === TemplateType.DIRECTLY_CONNECTED) {
          if (!compareObject(value, connectedViaGatewayDeviceTemplate)) {
            isModified = true;
          }
        } else if (selectedTemplate === TemplateType.VIA_GATEWAY) {
          if (!compareObject(value, directlyConnectedDeviceTemplate)) {
            isModified = true;
          }
        }
      }
    }
    return isModified;
  };

  const setDeviceInfoInDetail = (value?: string) => {
    if (errorMessage !== "") {
      setErrorMessage("");
    }
    setDevice(value);
  };

  const setDeviceUsingEditor = (value?: string) => {
    if (errorMessage !== "") {
      setErrorMessage("");
    }
    setDeviceJsonString(value);
  };

  const setDeviceUsingTemplate = (value?: string) => {
    if (isJsonModified(false)) {
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.LEAVE_CREATE_DEVICE,
        modalProps: {
          onConfirm: () => setDeviceInfoInDetail(value),
          option: "Modify",
          detail: `Do you want to modify this json without saving? All information will be lost.`,
          header: "Modify without saving ?",
          confirmButtonLabel: "Modify",
          iconType: "danger"
        }
      });
    } else {
      setDeviceInfoInDetail(value);
    }
  };
  const onCancel = () => {
    if (isJsonModified(true)) {
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
      return true;
    }
    const { hasError, value } = convertStringToJsonAndValidate(
      deviceJsonString
    );
    if (hasError) {
      return false;
    }
    if (value) {
      setDeviceInfoInDetail(getFormattedJsonString(value));
      return true;
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
        value={device}
        readOnly={false}
        name={"editor-add-device"}
        setDetail={setDeviceUsingEditor}
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
                      setDetail={setDeviceUsingTemplate}
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

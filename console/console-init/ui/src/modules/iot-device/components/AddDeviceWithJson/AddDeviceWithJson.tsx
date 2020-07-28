/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
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
import { compareObject, getFormattedJsonString } from "utils";
import { types, MODAL_TYPES, useStoreContext } from "context-state-reducer";
import { DeviceListAlert } from "modules/iot-device";
import { TemplateType } from "constant";
import {
  IDeviceProp,
  ReviewDeviceContainer
} from "modules/iot-device/containers";

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
interface IAddDeviceWithJsonProps {
  deviceDetail?: string;
  setDeviceDetail: (detail?: string) => void;
  onLeave: () => void;
  onSave: (detail: string) => void;
  // onPreview: () => void;
}

const AddDeviceWithJson: React.FunctionComponent<IAddDeviceWithJsonProps> = ({
  deviceDetail,
  setDeviceDetail,
  onLeave,
  onSave
  // onPreview,
}) => {
  const { dispatch } = useStoreContext();
  const [selectedTemplate, setSelectedTemplate] = useState<string>(
    TemplateType.DIRECTLY_CONNECTED
  );
  const [isPreviewEnabled, setIsPreviewEnabled] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>("");

  const setDeviceInfoInDetail = (value?: string) => {
    if (errorMessage !== "") {
      // setShowJsonValidationError(false);
      setErrorMessage("");
    }
    setDeviceDetail(value);
  };

  const onCancel = () => {
    let isJsonPresent: boolean = false;
    try {
      const detail = deviceDetail && JSON.parse(deviceDetail);
      if (detail && selectedTemplate === TemplateType.DIRECTLY_CONNECTED) {
        if (!compareObject(detail, directlyConnectedDeviceTemplate)) {
          isJsonPresent = true;
        }
      } else if (selectedTemplate === TemplateType.VIA_GATEWAY) {
        if (!compareObject(detail, connectedViaGatewayDeviceTemplate)) {
          isJsonPresent = true;
        }
      }
    } catch {
      if (deviceDetail) isJsonPresent = true;
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
    try {
      if (!deviceDetail || deviceDetail.trim() === "") {
        if (deviceDetail !== "") {
          setDeviceInfoInDetail(undefined);
        }
        return true;
      }
      const detail = JSON.parse(deviceDetail.trim());
      if (detail) {
        setDeviceInfoInDetail(getFormattedJsonString(detail));
        return true;
      }
    } catch {
      return false;
    }
  };

  const onFinish = () => {
    if (deviceDetail)
      if (!isJsonValid()) {
        setErrorMessage("Invalid JSON syntax, unable to parse JSON");
      } else {
        onSave(deviceDetail);
      }
  };

  const handleOnPreview = () => {
    if (deviceDetail) {
      if (!isJsonValid()) {
        setErrorMessage("Invalid JSON syntax, unable to parse JSON");
      } else {
        setIsPreviewEnabled(true);
      }
    }
  };

  const handleOnBack = () => {
    setIsPreviewEnabled(false);
  };

  const getDeviceDetail = () => {
    const device: IDeviceProp = {
      deviceInformation: {
        metadata: []
      },
      credentials: [],
      gateways: { gateways: [], gatewayGroups: [] }
    };
    if (deviceDetail) {
      const obj = JSON.parse(deviceDetail);
      if (device && device.deviceInformation) {
        device.deviceInformation.deviceId = obj.id;
        if (obj?.registration?.enabled) {
          device.deviceInformation.status = obj.registration.enabled;
        }
      }
      if (device.credentials && obj.credentials) {
        device.credentials = obj.credentials;
      }
      if (device.gateways?.gateways) {
        if (obj?.registration?.via) {
          device.gateways.gateways = obj.registration.via;
        }
      }
    }
    return device;
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
            <GridItem span={9} className={css(styles.box_align_style)}>
              <JsonEditor
                value={deviceDetail}
                readOnly={false}
                name={"editor-add-device"}
                setDetail={setDeviceInfoInDetail}
                style={{
                  minHeight: "39em"
                }}
              />
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
        )}
      </Grid>
      <br />
      <Split className={css(styles.padding_left)} hasGutter>
        <SplitItem>
          <Button
            id="add-device-json-finish-button"
            aria-label="Finish button"
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
              onClick={handleOnBack}
            >
              Back
            </Button>
          ) : (
            <Button
              id="add-device-json-preview-button"
              aria-label="Preview button"
              variant="secondary"
              onClick={handleOnPreview}
              disabled={!deviceDetail || deviceDetail?.trim() === ""}
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

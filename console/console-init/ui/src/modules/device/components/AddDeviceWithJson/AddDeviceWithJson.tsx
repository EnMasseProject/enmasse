/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  PageSection,
  PageSectionVariants,
  Flex,
  FlexItem,
  FlexModifiers,
  Button
} from "@patternfly/react-core";
import { StyleSheet, css } from "@patternfly/react-styles";
import { JsonEditor, useWindowDimensions } from "components";
import {
  AddJsonUsingTemplate,
  connectedViaGatewayDeviceTemplate,
  directlyConnectedDeviceTemplate
} from "./AddJsonUsingTemplate";
import { compareJsonObject, getFormattedJsonString } from "utils";
import { types, MODAL_TYPES, useStoreContext } from "context-state-reducer";
import { DeviceListAlert } from "modules/device";
import { TemplateType } from "constant";

const styles = StyleSheet.create({
  box_align_style: {
    minHeight: "40em",
    maxHeight: "40em",
    border: "1px solid",
    borderColor: "grey"
  }
});
interface IAddDeviceWithJsonProps {}

const AddDeviceWithJson: React.FunctionComponent<IAddDeviceWithJsonProps> = () => {
  const { dispatch } = useStoreContext();
  const width = useWindowDimensions().width;
  const [json, setJson] = useState<string>();
  const [selectedTemplate, setSelectedTemplate] = useState<string>(
    TemplateType.DIRECTLY_CONNECTED
  );
  const [showJsonValidationError, setShowJsonValidationError] = useState<
    boolean
  >(false);

  const setJsonHandler = (value?: string) => {
    if (showJsonValidationError) {
      setShowJsonValidationError(false);
    }
    setJson(value);
  };

  const onLeaveModal = () => {
    //TODO: handle onConfirm event for modal
  };

  const onCancel = () => {
    let isJsonPresent: boolean = false;
    try {
      const data = json && JSON.parse(json);
      if (data && selectedTemplate === TemplateType.DIRECTLY_CONNECTED) {
        if (!compareJsonObject(data, directlyConnectedDeviceTemplate)) {
          isJsonPresent = true;
        }
      } else if (selectedTemplate === TemplateType.VIA_GATEWAY) {
        if (!compareJsonObject(data, connectedViaGatewayDeviceTemplate)) {
          isJsonPresent = true;
        }
      }
    } catch {
      if (json) isJsonPresent = true;
    }
    if (isJsonPresent) {
      dispatch({
        type: types.SHOW_MODAL,
        modalType: MODAL_TYPES.LEAVE_CREATE_DEVICE,
        modalProps: {
          onConfirm: onLeaveModal,
          option: "Leave",
          detail: `Do you want to leave this creation page without saving? All information will be lost.`,
          header: "Leave without saving ?",
          confirmButtonLabel: "Leave"
        }
      });
    }
  };

  const isJsonValid = () => {
    try {
      if (!json || json.trim() === "") {
        if (json != "") {
          setJsonHandler(undefined);
        }
        return true;
      }
      const data = JSON.parse(json.trim());
      if (data) {
        setJsonHandler(getFormattedJsonString(data));
        return true;
      }
    } catch {
      return false;
    }
  };
  const onFinish = () => {
    if (!isJsonValid()) {
      setShowJsonValidationError(true);
    } else {
    }
  };
  const onPreview = () => {
    if (!isJsonValid()) {
      setShowJsonValidationError(true);
    } else {
    }
  };

  return (
    <PageSection variant={PageSectionVariants.light}>
      <DeviceListAlert
        id="alert-error-invalid-json"
        variant="danger"
        isInline
        visible={showJsonValidationError}
        title="Error"
        description="Invalid JSON syntax, unable to parse JSON"
      />

      <br />
      <Flex>
        <FlexItem
          breakpointMods={[{ modifier: FlexModifiers["spacer-3xl"] }]}
          style={{ minWidth: (width * 2) / 3 }}
        >
          <div className={css(styles.box_align_style)}>
            <JsonEditor
              value={json}
              readOnly={false}
              name={"editor-add-device"}
              setDetail={setJsonHandler}
              style={{
                minWidth: (width * 3) / 5,
                maxWidth: (width * 3) / 5,
                minHeight: "39em"
              }}
            />
          </div>
        </FlexItem>
        <FlexItem style={{ minWidth: width / 5 }}>
          <div className={css(styles.box_align_style)}>
            <PageSection variant={PageSectionVariants.light}>
              <AddJsonUsingTemplate
                setDetail={setJsonHandler}
                selectedTemplate={selectedTemplate}
                setSelectedTemplate={setSelectedTemplate}
              />
            </PageSection>
          </div>
        </FlexItem>
      </Flex>
      <br />

      <Button variant="primary" onClick={onFinish}>
        Finish
      </Button>
      {"  "}
      <Button variant="secondary" onClick={onPreview}>
        Preview
      </Button>
      {"  "}
      <Button variant="link" onClick={onCancel}>
        Cancel
      </Button>
    </PageSection>
  );
};

export { AddDeviceWithJson };

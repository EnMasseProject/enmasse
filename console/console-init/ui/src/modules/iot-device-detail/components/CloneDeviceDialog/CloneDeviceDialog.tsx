/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useStoreContext, types } from "context-state-reducer";
import {
  Modal,
  ModalVariant,
  Button,
  ButtonVariant,
  Radio
} from "@patternfly/react-core";
import { useHistory } from "react-router";

const CloneDeviceDialog: React.FunctionComponent<{}> = () => {
  const { state, dispatch } = useStoreContext();
  const { modalProps } = state && state.modal;
  const { projectname, namespace, deviceid } = modalProps;
  const history = useHistory();
  enum CloneType {
    WIZARD = "wizard",
    JSON = "json"
  }
  const [selectedOption, setSelectedOption] = useState<string>(
    CloneType.WIZARD
  );
  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
  };

  const onConfirmDialog = () => {
    onCloseDialog();
    let addDeviceRoute = "";
    if (selectedOption === CloneType.WIZARD) {
      addDeviceRoute = `/iot-projects/${namespace}/${projectname}/devices/${deviceid}/cloneform`;
    } else if (selectedOption === CloneType.JSON) {
      addDeviceRoute = `/iot-projects/${namespace}/${projectname}/devices/${deviceid}/clonejson`;
    }
    history.push(addDeviceRoute);
  };

  const onChange = (checked: boolean, event: any) => {
    const radioValue = event.target.value;
    if (checked) {
      if (radioValue === CloneType.WIZARD) {
        setSelectedOption(CloneType.WIZARD);
      } else if (radioValue === CloneType.JSON) {
        setSelectedOption(CloneType.JSON);
      }
    }
  };

  return (
    <>
      <Modal
        id="dialogue-prompt-modal"
        variant={ModalVariant.small}
        title={"Choose a way to edit the info of this device before clone it"}
        isOpen={true}
        onClose={onCloseDialog}
        actions={[
          <Button
            id="clone-device-choose-button"
            key={"choose"}
            variant={ButtonVariant.primary}
            onClick={onConfirmDialog}
          >
            {"Choose"}
          </Button>,
          <Button
            id="clone-device-cancel-button"
            key="cancel"
            variant="link"
            onClick={onCloseDialog}
          >
            Cancel
          </Button>
        ]}
      >
        <Radio
          value={CloneType.WIZARD}
          isChecked={selectedOption === CloneType.WIZARD}
          onChange={onChange}
          label={"Edit in Wizard"}
          name="radio-directly-connected-option"
          id="clone-device-wizard-option-radio"
          key="wizard-radio-option"
        />
        <Radio
          value={CloneType.JSON}
          isChecked={selectedOption === CloneType.JSON}
          onChange={onChange}
          label={"Edit in Json"}
          name="radio-directly-connected-option"
          id="clone-device-json-option-radio"
          key="json-radio-option"
        />
      </Modal>
    </>
  );
};

export { CloneDeviceDialog };

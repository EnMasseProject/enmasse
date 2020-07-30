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
import { useParams, useHistory } from "react-router";

const CloneDeviceOption: React.FunctionComponent<{}> = () => {
  const { state, dispatch } = useStoreContext();
  const { modalProps } = state && state.modal;
  const { projectname, namespace, deviceid } = modalProps;
  //   const { projectname, namespace } = useParams();
  const history = useHistory();
  const WIZARD = "wizard";
  const JSON = "json";
  const [selectedOption, setSelectedOption] = useState<string>(WIZARD);
  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
  };
  let addDeviceRoute = "";
  const onConfirmDialog = () => {
    onCloseDialog();
    if (selectedOption === WIZARD) {
      addDeviceRoute = `/iot-projects/${namespace}/${projectname}/devices/${deviceid}/cloneform`;
    } else if (selectedOption === JSON) {
      addDeviceRoute = `/iot-projects/${namespace}/${projectname}/devices/${deviceid}/clonejson`;
    }
    history.push(addDeviceRoute);
  };

  const onChange = (checked: boolean, event: any) => {
    const radioValue = event.target.value;
    if (checked) {
      if (radioValue === WIZARD) {
        setSelectedOption(WIZARD);
      } else if (radioValue === JSON) {
        setSelectedOption(JSON);
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
          value={WIZARD}
          isChecked={selectedOption === WIZARD}
          onChange={onChange}
          label={"Edit in Wizard"}
          name="radio-directly-connected-option"
          id="clone-device-wizard-option-radio"
          key="wizard-radio-option"
        />
        <Radio
          value={JSON}
          isChecked={selectedOption === JSON}
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

export { CloneDeviceOption };

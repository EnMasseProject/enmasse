/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Modal, Button } from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";

export const DialogPrompt: React.FunctionComponent<{}> = () => {
  const { state, dispatch } = useStoreContext();
  const { modalProps } = state && state.modal;
  const {
    onConfirm,
    onClose,
    data,
    selectedItems,
    option,
    detail,
    header
  } = modalProps;
  let nameString = "";

  if (Array.isArray(selectedItems)) {
    nameString = selectedItems.join(",");
  }

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    if (onClose) {
      onClose();
    }
  };

  const onConfirmDialog = () => {
    if (onConfirm) {
      onConfirm(data);
    }
    onCloseDialog();
  };

  return (
    <Modal
      id="Dialogue-prompt-modal"
      isSmall={true}
      title={header}
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button key={option} variant="primary" onClick={onConfirmDialog}>
          Confirm
        </Button>,
        <Button key="cancel" variant="link" onClick={onCloseDialog}>
          Cancel
        </Button>
      ]}
      isFooterLeftAligned={true}
    >
      <b>{nameString}</b>
      <br />
      {detail}
    </Modal>
  );
};

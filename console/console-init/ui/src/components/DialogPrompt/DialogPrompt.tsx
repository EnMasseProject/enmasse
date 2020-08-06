/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Modal, Button, Title, ModalVariant } from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";
import { WarningTriangleIcon } from "@patternfly/react-icons";

export enum IconVariant {
  "WARNING" = "warning",
  "DANGER" = "danger"
}

const getIcon = (icon?: string) => {
  if (icon && icon.trim() !== "")
    return (
      <>
        <WarningTriangleIcon color={"var(--pf-global--palette--orange-200)"} />
        &nbsp;
      </>
    );
};

const getConfirmButtonVariant = (confirmLabel?: string, iconType?: string) => {
  if (iconType && iconType === "danger") {
    return "danger";
  }
  switch (confirmLabel && confirmLabel.toLowerCase()) {
    case "delete":
      return "danger";
    default:
      return "primary";
  }
};

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
    header,
    confirmButtonLabel,
    iconType
  } = modalProps;
  let nameString = "";

  if (Array.isArray(selectedItems)) {
    nameString = selectedItems.join(", ");
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
      id="dialogue-prompt-modal"
      variant={ModalVariant.small}
      title={""}
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          id="dialogue-prompt-confirm-button"
          key={option}
          variant={getConfirmButtonVariant(confirmButtonLabel, iconType)}
          onClick={onConfirmDialog}
        >
          {confirmButtonLabel || "Confirm"}
        </Button>,
        <Button
          id="dialogue-prompt-cancel-button"
          key="cancel"
          variant="link"
          onClick={onCloseDialog}
        >
          Cancel
        </Button>
      ]}
    >
      <Title id="dialogue-prompt-header" headingLevel="h1" size="2xl">
        {getIcon(iconType)}
        {header}
      </Title>
      <br />
      {nameString && (
        <>
          <b>{nameString}</b>
          <br />
        </>
      )}
      {detail}
    </Modal>
  );
};

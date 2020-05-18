/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Modal, Button, Title } from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";
import { WarningTriangleIcon } from "@patternfly/react-icons";

export enum IconType {
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

const getConfirmButtonVariant = (confirmLabel?: string) => {
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
      title={""}
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key={option}
          variant={getConfirmButtonVariant(confirmButtonLabel)}
          onClick={onConfirmDialog}
        >
          {/* Confirm */}
          {confirmButtonLabel || "Confirm"}
        </Button>,
        <Button key="cancel" variant="link" onClick={onCloseDialog}>
          Cancel
        </Button>
      ]}
      isFooterLeftAligned={true}
    >
      <Title headingLevel="h1" size="2xl">
        {getIcon(iconType)}
        {header}
      </Title>
      <br />
      <b>{nameString}</b>
      <br />
      {detail}
    </Modal>
  );
};

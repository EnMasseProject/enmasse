/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Modal, Button } from "@patternfly/react-core";

interface IDialogueProps {
  option: "Delete" | "Purge";
  header: string;
  detail: string;
  names: string[];
  handleCancelDialogue: () => void;
  handleConfirmDialogue: () => void;
}
export const DialoguePrompt: React.FunctionComponent<IDialogueProps> = ({
  option,
  header,
  detail,
  names,
  handleCancelDialogue,
  handleConfirmDialogue
}) => {
  let nameString = "";
  for (let i = 0; i < names.length; i++) {
    if (i > 0) nameString += ", ";
    nameString += names[i];
  }
  return (
    <Modal
      id="Dialogue-prompt-modal"
      isSmall={true}
      title={header}
      isOpen={true}
      onClose={handleCancelDialogue}
      actions={[
        <Button key={option} variant="primary" onClick={handleConfirmDialogue}>
          Confirm
        </Button>,
        <Button key="cancel" variant="link" onClick={handleCancelDialogue}>
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

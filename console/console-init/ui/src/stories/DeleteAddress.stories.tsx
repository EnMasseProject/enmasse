/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { createElement } from "react";
import { MemoryRouter } from "react-router";
import { Button } from "@patternfly/react-core";
import { DialoguePrompt } from "components/common/DialoguePrompt";
import { text } from "@storybook/addon-knobs";

export default {
  title: "Address"
};

export const deleteAddressPrompt = () => {
  return createElement(() => {
    const [isOpen, setIsOpen] = React.useState(false);
    const handleCancel = () => setIsOpen(!isOpen);
    const handleDelete = () => setIsOpen(!isOpen);
    return (
      <MemoryRouter>
        <Button
          onClick={() => {
            setIsOpen(!isOpen);
          }}
        >
          Open Modal On Delete
        </Button>
        <DialoguePrompt
          option="Delete"
          header={text("Header", "Delete the Address ?")}
          names={[text("Name at top of details", "leo_b")]}
          detail={text(
            "Details",
            "There are some description that telling users what would happen after deleting this address."
          )}
          handleConfirmDialogue={handleDelete}
          handleCancelDialogue={handleCancel}
        />
      </MemoryRouter>
    );
  });
};

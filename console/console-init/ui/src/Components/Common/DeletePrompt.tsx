import * as React from "react";
import { Modal, Button } from "@patternfly/react-core";

interface IDeleteProps {
  header: string;
  detail: string;
  name: string;
  handleCancelDelte: () => void;
  handleConfirmDelete: () => void;
}
export const DeletePrompt: React.FunctionComponent<IDeleteProps> = ({
  header,
  detail,
  name,
  handleCancelDelte,
  handleConfirmDelete
}) => {
  return (
    <Modal
      isSmall={true}
      title={header}
      isOpen={true}
      onClose={handleCancelDelte}
      actions={[
        <Button key="Delete" variant="primary" onClick={handleConfirmDelete}>
          Confirm
        </Button>,
        <Button key="cancel" variant="link" onClick={handleCancelDelte}>
          Cancel
        </Button>
      ]}
      isFooterLeftAligned={true}
    >
      <b>{name}</b>
      <br />
      {detail}
    </Modal>
  );
};

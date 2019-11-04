import * as React from 'react';
import { Modal, Button } from '@patternfly/react-core';

interface IDeleteProps {
  header: string;
  detail: string;
  name: string;
  isOpen: boolean;
  setIsOpen: (isOpen: boolean) => void;
}
export const Delete: React.FunctionComponent<IDeleteProps> = ({
  header,
  detail,
  name,
  isOpen,
  setIsOpen,
}) => {
  const handleModalToggle = () => {
    setIsOpen(!isOpen);
  };
  return (
    <Modal
      isSmall
      title={header}
      isOpen={isOpen}
      onClose={handleModalToggle}
      actions={[
        <Button key="Delete" variant="primary" onClick={handleModalToggle}>
          Confirm
        </Button>,
        <Button key="cancel" variant="link" onClick={handleModalToggle}>
          Cancel
        </Button>,
      ]}
      isFooterLeftAligned
    >
      <b>{name}</b>
      <br />
      {detail}
    </Modal>
  );
};

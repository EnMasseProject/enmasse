import React, {Component} from 'react';
import {Modal, Button} from '@patternfly/react-core';

const deleteInstanceModal = (props) => {


  return (
    <Modal
      isSmall
      title="Delete?"
      isOpen={props.isOpen}
      onClose={props.handleDeleteModalToggle}
      actions={[
        <Button id="button-delete-cancel" key="cancel" variant="secondary" onClick={props.handleDeleteModalToggle}>
          Cancel
        </Button>,
        <Button id="button-delete" key="confirm" variant="primary" onClick={() => props.handleDelete(props.addNotification)}>
          Delete
        </Button>
      ]}
    >
      Once you delete the instance(s), all the related content and configurations will be deleted. Do you still want
      to delete?
    </Modal>
  );
}

export default deleteInstanceModal;

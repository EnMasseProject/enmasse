import React, {Component} from 'react';
import {Modal, Button, Form, TextInput} from '@patternfly/react-core';
import ConfigurationForm from "../ConfigureAddressSpace/ConfigurationForm";
import {editMessagingInstance, loadAddressSpace} from "../EnmasseAddressSpaces";
import CreateAddressSpace from "../CreateAddressSpace/CreateAddressSpace";

class EditAddressSpace extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      addressSpaceInstance: {},
      };
  };

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevProps.editInstance && prevProps.editInstance() !== this.props.editInstance()
      || (!prevProps.isOpen && this.props.isOpen)) {
      this.loadInstance(this.props.editInstance);
    }
  }

  loadInstance(editInstance) {
    if (editInstance() != null) {
      loadAddressSpace(editInstance().namespace, editInstance().name)
        .then(response => {
          this.setState((prevState) => ({ addressSpaceInstance: {...prevState.addressSpaceInstance, ...response} }));
        })
        .catch(error => {
          console.log("Could load Address Space: ",error);
        });
    }
  }

  handlePlanChange = (isValid, value) => {
    this.setState(
      {
        addressSpaceInstance: value
      }
    )};

  onSave = (addNotification) => {
      editMessagingInstance(this.state.addressSpaceInstance.name, this.state.addressSpaceInstance.namespace, this.state.addressSpaceInstance.plan)
        .then(response => {
           addNotification('success', 'Edited plan on '+ this.state.addressSpaceInstance.name);
        })
        .catch(error => {
          console.log('FAILED to edit name <' + this.state.addressSpaceInstance.name + '> namespace <' + this.state.addressSpaceInstance.namespace + '>', error);
          if (error.response) {
            addNotification('danger', 'Failed to edit '+ this.state.addressSpaceInstance.name, error.response.data.message);
          } else {
            addNotification('danger', 'Failed to edit '+ this.state.addressSpaceInstance.name);
          }
        })
        .finally(() => this.props.handleEdit(addNotification));
  };

  render() {

    let isConfigurationFormValid = Boolean(true);

    return (
      <Modal
        isLarge
        title="Edit"
        isOpen={this.props.isOpen}
        onClose={this.props.handleEditModalToggle}
        actions={[
          <Button id="button-edit-cancel" key="cancel" variant="secondary" onClick={this.props.handleEditModalToggle}>
            Cancel
          </Button>,
          <Button id="button-edit-save" key="confirm" variant="primary"
                  onClick={() => this.onSave(this.props.addNotification)}>
            Save
          </Button>
        ]}
      >
        <ConfigurationForm
          newInstance={this.state.addressSpaceInstance}
          isConfigurationFormValid={isConfigurationFormValid}
          isReadOnly={true}
          title="Choose a new plan."
          onChange={this.handlePlanChange}
        />
      </Modal>
    );
  }
}

export default EditAddressSpace;

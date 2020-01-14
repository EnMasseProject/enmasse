import React from 'react';
import {
  Dropdown, DropdownItem, DropdownPosition,
  KebabToggle,
} from '@patternfly/react-core';

import {deleteMessagingInstances} from '../MessagingInstance/Enmasse/EnmasseAddressSpaces';


class InstancesActionKebab extends React.Component {

  constructor(props) {
    super(props);

    const del = deleteMessagingInstances;

    this.state = {
      isKebabOpen: false,
    }
  }

  onKebabToggle = isOpen => {
    this.setState({
      isKebabOpen: isOpen
    });
  };

  onKebabSelect = event => {
    this.setState({
      isKebabOpen: !this.state.isKebabOpen
    });
  };

  render() {

    const {isKebabOpen: isInstancesKebabOpen} = this.state;

    return (
        <Dropdown
          onToggle={this.onKebabToggle}
          onSelect={this.onKebabSelect}
          position={DropdownPosition.right}
          toggle={<KebabToggle onToggle={this.onKebabToggle}/>}
          isOpen={isInstancesKebabOpen}
          isPlain
          dropdownItems={[
            <DropdownItem id="dd-menuitem-delete" isDisabled={!this.props.hasSelectedRows} component="button" key="delete"
                          onClick={this.props.openDeleteModal}>Delete</DropdownItem>,
          ]}
        />
    );
  }
}

export default InstancesActionKebab;

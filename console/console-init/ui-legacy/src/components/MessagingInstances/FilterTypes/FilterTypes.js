import React, {Component} from 'react';

import { Dropdown, DropdownToggle, DropdownItem } from '@patternfly/react-core';
import {FilterIcon} from '@patternfly/react-icons';

const filterTypes = [
    { label: 'Name', type: 'name'},
    { label: 'Namespace', type: 'namespace'},
    { label: 'Type', type: 'type'}
  ];

class FilterTypes extends Component {

  state = {
    isOpen: false,
    selectedFilterType: 'name',
  }

  onSelectDropdown = (event) => {
    this.setState({
      isOpen: !this.state.isOpen
    });
  };

  onToggle = isOpen => {
    this.setState({
      isOpen
    });
  };

  render() {
    let items = filterTypes.map(type => (
      <DropdownItem key={type.type} component="button" onClick={() => props.onFilterSelect(type.type)}>{type.label}</DropdownItem>
    ));

    <Dropdown
      onSelect={this.onSelectDropdown}
      toggle={<DropdownToggle onToggle={this.onToggle}><FilterIcon/> {selectedFilterType}</DropdownToggle>}
      isOpen={isOpen}
      dropdownItems={(items)}
    />

  }
}


export default FilterTypes;

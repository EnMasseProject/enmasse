import React from 'react';

import {configure, shallow} from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import EditAddressSpace from './EditAddressSpace';
import NamespaceInput from '../ConfigureAddressSpace/NamespaceInput';
import ConfigurationForm from '../ConfigureAddressSpace/ConfigurationForm';
import {FormSelectOption} from "@patternfly/react-core";
import PlansFormGroup from "../ConfigureAddressSpace/PlansInput";
import {Modal, Button, Form, TextInput} from '@patternfly/react-core';

configure({adapter: new Adapter()});

describe('< EditAddressSpace />', () => {
  let wrapper;
  let addressSpaceInstance = {};

  it('Loads the form in readonly mode', () => {

    wrapper = shallow(<EditAddressSpace newInstance={addressSpaceInstance}
                                        isConfigurationFormValid={() => {}}
                                        isReadOnly={true}
                                        title="Choose a new plan."
                                        onChange={() => {}}
    />);

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find(ConfigurationForm).props().isReadOnly).toBeTruthy();
  });

});

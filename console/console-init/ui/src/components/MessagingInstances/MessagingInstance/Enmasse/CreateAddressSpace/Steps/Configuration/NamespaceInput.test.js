import React from 'react';

import {configure, shallow} from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import NamespaceInput from './NamespaceInput';
import {FormSelectOption} from "@patternfly/react-core";

configure({adapter: new Adapter()});

describe('< Namespace Input />', () => {
  let wrapper;

  let newInstance = {
    namespace: '',
  };
  let namespaces = ['testNamespace1', 'testNamespace2'];


  it('If running in Openshift show the select box', () => {

    wrapper = shallow(<NamespaceInput namespaces={namespaces} handleNamespaceChange={()=>{}}/>);

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find(FormSelectOption)).toHaveLength(2);
    expect(wrapper.find('TextInput')).toHaveLength(0);
   });

  it('If running in Kubernetes show the select box', () => {

    wrapper = shallow(<NamespaceInput namespaces={[]} handleNamespaceChange={()=>{}}/>);

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('TextInput')).toHaveLength(1);
    expect(wrapper.find(FormSelectOption)).toHaveLength(0);
  });

});

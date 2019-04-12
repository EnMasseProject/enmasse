import React from 'react';

import {configure, shallow} from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import ConfigurationForm from './ConfigurationForm';
import PlansFormGroup from './PlansInput';

configure({adapter: new Adapter()});

describe('< Configuration Form />', () => {
  let wrapper;

  let newInstance = {
    name: '',
    namespace: '',
    typeStandard: true,
    typeBrokered: false,
  };

  beforeEach(() => {
    wrapper = shallow(<ConfigurationForm newInstance={newInstance} isValid={()=>{}}/>);
  });

  it('Brokered Plans should display when type is brokered', () => {

    newInstance.typeBrokered = true;
    newInstance.typeStandard= false;

    wrapper.setState(newInstance);
    expect(wrapper.find(PlansFormGroup).props().typeBrokered).toBeTruthy();
    expect(wrapper.find(PlansFormGroup).props().typeStandard).toBeFalsy();
  });

  it('Standard Plans should display when type is standard', () => {

    newInstance.typeBrokered = false;
    newInstance.typeStandard= true;

    wrapper.setState(newInstance);
    expect(wrapper.find(PlansFormGroup).props().typeStandard).toBeTruthy();
    expect(wrapper.find(PlansFormGroup).props().typeBrokered).toBeFalsy();
  });
});

import React from 'react';

import {FormSelectOption} from "@patternfly/react-core";

import {configure, shallow} from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import PlansFormGroup from './PlansInput';

configure({adapter: new Adapter()});

describe('< PlansFormGroup />', () => {

  const brokeredPlanNames = ["brokered-single-broker"];
  const standardPlanNames = ['standard-small', 'standard-unlimited', 'standard-medium', 'standard-unlimited-with-mqtt'];

  let wrapper;

  beforeEach(() => {
    let instance = {plan: null};

    wrapper = shallow(<PlansFormGroup
      standardPlans={standardPlanNames}
      brokeredPlans={brokeredPlanNames}
      newInstance={instance}
    />);

  });

  it('Display brokered plans when brokered is selected', () => {
    wrapper.setProps({
      typeStandard: false,
      typeBrokered: true
    });
    expect(wrapper.find(FormSelectOption)).toHaveLength(1);
  });


  it('Display brokered plans when brokered is selected', () => {
    wrapper.setProps({
      typeStandard: true,
      typeBrokered: false
    });
    expect(wrapper.find(FormSelectOption)).toHaveLength(4);

  });
});

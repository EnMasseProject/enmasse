import React from 'react';

import {configure, shallow} from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import {AboutModal} from '@patternfly/react-core';

import About from './About';

configure({adapter: new Adapter()});

describe('< About Box />', () => {
  let wrapper;
  const OLD_ENV = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...OLD_ENV };
    delete process.env.NODE_ENV;

    process.env.REACT_APP_NAME="Enmasse Console";
  });

  afterEach(() => {
    process.env = OLD_ENV;
  });

  it('Test bg image and logo is set', () => {

    wrapper = shallow(<About />);

    expect(wrapper.find(AboutModal).props().brandImageSrc).toBeDefined();
    expect(wrapper.find(AboutModal).props().brandImageAlt).toEqual("Enmasse Console");
    expect(wrapper.find(AboutModal).props().backgroundImageSrc).toBeDefined();
  });


});

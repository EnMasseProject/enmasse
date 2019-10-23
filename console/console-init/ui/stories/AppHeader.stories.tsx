import * as React from 'react';
import { storiesOf } from '@storybook/react';
import { withKnobs, boolean } from '@storybook/addon-knobs';
import { action } from '@storybook/addon-actions';
import { AppLayout } from 'use-patternfly';
import { Avatar, Brand, Text, TextVariants, Dropdown, DropdownToggle, DropdownPosition, DropdownItem } from '@patternfly/react-core';
import { CogIcon } from '@patternfly/react-icons';
import avatarImg from '../src/logo.svg';
import brandImg from '../src/brand_logo.svg';

const stories = storiesOf('Utils', module);
stories.addDecorator(withKnobs);

const avatar = (
  <React.Fragment>
    <Text component={TextVariants.p}>Ramakrishna Pattnaik</Text>
    <Avatar src={avatarImg} alt="avatar" />
  </React.Fragment>
);
const dropdownItems = [
  <DropdownItem key="help">Help</DropdownItem>,
  <DropdownItem key="About">About</DropdownItem>
];
const NavToolBar = (
  <Dropdown
    position={DropdownPosition.right}
    toggle={
      <DropdownToggle iconComponent={null} aria-label="Applications">
        <CogIcon />
      </DropdownToggle>
    }
    isOpen={boolean("keep toolbar open", true)}
    isPlain
    dropdownItems={dropdownItems}
  />
)
const logo = <Brand src={brandImg} alt="Console Logo" />;

stories.add('Page Header', () => (
  <AppLayout
    logoProps={{
      onClick: action('Logo clicked'),
    }}
    logo={logo}
    avatar={avatar}
    toolbar={NavToolBar}
  ></AppLayout>
));
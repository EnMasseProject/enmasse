import React from 'react';
import {
  Avatar,
  Brand,
  Dropdown,
  DropdownToggle,
  DropdownItem,
  Nav,
  NavItem,
  NavList,
  NavVariants,
  Page,
  PageHeader,
  PageSection,
  PageSectionVariants,
  TextContent,
  Toolbar,
  ToolbarGroup,
  ToolbarItem
} from '@patternfly/react-core';
import accessibleStyles from '@patternfly/patternfly/utilities/Accessibility/accessibility.css';
import spacingStyles from '@patternfly/patternfly/utilities/Spacing/spacing.css';
import { css } from '@patternfly/react-styles';
import { CogIcon } from '@patternfly/react-icons';

import Aux from '../../hoc/Aux/Aux';
import About from '../About/About';

import avatarImg from "../../assets/images/img_avatar.svg";
import brandImg from '../../assets/images/logo.svg';

import './Layout.css';

class Layout extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isHelpDropdownOpen: false,
      isAboutModalOpen: false,
      activeItem: 0,
    };
  }

  handleAboutModalToggle = () => {
    this.setState(({isAboutModalOpen}) => ({
      isAboutModalOpen: !isAboutModalOpen,
      isHelpDropdownOpen: false
    }));
  };

  onDropdownToggle = isDropdownOpen => {
    this.setState({
      isDropdownOpen
    });
  };

  onDropdownSelect = event => {
    this.setState({
      isDropdownOpen: !this.state.isDropdownOpen
    });
  };

  onHelpDropdownToggle = isHelpDropdownOpen => {
    this.setState({
      isHelpDropdownOpen
    });
  };

  onHelpDropdownSelect = event => {
    this.setState({
      isHelpDropdownOpen: !this.state.isHelpDropdownOpen
    });
  };

  onNavSelect = result => {
    this.setState({
      activeItem: result.itemId
    });
  };

  convertCamelCaseToTitle(text) {
    var result = text.replace( /([A-Z])/g, " $1" );
    return result.charAt(0).toUpperCase() + result.slice(1);
  }

  render() {
    const { isDropdownOpen, isHelpDropdownOpen, activeItem, isAboutModalOpen } = this.state;

    var style = {
      textAlign: 'center',
    };

    const navItems = Object.keys(this.props.instanceTypes)
      .map((key, i )=> {
          return <NavItem to={"#nav-link"+i} key={i} itemId={i} isActive={activeItem === i}>
            <div style={style}>{this.props.instanceTypes[key]}<br/>{this.convertCamelCaseToTitle(key)}</div>
          </NavItem>
        });

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList variant={NavVariants.horizontal}>
          {navItems}
        </NavList>
      </Nav>
    );
    const helpDropdownItems = [
      <DropdownItem key="About" onClick={this.handleAboutModalToggle}>About</DropdownItem>
    ];
    const userDropdownItems = [
      <DropdownItem key={"logout"} href="oauth/sign_in">Logout</DropdownItem>
    ];
    const PageToolbar = (
      <Toolbar>
        <ToolbarGroup>
          <ToolbarItem className={css(accessibleStyles.hiddenOnLg, spacingStyles.mr_0)}>
            <Dropdown
              isPlain
              position="right"
              onSelect={this.on171DropdownSelect}
              toggle={<DropdownToggle onToggle={this.onHelpDropdownToggle}><CogIcon/></DropdownToggle>}
              isOpen={isHelpDropdownOpen}
              dropdownItems={helpDropdownItems}
            />
          </ToolbarItem>
          <ToolbarItem className={css(accessibleStyles.screenReader, accessibleStyles.visibleOnMd)}>
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={<DropdownToggle onToggle={this.onDropdownToggle}>{this.props.user}</DropdownToggle>}
              dropdownItems={userDropdownItems}
            />
          </ToolbarItem>
        </ToolbarGroup>
      </Toolbar>
    );

    const Header = (
      <PageHeader
        logo={<Brand src={brandImg} alt="Console Logo" />}
        toolbar={PageToolbar}
        avatar={<Avatar src={avatarImg} alt="Avatar image" />}
        topNav={PageNav}
      />
    );

    return (
      <Aux>
        <Page header={Header} >
          {/*<PageSection variant={PageSectionVariants.darker} className='navSection'>{PageNav}</PageSection>*/}
          <PageSection variant={PageSectionVariants.light}>
            <About handleAboutModalToggle={this.handleAboutModalToggle} isAboutModalOpen={isAboutModalOpen}/>
            <TextContent>
              {this.props.children}
            </TextContent>
          </PageSection>
        </Page>
      </Aux>
    );
  }
}

export default Layout;

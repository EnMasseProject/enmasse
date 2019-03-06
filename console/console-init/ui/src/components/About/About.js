import React, {Component} from 'react';
import {AboutModal, TextContent, TextList, TextListItem} from '@patternfly/react-core';
import brandImg from './pf_mini_logo_white.svg';
import logoImg from './pf_logo.svg';

class About extends Component {
  render() {
    return (
      <AboutModal
        isOpen={this.props.isAboutModalOpen}
        onClose={this.props.handleAboutModalToggle}
        productName={process.env.REACT_APP_NAME}
        brandImageSrc={brandImg}
        brandImageAlt="Patternfly Logo"
        logoImageSrc={logoImg}
        logoImageAlt="Patternfly Logo"
      >
        <TextContent>
          <TextList component="dl">
            <TextListItem component="dt">Version</TextListItem>
            <TextListItem component="dd">{process.env.REACT_APP_VERSION}</TextListItem>
          </TextList>
        </TextContent>
      </AboutModal>
    )
  }
}



export default About;

import React, {Component} from 'react';
import {AboutModal, TextContent, TextList, TextListItem} from '@patternfly/react-core';
import brandImg from './pf_mini_logo_white.svg';

class About extends Component {


  render() {
    let productName = process.env.REACT_APP_NAME ;
    let productVersion = process.env.REACT_APP_VERSION;
    if (!productName) {
      productName = '';
      console.log('Product name not set in process.env.REACT_APP_NAME');
    }
    if (!productVersion) {
      productVersion = '';
      process.env.REACT_APP_VERSION
    }

    return (
      <AboutModal
        isOpen={this.props.isAboutModalOpen}
        onClose={this.props.handleAboutModalToggle}
        productName={productName}
         brandImageSrc={brandImg}
         brandImageAlt="Patternfly Logo"
      >
        <TextContent>
          <TextList component="dl">
            <TextListItem component="dt">Version</TextListItem>
            <TextListItem component="dd">{productVersion}</TextListItem>
            <TextListItem component="dt">Copyright</TextListItem>
            <TextListItem component="dd">Apache License, Version 2.0</TextListItem>
          </TextList>
        </TextContent>
      </AboutModal>
    )
  }
}



export default About;

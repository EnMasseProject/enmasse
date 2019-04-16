import React, {Component} from 'react';
import {AboutModal, TextContent, TextList, TextListItem} from '@patternfly/react-core';
import EmptyImg from '../../assets/images/Empty.svg';
import Aux from '../../hoc/Aux/Aux';

class About extends Component {


  render() {
    let productName = process.env.REACT_APP_NAME;
    let productVersion = process.env.REACT_APP_VERSION;
    if (!productName) {
      productName = '';
      console.log('Product name not set in process.env.REACT_APP_NAME');
    }
    if (!productVersion) {
      productVersion = '';
      console.log('process.env.REACT_APP_VERSION is not set');
    }
    let docs = process.env.REACT_APP_DOCS;
    let docsElement;
    if (docs) {
      docsElement=
        <Aux>
          <TextListItem component="dt">Documentation</TextListItem>
          <TextListItem component="dd"><a href={process.env.REACT_APP_DOCS}>{process.env.REACT_APP_DOCS}</a></TextListItem>
        </Aux>;
    }

    return (
      <AboutModal
        isOpen={this.props.isAboutModalOpen}
        onClose={this.props.handleAboutModalToggle}
        productName={productName}
        brandImageSrc={EmptyImg}
        brandImageAlt=""
      >
        <TextContent>
          <TextList component="dl">
            <TextListItem component="dt">EnMasse Version</TextListItem>
            <TextListItem component="dd">{productVersion}</TextListItem>
            {(docsElement)}
          </TextList>
        </TextContent>
      </AboutModal>
    )
  }
}


export default About;

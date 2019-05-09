import React from 'react';
import {
  Card, CardHeader, CardBody,
  Form, FormGroup,
  Split, SplitItem,
  Text,
  Title,
} from '@patternfly/react-core';


import './Review.css';

class Review extends React.Component {
  state = {
    newInstance: this.props.newInstance,
  };

  render() {
    const {newInstance} = this.state;

    return (
      <Split gutter="lg">
        <SplitItem isMain>
          <Card id="xxx" name="yyy" className="noBoShadow">
            <CardHeader>
              <Title size="md">Review the information below and click Finish to create a new instance. Use the Back
                button to make changes.</Title>
            </CardHeader>
            <CardBody>
              <Form isHorizontal>
                <FormGroup label="Instance name" fieldId="name">
                  <Text>{newInstance.name}</Text>
                </FormGroup>
                <FormGroup label="Namespace" fieldId="namespace">
                  <Text>{newInstance.namespace}</Text>
                </FormGroup>
                <FormGroup isInline label="Type" fieldId="type">
                  <Text>{newInstance.typeStandard ? 'Standard' : 'Brokered'} address space</Text>
                </FormGroup>
                <FormGroup label="Address space plan" fieldId={"plan"}>
                  <Text>{newInstance.plan}</Text>
                </FormGroup>
                <FormGroup label="Authentication service" fieldId={"auth-service"}>
                  <Text>{newInstance.authenticationService}</Text>
                </FormGroup>
              </Form>
            </CardBody>
          </Card>
        </SplitItem>

        <SplitItem isMain>
          <Text></Text>
        </SplitItem>
      </Split>
    )
      ;
  }
}

export default Review;

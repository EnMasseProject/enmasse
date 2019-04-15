import React from 'react';
import {FormGroup, FormSelect, FormSelectOption} from "@patternfly/react-core";


const authenticationServiceInput = (props) => {

  const authServices = props.typeStandard ? props.standardAuthenticationServices : props.brokeredAuthenticationServices;

  return (
    <FormGroup
      isRequired
      label="Authentication Service"
      fieldId="form-authenticationService">
      <FormSelect
        value={props.authenticationService}
        onChange={props.handleAuthenticationServiceChange}
        id="form-namespace"
        name="form-namespace"
      >
        {
          authServices.map((option, index) => (
            <FormSelectOption key={index} value={option}
                              label={option}/>
          ))
        }
      </FormSelect>
    </FormGroup>
  );
}


export default authenticationServiceInput;

import React from 'react';
import {FormGroup, FormSelect, FormSelectOption, TextInput} from "@patternfly/react-core";

import InstanceLoader from '../../../../InstanceLoader';

const namespaceInput = (props) => {


  let textField = <TextInput
    isRequired
    type="text"
    id="form-namespace"
    name="form-namespace"
    value={props.namespace}
    onChange={props.handleNamespaceChange}
  />;

  let selectBox = <FormSelect
    value={props.namespace}
    onChange={props.handleNamespaceChange}
    id="form-namespace"
    name="form-namespace"
  >
    {
      props.namespaces.map((option, index) => (
        <FormSelectOption key={index} value={option}
                          label={option}/>
      ))
    }
  </FormSelect>;

  return (
    <FormGroup
      isRequired
      label="Namespace"
      fieldId="form-namespace">
      {props.namespaces.length >0 ? selectBox : textField}
    </FormGroup>
  );
}


export default namespaceInput;

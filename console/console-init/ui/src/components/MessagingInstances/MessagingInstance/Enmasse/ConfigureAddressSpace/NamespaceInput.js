import React from 'react';
import {FormGroup, FormSelect, FormSelectOption, TextInput} from "@patternfly/react-core";

const namespaceInput = (props) => {


  let textField = <TextInput
    isRequired
    isReadOnly={props.isReadOnly}
    type="text"
    id="form-namespace"
    name="form-namespace"
    value={props.namespace}
    onChange={props.handleNamespaceChange}
  />;

  let selectBox = <FormSelect
    isDisabled={props.isReadOnly}
    value={props.namespace}
    onChange={props.handleNamespaceChange}
    id="form-namespace"
    name="form-namespace"
  >
    {
      props.namespaces &&
      props.namespaces.map((option, index) => (
        <FormSelectOption key={index} value={option} label={option}/>
        ))
    }
  </FormSelect>;


  return (
    <FormGroup
      isRequired
      isReadOnly={props.isReadOnly}
      label="Namespace"
      fieldId="form-namespace">
      {props.namespaces && props.namespaces.length >0 ? selectBox : textField}
    </FormGroup>
  );
}


export default namespaceInput;

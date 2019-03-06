import React from 'react';
import {FormGroup, FormSelect, FormSelectOption} from "@patternfly/react-core";

const namespaceInput = (props) => {


  return (
    <FormGroup
      isRequired
      label="Namespace"
      fieldId="form-namespace">
      <FormSelect
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
      </FormSelect>
    </FormGroup>
  );
}


export default namespaceInput;

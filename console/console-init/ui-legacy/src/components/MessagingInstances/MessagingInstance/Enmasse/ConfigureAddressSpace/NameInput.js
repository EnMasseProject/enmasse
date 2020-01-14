import React from 'react';
import {FormGroup, TextInput} from "@patternfly/react-core";

const namespaceInput = (props) => {

  const newInstance = props.newInstance;

  return (
    <FormGroup
      label="Name"
      isRequired
      fieldId="name"
    >
      <TextInput
        isRequired
        isReadOnly={props.isReadOnly}
        type="text"
        id="form-name"
        name="form-name"
        aria-describedby="formname"
        value={newInstance.name}
        onChange={props.handleNameChange}
      />
    </FormGroup>

  );
}


export default namespaceInput;

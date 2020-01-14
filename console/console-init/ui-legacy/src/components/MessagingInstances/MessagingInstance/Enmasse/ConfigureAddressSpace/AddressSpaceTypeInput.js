import React from 'react';
import {FormGroup, Radio} from "@patternfly/react-core";

const addressSpaceTypeInput = (props) => {

  const newInstance = props.newInstance;
  return (
    <FormGroup
      isInline
      label="Type"
      isRequired
      fieldId="addressSpaceType">
      <Radio id="radio-addressspace-standard" name="addressSpaceType" value={newInstance.typeStandard}
             checked={newInstance.typeStandard} label="Standard" aria-label="Standard"
             onChange={props.handleTypeStandardChange} isDisabled={props.isReadOnly}/>
      <Radio id="radio-addressspace-brokered" name="addressSpaceType" value={newInstance.typeBrokered}
             checked={newInstance.typeBrokered} label="Brokered" aria-label="Brokered"
             onChange={props.handleTypeBrokeredChange} isDisabled={props.isReadOnly}/>
    </FormGroup>
  );
}


export default addressSpaceTypeInput;

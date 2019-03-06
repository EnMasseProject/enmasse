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
      <Radio id="addressSpaceType1" name="addressSpaceType" value={newInstance.typeStandard}
             checked={newInstance.typeStandard} label="Standard" aria-label="Standard"
             onChange={props.handleTypeStandardChange}/>
      <Radio id="addressSpaceType2" name="addressSpaceType" value={newInstance.typeBrokered}
             checked={newInstance.typeBrokered} label="Brokered" aria-label="Brokered"
             onChange={props.handleTypeBrokeredChange}/>
    </FormGroup>
  );
}


export default addressSpaceTypeInput;

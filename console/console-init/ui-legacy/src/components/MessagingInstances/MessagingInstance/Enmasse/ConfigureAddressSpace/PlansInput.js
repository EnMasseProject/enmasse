import React from 'react';
import {FormGroup, FormSelect, FormSelectOption} from "@patternfly/react-core";

const plansFormGroup = (props) => {

  const plans = props.typeStandard ? props.standardPlans : props.brokeredPlans;

  return (
    <FormGroup
      isRequired
      label="Address space plan"
      fieldId="form-planName">
      <FormSelect
        value={props.plan}
        onChange={props.handlePlanChange}
        id="form-planName"
        name="form-planName"
      >
        {
          plans.map((option, index) => (
            <FormSelectOption key={index} value={option}
                              label={option}/>
          ))
        }
      </FormSelect>
    </FormGroup>
  );
}


export default plansFormGroup;

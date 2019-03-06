import React from 'react';
import {
  Form,
  Title
} from '@patternfly/react-core';

import PlansInput from './PlansInput';
import AddressSpaceTypeInput from './AddressSpaceTypeInput';
import NameInput from './NameInput';
import NamespaceInput from './NamespaceInput';

class ConfigurationForm extends React.Component {
  state = {
    newInstance: this.props.newInstance,
    isValid: this.props.isConfigurationFormValid,
  };

  handleNameChange = name => {
    var newInstance = {...this.state.newInstance};
    newInstance.name = name;

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleNamespaceChange = namespace => {
    var newInstance = {...this.state.newInstance};
    newInstance.namespace = namespace;
    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleTypeStandardChange = (value) => {
    var newInstance = {...this.state.newInstance};
    newInstance.typeStandard = value;
    newInstance.typeBrokered = !value;
    newInstance.plan = this.props.standardPlans[0];

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleTypeBrokeredChange = (value) => {
    var newInstance = {...this.state.newInstance};
    newInstance.typeBrokered = value;
    newInstance.typeStandard = !value;
    newInstance.plan = this.props.brokeredPlans[0];

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handlePlanChange = plan => {
    var newInstance = {...this.state.newInstance};
    newInstance.plan = plan;
    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }


  isValid(instance) {
    return Boolean(instance.name && instance.plan && instance.namespace && (instance.typeBrokered || instance.typeStandard));
  }

  render() {
    const {newInstance} = this.state;

    return (
      <Form>
        <Title size={"xl"}>Configure your component</Title>
        <NamespaceInput
          handleNamespaceChange={this.handleNamespaceChange}
          namespace={newInstance.namespace}
          namespaces={this.props.namespaces}
        />
        <NameInput
          newInstance={newInstance}
          handleNameChange={this.handleNameChange}
        />
        <AddressSpaceTypeInput
          newInstance={newInstance}
          handleTypeStandardChange={this.handleTypeStandardChange}
          handleTypeBrokeredChange={this.handleTypeBrokeredChange}
        />
        <PlansInput
          newInstance={newInstance}
          handlePlanChange={this.handlePlanChange}
          standardPlans={this.props.standardPlans}
          brokeredPlans={this.props.brokeredPlans}
          typeStandard={newInstance.typeStandard}
          typeBrokered={newInstance.typeBrokered}
        />
      </Form>
    )
      ;
  }
}

export default ConfigurationForm;

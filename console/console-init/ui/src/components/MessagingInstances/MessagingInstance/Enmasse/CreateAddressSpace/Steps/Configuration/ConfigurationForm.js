import React from 'react';
import {
  Form,
  Title
} from '@patternfly/react-core';

import AuthenticationServiceInput from './AuthenticationServiceInput';
import PlansInput from './PlansInput';
import AddressSpaceTypeInput from './AddressSpaceTypeInput';
import NameInput from './NameInput';
import NamespaceInput from './NamespaceInput';
import {
  loadBrokeredAddressPlans, loadBrokeredAuthenticationServices,
  loadStandardAddressPlans,
  loadStandardAuthenticationServices
} from "../../../EnmasseAddressSpaces";
import InstanceLoader from "../../../../InstanceLoader";

class ConfigurationForm extends React.Component {
  state = {
    newInstance: this.props.newInstance,
    isValid: this.props.isConfigurationFormValid,
    standardPlans:[],
    brokeredPlans:[],
    standardAuthenticationServices:[],
    brokeredAuthenticationServices:[]
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
    this.loadOptionsForNamespace(namespace);
  }

  componentDidMount() {
    InstanceLoader.loadNamespaces()
      .then(namespaces => {
        this.setState({namespaces: namespaces});

        if ( (!this.state.standardPlans || this.state.standardPlans.length==0)
          && (!this.state.brokeredPlans || this.state.brokeredPlans.length==0)
          && (!this.state.standardAuthenticationServices || this.state.standardAuthenticationServices.length==0)
          && (!this.state.brokeredAuthenticationServices || this.state.brokeredAuthenticationServices.length==0)) {

          if (!this.state.newInstance.namespace
            || !namespaces.includes(this.state.newInstance.namespace)) {
            this.setState(state => {
              var newInstance = {...state.newInstance};

              newInstance.namespace = namespaces[0];
              this.loadOptionsForNamespace(namespaces[0]);
              return {newInstance: newInstance};
            });
          }
          this.loadOptionsForNamespace(namespaces[0]);
        }
      })
      .catch(error => {
        console.log("Couldn't set the namespaces instances: ", error);
      });
  };

  loadOptionsForNamespace = namespace => {
    loadStandardAddressPlans(namespace)
      .then(plans => {
        this.setState({standardPlans: plans});
        if (this.state.newInstance.typeStandard
          && !this.state.standardPlans.find(plan => plan==newInstance.plan)) {
          var newInstance = {...this.state.newInstance};
          newInstance.plan = this.state.standardPlans[0];
          this.setState({newInstance: newInstance});
        }
      })
      .catch(error => {
        console.log("Couldn't set the standard plans: ", error);
      });
    loadBrokeredAddressPlans(namespace)
      .then(plans => {

        this.setState({brokeredPlans: plans});
        if (this.state.newInstance.typeBrokered
          && !this.brokeredPlans.find(plan => plan==newInstance.plan)) {
          var newInstance = {...this.state.newInstance};
          newInstance.plan = this.brokeredPlans[0];
          this.setState({newInstance: newInstance});
        }
      })
      .catch(error => {
        console.log("Couldn't set the brokered plans: ", error);
      });
    loadStandardAuthenticationServices(namespace)
      .then(authenticationServices => {
        this.setState({standardAuthenticationServices: authenticationServices});
        if (this.state.newInstance.typeStandard
          && !this.state.standardAuthenticationServices.find(authService => authService==newInstance.authenticationService)) {
          var newInstance = {...this.state.newInstance};
          newInstance.authenticationService = this.state.standardAuthenticationServices[0];
          this.setState({newInstance: newInstance});
        }
      })
      .catch(error => {
        console.log("Couldn't set the authenticationServices: ", error);
      });
    loadBrokeredAuthenticationServices(namespace)
      .then(authenticationServices => {
        this.setState({brokeredAuthenticationServices: authenticationServices});
        this.state.brokeredAuthenticationServices = authenticationServices;
        if (this.state.newInstance.typeBrokered
          && !this.state.brokeredAuthenticationServices.find(authService => authService==newInstance.authenticationService)) {
          var newInstance = {...this.state.newInstance};
          newInstance.authenticationService = this.state.brokeredAuthenticationServices[0];
          this.setState({newInstance: newInstance});
        }
      })
      .catch(error => {
        console.log("Couldn't set the authenticationServices: ", error);
      });
  }

  handleAuthenticationServiceChange = authenticationService => {
    var newInstance = {...this.state.newInstance};
    newInstance.authenticationService = authenticationService;
    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleTypeStandardChange = (value) => {
    var newInstance = {...this.state.newInstance};
    newInstance.typeStandard = value;
    newInstance.typeBrokered = !value;
    newInstance.plan = this.state.standardPlans[0];
    if (!this.state.standardAuthenticationServices.find(authService => authService==newInstance.authenticationService)) {
      newInstance.authenticationService = this.state.standardAuthenticationServices[0];
    }

    this.props.onChange(this.isValid(newInstance), newInstance);
    this.setState({newInstance: newInstance});
  }

  handleTypeBrokeredChange = (value) => {
    var newInstance = {...this.state.newInstance};
    newInstance.typeBrokered = value;
    newInstance.typeStandard = !value;
    newInstance.plan = this.state.brokeredPlans[0];
    if (!this.state.brokeredAuthenticationServices.find(authService => authService==newInstance.authenticationService)) {
      newInstance.authenticationService = this.state.brokeredAuthenticationServices[0];
    }

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
    return Boolean(instance.authenticationService && instance.name && instance.plan && instance.namespace && (instance.typeBrokered || instance.typeStandard));
  }

  render() {
    const {newInstance} = this.state;

    return (
      <Form id="form-configuration">
        <Title size={"xl"}>Configure your component</Title>
        <NamespaceInput
          handleNamespaceChange={this.handleNamespaceChange}
          namespace={newInstance.namespace}
          namespaces={this.state.namespaces}
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
          plan={newInstance.plan}
          handlePlanChange={this.handlePlanChange}
          standardPlans={this.state.standardPlans}
          brokeredPlans={this.state.brokeredPlans}
          typeStandard={newInstance.typeStandard}
          typeBrokered={newInstance.typeBrokered}
        />
        <AuthenticationServiceInput
          authenticationService={newInstance.authenticationService}
          handleAuthenticationServiceChange={this.handleAuthenticationServiceChange}
          brokeredAuthenticationServices={this.state.brokeredAuthenticationServices}
          standardAuthenticationServices={this.state.standardAuthenticationServices}
          typeStandard={newInstance.typeStandard}
          typeBrokered={newInstance.typeBrokered}
        />
      </Form>
    )
      ;
  }
}

export default ConfigurationForm;

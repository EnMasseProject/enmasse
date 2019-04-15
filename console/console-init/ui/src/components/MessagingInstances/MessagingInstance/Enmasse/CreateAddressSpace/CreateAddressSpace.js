import React from 'react';
import {Button, BackgroundImageSrc, Wizard} from '@patternfly/react-core';

import ConfigurationForm from './Steps/Configuration/ConfigurationForm';
import Review from './Steps/Review';
import {
  createNewAddressSpace,
  loadBrokeredAddressPlans,
  loadStandardAddressPlans,
  loadStandardAuthenticationServices,
  loadBrokeredAuthenticationServices
} from '../EnmasseAddressSpaces';
import InstanceLoader from '../../InstanceLoader';
import {NotificationConsumer, NotificationProvider} from '../../../../../context/notification-manager';


class CreateAddressSpace extends React.Component {


  initialState = {
    isOpen: false,
    isConfigurationFormValid: Boolean(false),
    newInstance: {
      name: '',
      namespace: '',
      typeStandard: '',
      typeBrokered: '',
      plan: '',
      authenticationService: '',
    },
    brokeredPlans: [],
    standardPlans: [],
    namespaces: [],
    brokeredAuthenticationServices: [],
    standardAuthenticationServices: []
  };

  state = {...this.initialState};

  componentDidMount() {
    loadStandardAddressPlans()
      .then(plans => {
        this.state.standardPlans = plans;
      })
      .catch(error => {
        console.log("Couldn't set the standard plans: " + error);
      });
    loadBrokeredAddressPlans()
      .then(plans => {
        this.state.brokeredPlans = plans;
      })
      .catch(error => {
        console.log("Couldn't set the brokered plans: " + error);
      });
    loadStandardAuthenticationServices()
      .then(authenticationServices => {
        this.state.standardAuthenticationServices = authenticationServices;
      })
      .catch(error => {
        console.log("Couldn't set the authenticationServices: " + error);
      });
    loadBrokeredAuthenticationServices()
      .then(authenticationServices => {
        this.state.brokeredAuthenticationServices = authenticationServices;
      })
      .catch(error => {
        console.log("Couldn't set the authenticationServices: " + error);
      });
    InstanceLoader.loadNamespaces()
      .then(namespaces => {
        this.setState({namespaces: namespaces});
        if (!this.state.newInstance.namespace
          || !namespaces.includes(this.state.newInstance.namespace)) {
          this.setState(state => {
            var newInstance = {...state.newInstance};
            newInstance.namespace = namespaces[0];
            return {newInstance: newInstance};
          });
          this.initialState.newInstance.namespace = namespaces[0];
        }
      })
      .catch(error => {
        console.log("Couldn't set the namespaces instances: " + error);
      });
  };

  resetConfiguration = () => {
    this.state.newInstance.namespace = this.initialState.newInstance.namespace;
    this.state.newInstance.authenticationService = this.initialState.newInstance.authenticationService;
    this.state.newInstance.plan = '';
    this.state.newInstance.typeBrokered = this.state.newInstance.typeStandard = false;
    this.state.newInstance.name = '';
  }

  toggleOpen = () => {
    if (this.state.isOpen) {
      this.resetConfiguration();
    }
    this.setState(({isOpen: prevIsOpen}) => {
      return {isOpen: !prevIsOpen};
    });
  };

  onConfigurationFormChange = (isValid, value) => {
    this.setState(
      {
        isConfigurationFormValid: isValid,
        newInstance: value
      }
    );
  };

  onSave = (addNotification) => {
    let name = this.state.newInstance.name;
    createNewAddressSpace(this.state.newInstance)
      .then(response => {
        this.props.reload();
        addNotification('success', 'Successfully created '+ name);
      })
      .catch(error => {
        console.log('Failed to create address space <' + name + '>', error);
        if (error.response) {
          console.log(error.response.data);
          console.log(error.response.status);
          console.log(error.response.headers);
          addNotification('danger', 'Failed to create '+ name, error.response.data.message);
        } else {
          addNotification('danger', 'Failed to create name');
        }
      })
      .finally(() => this.toggleOpen());
  };


  render() {
    const {isOpen, isConfigurationFormValid, newInstance, allStepsValid} = this.state;

    const steps = [
      {
        id: 1,
        name: 'Configuration',
        component: (<ConfigurationForm newInstance={newInstance}
                                       isConfigurationFormValid={isConfigurationFormValid}
                                       onChange={this.onConfigurationFormChange}
                                       standardPlans={this.state.standardPlans}
                                       brokeredPlans={this.state.brokeredPlans}
                                       standardAuthenticationServices={this.state.standardAuthenticationServices}
                                       brokeredAuthenticationServices={this.state.brokeredAuthenticationServices}
                                       namespaces={this.state.namespaces}/>),
        enableNext: isConfigurationFormValid
      },
      {
        id: 2,
        name: 'Review',
        component: (<Review newInstance={newInstance}/>)
      }
    ];

    return (

      <React.Fragment>
        <Button id="button-create" variant="primary" onClick={this.toggleOpen}>
          Create
        </Button>
        {isOpen && (
          <NotificationConsumer>
            {({add}) => (

              <Wizard
                style={{backgroundColor: '#151515'}}
                isOpen={isOpen}
                title="Create an Instance"
                onClose={this.toggleOpen}
                onSave={() => this.onSave(add)}
                steps={steps}
                lastStepButtonText="Finish"
              />
            )}
          </NotificationConsumer>

        )}
      </React.Fragment>
    );
  }
}

export default CreateAddressSpace;

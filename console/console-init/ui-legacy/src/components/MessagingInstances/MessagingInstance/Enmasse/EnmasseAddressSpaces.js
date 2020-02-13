import axios from 'axios';
import MessagingSpace from '../../MessagingInstance';

const translateMessagingSpaces = addressSpaces => {

  let translation = addressSpaces.items.map(namespace => {

    let consoleUrl = null;
    namespace.status.endpointStatuses.forEach(function (endpoint, index) {
      if (endpoint.name == 'console') {
        consoleUrl = 'https://' + endpoint.externalHost;
      }
    });

    return new MessagingSpace(namespace.metadata.name,
      namespace.metadata.namespace,
      namespace.kind,
      namespace.spec.type,
      namespace.metadata.creationTimestamp,
      namespace.status.isReady,
      namespace.status.phase,
      consoleUrl);
  });
  return translation;
}

const translateAddressSpaces = addressSpace => {
  return {
    name: addressSpace.metadata.name,
    namespace: addressSpace.metadata.namespace,
    typeStandard: (addressSpace.spec.type == 'standard'),
    typeBrokered: (addressSpace.spec.type == 'brokered'),
    plan: addressSpace.spec.plan,
    authenticationService: addressSpace.spec.authenticationService.name};

  return translation;
}

const getAuthenticationServices = authenticationServices => {
  return authenticationServices.spec.authenticationServices;
}

const getPlanNames = plans => plans.spec.plans.map(plan => plan.name);

//needs tenant-view role
export function loadMessagingInstances() {
  return axios.get('apis/enmasse.io/v1beta1/addressspaces')
    .then(response => translateMessagingSpaces(response.data))
    .catch(error => {
      console.log(error);
      return [];
    });
}

export function loadAddressSpace(namespace, addressSpace) {
  return axios.get('apis/enmasse.io/v1beta1/namespaces/'+namespace+'/addressspaces/'+addressSpace)
    .then(response => translateAddressSpaces(response.data))
    .catch(error => {
      console.log(error);
      return [];
    });

}

export function loadMessagingInstance(namespace) {
  return axios.get('apis/enmasse.io/v1beta1/namespaces/'+namespace+'/addressspaces')
    .then(response => translateMessagingSpaces(response.data))
    .catch(error => {
      console.log(error);
      return [];
    });
}

export function loadStandardAuthenticationServices(namespace) {
  return axios.get('apis/enmasse.io/v1beta1/addressspaceschemas/standard')
    .then(response => getAuthenticationServices(response.data))
    .catch(error => {
      console.log(error);
      throw(error);
    });
}

export function loadBrokeredAuthenticationServices(namespace) {
  return axios.get('apis/enmasse.io/v1beta1/addressspaceschemas/brokered')
    .then(response => getAuthenticationServices(response.data))
    .catch(error => {
      console.log(error);
      throw(error);
    });
}

export function loadStandardAddressPlans(namespace) {
  return axios.get('apis/enmasse.io/v1beta1/addressspaceschemas/standard')
    .then(response => getPlanNames(response.data))
    .catch(error => {
      console.log(error);
      throw(error);
    });
}

export function loadBrokeredAddressPlans(namespace) {
  return axios.get('apis/enmasse.io/v1beta1/addressspaceschemas/brokered')
    .then(response => getPlanNames(response.data))
    .catch(error => {
      console.log(error);
      throw(error);
    });
}

export function editMessagingInstance(name, namespace, newPlan) {
  return axios.patch('apis/enmasse.io/v1beta1/namespaces/' + namespace + '/addressspaces/' + name,
    {spec:{plan:newPlan}},
    { headers: { 'Content-Type': 'application/merge-patch+json', 'Accept': 'application/json' } }
    )
    .then(response => console.log('EDIT successful: ', response))
    .catch(error => {
      console.log('EDIT FAILED: ', error);
      throw(error);
    });
}

export function deleteMessagingInstance(name, namespace) {
  return axios.delete('apis/enmasse.io/v1beta1/namespaces/' + namespace + '/addressspaces/' + name)
    .then(response => console.log('DELETE successful: ', response))
    .catch(error => {
      console.log('DELETE FAILED: ', error);
      throw(error);
    });
}

export function createNewAddressSpace(instance) {
  return axios.post('apis/enmasse.io/v1beta1/namespaces/'+ instance.namespace +'/addressspaces', {
    apiVersion: 'enmasse.io/v1beta1',
    kind: 'AddressSpace',
    metadata: {
      name: instance.name,
      namespace: instance.namespace
    },
    spec: {
      plan: instance.plan,
      type: (instance.typeStandard ? 'standard' : 'brokered'),
      authenticationService:
        {
          name: instance.authenticationService
        }
    }
  })
    .then(response => console.log('CREATE successful: ', response))
    ;
}



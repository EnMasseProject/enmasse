import axios from 'axios';
import MessagingSpace from '../../MessagingInstance';

const translateAddressSpaces = addressSpaces => {

  let translation = addressSpaces.items.map(namespace => {

      let consoleUrl = null;
      namespace.status.endpointStatuses.forEach(function(endpoint,index) {
        if (endpoint.name == 'console') {
          consoleUrl = 'https://'+endpoint.externalHost;
        }
      });

      return new MessagingSpace(namespace.metadata.name,
        namespace.metadata.namespace,
        namespace.kind,
        namespace.spec.type,
        namespace.metadata.creationTimestamp,
        namespace.status.isReady,
        consoleUrl);
    });
    return translation;
}

const getPlanNames = plans => plans.spec.plans.map(plan => plan.name).sort();

//needs tenant-view role
export function loadMessagingInstances() {
  return axios.get('apis/enmasse.io/v1beta1/addressspaces')
    .then(response => translateAddressSpaces(response.data))
    .catch(error => {
      console.log(error);
      return [];
    });
}

export function loadMessagingInstance(namespace) {
  return axios.get('apis/enmasse.io/v1beta1/namespaces/'+namespace+'/addressspaces')
    .then(response => translateAddressSpaces(response.data))
    .catch(error => {
      console.log(error);
      return [];
    });
}

export function loadStandardAddressPlans() {
  return axios.get('apis/enmasse.io/v1beta1/namespaces/enmasse-infra/addressspaceschemas/standard')
    .then(response => getPlanNames(response.data))
    .catch(error => {
       console.log(error);
      throw(error);
    });
}

export function loadBrokeredAddressPlans() {
  return axios.get('apis/enmasse.io/v1beta1/namespaces/enmasse-infra/addressspaceschemas/brokered')
    .then(response => getPlanNames(response.data))
    .catch(error => {
      console.log(error);
      throw(error);
    });
}

export function deleteMessagingInstances(name, namespace) {
  console.log('clicked on delete action, on: ' + name + ' ' + namespace);
  return axios.delete('apis/enmasse.io/v1beta1/namespaces/' + namespace + '/addressspaces/' + name)
    .then(response => console.log('DELETE successful: ', response))
    .catch(error => {
      console.log('DELETE FAILED: ', error);
      throw(error);
    });
}

export function createNewAddressSpace(instance) {
  console.log('Creating new address sapce', instance);
  return axios.post('apis/enmasse.io/v1alpha1/namespaces/'+ instance.namespace +'/addressspaces', {
    apiVersion: 'enmasse.io/v1alpha1',
    kind: 'AddressSpace',
    metadata: {
      name: instance.name,
      namespace: instance.namespace
    },
    spec: {
      plan: instance.plan,
      type: (instance.typeStandard ? 'standard' : 'brokered')
    }
  })
    .then(response => console.log('CREATE successful: ', response))
    ;
}



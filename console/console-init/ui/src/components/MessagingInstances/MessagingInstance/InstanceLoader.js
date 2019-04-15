import React from 'react';

import axios from 'axios';

import {loadMessagingInstance, loadMessagingInstances, deleteMessagingInstances} from './Enmasse/EnmasseAddressSpaces';

class InstanceLoader {

  translateNamespaces = namespaces => {
    console.log('returning : ',namespaces.items.map(namespace => namespace.metadata.name));
    return namespaces.items.map(namespace => namespace.metadata.name);
  };

  loadNamespaces() {
    return axios.get('apis/project.openshift.io/v1/').then(() => {
      return axios.get('apis/project.openshift.io/v1/projects')
          .then(response => {
            return this.translateNamespaces(response.data);
          })
          .catch(error => {
              console.log('FAILED to load namespaces', error);
              throw(error);
            }
          );
      }
    ).catch(() => {
      console.log('Can not load namespaces in Kubernetes');
      return new Promise((resolve, reject) =>
        resolve([]));
    });
  }

  loadInstances() {
    return axios.get('apis/project.openshift.io/v1/')
      .then(() => {
        return this.loadNamespaces().then(namespaces => {
            console.log('loadInstances....1');
            let promises = [];
            namespaces.forEach(namespace => {
              promises.push(loadMessagingInstance(namespace));
            });
            return Promise.all(promises)
              .then(values => {
                return [].concat.apply([], values);
              })
              .catch(error => {
                console.log('failed: ', error);
                return error;
              });
          });
        })
      .catch(() => {
        return loadMessagingInstances();
      });
  }
};

export default (new InstanceLoader);

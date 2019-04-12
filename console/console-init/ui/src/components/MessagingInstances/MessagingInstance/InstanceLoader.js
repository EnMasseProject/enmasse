import React from 'react';

import axios from 'axios';

import {loadMessagingInstance, loadMessagingInstances, deleteMessagingInstances} from './Enmasse/EnmasseAddressSpaces';

class InstanceLoader {

  openshiftApiAvailable;

  constructor() {
    this.openshiftApiAvailable = true;
    axios.get('apis/project.openshift.io/v1/')
      .then(response => {
        console.log('Running in Openshift');
      })
      .catch(error => {
          console.log('Running in Kubernetes.', error);
          this.openshiftApiAvailable = false;
        }
      );
  }

  translateNamespaces = namespaces => (
    namespaces.items.map(namespace => namespace.metadata.name)
  );

  loadNamespaces() {
    if (this.openshiftApiAvailable) {
      return axios.get('apis/project.openshift.io/v1/projects')
        .then(response => this.translateNamespaces(response.data))
        .catch(error => {
            console.log('FAILED to load namespaces', error);
            throw(error);
          }
        );
    } else {
      console.log('Can not load namespaces in Kubernetes');
      return new Promise((resolve, reject) =>
        resolve([]));
    }
  }

  loadInstances() {
    if (this.openshiftApiAvailable) {
      return new Promise((resolve, reject) => {
        this.loadNamespaces().then(namespaces => {
          let promises = [];
          namespaces.forEach(namespace => {
            promises.push(loadMessagingInstance(namespace));
          });
          Promise.all(promises)
            .then(values => {
              console.log('success: ', values);
              resolve([].concat.apply([], values));
            })
            .catch(error => {
              console.log('failed: ', error);
              reject(error);
            });
        });
      });
    } else {
      return loadMessagingInstances();
    }
  }
};

export default (new InstanceLoader);

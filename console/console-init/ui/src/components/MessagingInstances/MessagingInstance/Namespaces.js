import React from 'react';
// import {FormSelectOption} from "@patternfly/react-core";

import axios from 'axios';

const translate = namespaces => {

  let translation = namespaces.items.map(namespace => namespace.metadata.name);

  return translation;
}

export function loadNamespaces() {
  return axios.get('apis/project.openshift.io/v1/projects')
    .then(response => translate(response.data))
    .catch(error => {
      console.log('FAILED to load namespaces', error);
      throw(error);
      }
    );
}




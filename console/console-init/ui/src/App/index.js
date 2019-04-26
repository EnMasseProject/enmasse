import React, {Component} from 'react';
import './app.css';
import Console from '../containers/Console/Console'
import "@patternfly/patternfly/patternfly.css";

export default class App extends Component {

  render() {
    console.log('[App.js] Inside render()');

    return (
      <Console />
    );
  }

}

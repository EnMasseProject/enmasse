import React, {Component} from 'react';
import {loadMessagingInstances} from '../../components/MessagingInstances/MessagingInstance/Enmasse/EnmasseAddressSpaces';

import MessagingInstances from '../../components/MessagingInstances/MessagingInstances';
import Layout from '../../components/Layout/Layout';
import User from '../../components/User';

import {NotificationProvider, NotificationConsumer} from '../../context/notification-manager';

class Console extends Component {

  state = {
    messagingInstances: null,
    instanceTypes: {
      totalInstances: 0,
      addressSpaces: 0,
    }
  };

  refresh = () => {
    loadMessagingInstances()
      .then(messagingSpaces => {
        messagingSpaces.sort((a, b) => (a.timeCreated < b.timeCreated) ? 1 : (a.timeCreated === b.timeCreated) ? ((a.name > b.name) ? 1 : -1) : -1)
        this.setState({
          messagingInstances: messagingSpaces,
          instanceTypes: {
            totalInstances: messagingSpaces.length,
            addressSpaces: messagingSpaces.length
          }
        });

      })
      .catch(error => {
        console.log("ERROR Couldn't set the message instances: " + error);
        this.setState({
          messagingInstances: [],
          instanceTypes: {
            totalInstances: 0,
            addressSpaces: 0
          }
        });
      });
  }

  componentDidMount() {
    this.refresh();
  }


  render() {
    return (
      <Layout user={<User/>} instanceTypes={this.state.instanceTypes}>
        <NotificationProvider>
          <MessagingInstances messagingInstances={this.state.messagingInstances}
                              reloadMessagingInstances={this.refresh}/>
        </NotificationProvider>
      </Layout>
    )
  }
}

export default Console;

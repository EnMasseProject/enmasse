import React, {Component} from 'react';
import InstanceLoader from '../../components/MessagingInstances/MessagingInstance/InstanceLoader';

import MessagingInstances from '../../components/MessagingInstances/MessagingInstances';
import Layout from '../../components/Layout/Layout';
import User from '../../components/User';

import {NotificationProvider, NotificationConsumer} from '../../context/notification-manager';

class Console extends Component {


  constructor(props) {
    super(props);

    this.refreshRate = parseInt(window.env.ITEM_REFRESH_RATE, 10);
    this.refreshRate = (this.refreshRate) ? this.refreshRate : 30000;
    console.log('Refresh rate set to : ', this.refreshRate);
  }

  state = {
    messagingInstances: [],
    instanceTypes: {
      totalInstances: 0,
      addressSpaces: 0,
    }
  };

  refresh = () => {
    InstanceLoader.loadInstances()
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
    this.interval = setInterval(() => {this.refresh()}, this.refreshRate);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
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

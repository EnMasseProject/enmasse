import React, {Component} from 'react';

import {Alert, AlertActionCloseButton} from '@patternfly/react-core';


const {Consumer, Provider} = React.createContext();


const NotificationContainer = props => {
  let style = {position: "fixed", right: '15px', top: '100px', zIndex: 'var(--pf-global--ZIndex--lg)'};
  return (<div style={style} {...props} />);
};

const Notification = ({notification, onDismiss}) => (
  <Alert
    variant={notification.variant}
    title={notification.content}
    action={<AlertActionCloseButton onClose={onDismiss}/>}
  />);

export class NotificationProvider extends Component {

  state = {notifications: []};
  count = 0;

  add = (content, variant) => {
    this.setState(state => {
      const item = {
        id: this.count++,
        variant: variant,
        content: content
      };
      return {notifications: [...state.notifications, item]};
    });
  };

  remove = id => {
    this.setState(state => {
      const notifications = state.notifications.filter(t => t.id !== id);
      return {notifications: notifications};
    });
  };

  onDismiss = id => () => this.remove(id);

  render() {
    const context = {
      add: this.add,
      remove: this.remove
    };

    return (
      <Provider value={context}>
        <NotificationContainer>
          {this.state.notifications.map((notification) => (
            <Notification
              key={notification.id}
              notification={notification}
              onDismiss={this.onDismiss(notification.id)}
            />
          ))}
        </NotificationContainer>

        {this.props.children}
      </Provider>
    );
  }
}

export const NotificationConsumer = ({children}) => (
  <Consumer>{context => children(context)}</Consumer>
);

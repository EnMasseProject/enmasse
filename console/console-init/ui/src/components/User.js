import React from 'react';


export default class User extends React.Component {

  constructor(props) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    fetch("/apis/user.openshift.io/v1/users/~")
      .then(res => res.json())
      .then(user => this.setState({
        name: user.metadata ? user.metadata.name : "unknown"
      }))
      .catch(reason => {
        console.info("Failed to complete user request", reason);
        this.setState({name: "unknown"})
      });
  }

  render() {
    return <React.Fragment>{this.state.name}</React.Fragment>;
  }
}

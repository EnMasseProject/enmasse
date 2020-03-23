/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useHistory } from "react-router-dom";
import ApolloClient from "apollo-boost";
import "@patternfly/react-core/dist/styles/base.css";
import { AppLayout as Layout } from "use-patternfly";
import { Brand, Avatar } from "@patternfly/react-core";
import { ApolloProvider } from "@apollo/react-hooks";
import {
  NavToolBar,
  ServerMessageAlert,
  NetworkStatusAlert,
  RootModal
} from "components";
import { AppRoutes } from "Routes";
import brandImg from "./assets/images/logo.svg";
import avatarImg from "./img_avatar.svg";
import "./App.css";
import { useStoreContext } from "./context-state-reducer";
import { onServerError } from "./graphql-module";

let history: any, dispactAction: any, states: any;

const graphqlEndpoint = process.env.REACT_APP_GRAPHQL_ENDPOINT
  ? process.env.REACT_APP_GRAPHQL_ENDPOINT
  : "http://localhost:4000";

const client = new ApolloClient({
  uri: graphqlEndpoint,
  onError(error: any) {
    onServerError(error, dispactAction, states);
  }
});

const avatar = (
  <React.Fragment>
    <Avatar src={avatarImg} alt="avatar" />
  </React.Fragment>
);

const logo = <Brand src={brandImg} alt="Console Logo" />;

const AppLayout: React.FC = () => {
  history = useHistory();
  const { dispatch, state } = useStoreContext();
  states = state;
  dispactAction = dispatch;
  const logoProps = React.useMemo(
    () => ({
      onClick: () => history.push("/")
    }),
    [history]
  );

  return (
    <ApolloProvider client={client}>
      <RootModal />
      <Layout
        logoProps={logoProps}
        logo={logo}
        avatar={avatar}
        toolbar={<NavToolBar />}
      >
        <NetworkStatusAlert />
        <ServerMessageAlert />
        <AppRoutes />
      </Layout>
    </ApolloProvider>
  );
};

export default AppLayout;

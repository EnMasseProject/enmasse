/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import "@patternfly/react-core/dist/styles/base.css";
import { AppLayout as Layout } from "use-patternfly";
import { useHistory } from "react-router-dom";
import { Brand, Avatar } from "@patternfly/react-core";
import { CrossNavHeader } from "@rh-uxd/integration-react";
import ApolloClient from "apollo-boost";
import { ApolloProvider } from "@apollo/react-hooks";
import NavToolBar from "components/NavToolBar/NavToolBar";
import { AppRoutes } from "Routes";
import brandImg from "./assets/images/logo.svg";
import avatarImg from "./img_avatar.svg";
import "./App.css";
import { ServerMessageAlert, NetworkStatusAlert } from "./components/common";
import { useErrorContext } from "./context-state-reducer";
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
  const { dispatch, state } = useErrorContext();
  states = state;
  dispactAction = dispatch;
  const logoProps = React.useMemo(
    () => ({
      onClick: () => history.push("/")
    }),
    [history]
  );

  const getCrossNavApps = [
    { id: "0", name: "3 Scale", rootUrl: "http://localhost:8000" },
    { id: "1", name: "AMQ Online", rootUrl: "http://localhost:8000" },
    { id: "2", name: "API Designer", rootUrl: "http://localhost:8000" },
    { id: "3", name: "Red Hat Fuse Online", rootUrl: "http://localhost:8000" }
  ];

  return (
    <ApolloProvider client={client}>
      <CrossNavHeader
        apps={getCrossNavApps}
        currentApp={{
          id: "solution-explorer",
          name: "Solution Explorer",
          rootUrl: "localhost:3000"
        }}
        logoProps={logoProps}
        logo={logo}
        avatar={avatar}
        toolbar={<NavToolBar />}
      >
        <NetworkStatusAlert />
        <ServerMessageAlert />
        <AppRoutes />
      </CrossNavHeader>
    </ApolloProvider>
  );
};

export default AppLayout;

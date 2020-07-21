/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useHistory } from "react-router-dom";
import ApolloClient from "apollo-boost";
import "@patternfly/react-core/dist/styles/base.css";
import { AppLayoutContext } from "use-patternfly";
import { Brand, Avatar, Page } from "@patternfly/react-core";

import { ApolloProvider } from "@apollo/react-hooks";
import {
  NavToolBar,
  ServerMessageAlert,
  NetworkStatusAlert,
  RootModal
} from "components";
import { AppRoutes } from "Routes";
import { AppNavHeader } from "./AppNavHeader";
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
    <Avatar src={avatarImg} className="app-avatar" alt="avatar" />
  </React.Fragment>
);

const logoText = process.env.REACT_APP_NAME
  ? process.env.REACT_APP_NAME + " Logo"
  : "Console Logo";
const brandImgLogo = <Brand src={brandImg} alt={logoText} />;

const AppLayout: React.FC = () => {
  history = useHistory();
  const [breadcrumb, setBreadcrumb] = React.useState<
    React.ReactNode | undefined
  >();
  const previousBreadcrumb = React.useRef<React.ReactNode | null>();

  const handleSetBreadcrumb = React.useCallback(
    (newBreadcrumb: React.ReactNode) => {
      if (previousBreadcrumb.current !== newBreadcrumb) {
        previousBreadcrumb.current = newBreadcrumb;
        setBreadcrumb(previousBreadcrumb.current);
      }
    },
    [setBreadcrumb, previousBreadcrumb]
  );
  const { dispatch, state } = useStoreContext();
  states = state;
  dispactAction = dispatch;
  const logoProps = React.useMemo(
    () => ({
      onClick: () => history.push("/")
    }),
    []
  );

  const header = (
    <AppNavHeader
      logo={brandImgLogo}
      logoProps={logoProps}
      avatar={avatar}
      toolbar={<NavToolBar />}
    />
  );
  return (
    <ApolloProvider client={client}>
      <RootModal />
      <AppLayoutContext.Provider value={{ setBreadcrumb: handleSetBreadcrumb }}>
        <Page
          header={header}
          breadcrumb={breadcrumb}
          mainContainerId="main-container"
        >
          <NetworkStatusAlert />
          <ServerMessageAlert />
          <AppRoutes />
        </Page>
      </AppLayoutContext.Provider>
    </ApolloProvider>
  );
};

export default AppLayout;

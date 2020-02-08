/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import "./App.css";
import "@patternfly/react-core/dist/styles/base.css";
import { AppLayout } from "use-patternfly";
import { useHistory } from "react-router-dom";
import { Brand, Avatar } from "@patternfly/react-core";
import brandImg from "./brand_logo.svg";
import NavToolBar from "components/NavToolBar/NavToolBar";
import { AppRoutes } from "AppRoutes";
import ApolloClient from "apollo-boost";
import { ApolloProvider } from "@apollo/react-hooks";
import avatarImg from "./img_avatar.svg";

const graphqlEndpoint = process.env.REACT_APP_GRAPHQL_ENDPOINT
  ? process.env.REACT_APP_GRAPHQL_ENDPOINT
  : "http://localhost:4000";
const client = new ApolloClient({
  uri: graphqlEndpoint
});

const avatar = (
  <React.Fragment>
    <Avatar src={avatarImg} alt="avatar" />
  </React.Fragment>
);

const logo = <Brand src={brandImg} alt="Console Logo" />;

const App: React.FC = () => {
  const history = useHistory();
  const logoProps = React.useMemo(
    () => ({
      onClick: () => history.push("/")
    }),
    [history]
  );
  return (
    <ApolloProvider client={client}>
      <AppLayout
        logoProps={logoProps}
        logo={logo}
        avatar={avatar}
        toolbar={<NavToolBar />}
      >
        <AppRoutes />
      </AppLayout>
    </ApolloProvider>
  );
};

export default App;

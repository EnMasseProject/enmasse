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
import { CrossNavHeader, CrossNavApp } from "@rh-uxd/integration-react";
import {
  getAvailableApps,
  getSolutionExplorerServer
} from "@rh-uxd/integration-core";
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
import rhiImage from "@rh-uxd/integration-core/styles/assets/Logo-Red_Hat-Managed_Integration-A-Reverse-RGB.png";

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

const rhiImgLogo = <Brand src={rhiImage} alt="Integration Logo" />;
const brandImgLogo = <Brand src={brandImg} alt="AMQ Logo" />;

const AppLayout: React.FC = () => {
  history = useHistory();
  const { dispatch, state } = useStoreContext();
  states = state;
  dispactAction = dispatch;
  const logoProps = React.useMemo(
    () => ({
      onClick: () => history.push("/")
    }),
    []
  );

  const [availableApps, setHasAvailableApps] = React.useState<
    CrossNavApp[] | null
  >(null);
  const [showLogo, setShowLog] = React.useState(false);

  if (!availableApps) {
    getAvailableApps(
      process.env.REACT_APP_RHMI_SERVER_URL
        ? process.env.REACT_APP_RHMI_SERVER_URL
        : getSolutionExplorerServer(),
      undefined,
      process.env.REACT_APP_RHMI_SERVER_URL ? "localhost:3006" : undefined,
      ["3scale", "amqonline"],
      !!process.env.REACT_APP_RHMI_SERVER_URL
    ).then(apps => {
      setHasAvailableApps(apps);
      setShowLog(true);
    });
  }

  return (
    <ApolloProvider client={client}>
      <RootModal />
      <CrossNavHeader
        apps={availableApps}
        currentApp={{
          id: "amqonline",
          name: "AMQ Online",
          rootUrl: window.location.href
        }}
        // logoProps={availableApps && availableApps.length > 0 && {}}
        logo={
          showLogo
            ? availableApps && availableApps.length > 0
              ? rhiImgLogo
              : brandImgLogo
            : null
        }
      />
      <NetworkStatusAlert />
      <ServerMessageAlert />
      <AppRoutes />
    </ApolloProvider>
  );
};

export default AppLayout;

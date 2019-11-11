import React from "react";
import "./App.css";
import "@patternfly/react-core/dist/styles/base.css";
import { AppLayout } from "use-patternfly";
import { useHistory, BrowserRouter as Router } from "react-router-dom";
import { Avatar, Brand, Text, TextVariants } from "@patternfly/react-core";
import avatarImg from "./logo.svg";
import brandImg from "./brand_logo.svg";
import NavToolBar from "./Components/NavToolBar/NavToolBar";
import { LastLocationProvider } from "react-router-last-location";
import { AppRoutes } from "./AppRoutes";
import ApolloClient from "apollo-boost";
import { ApolloProvider } from "@apollo/react-hooks";

const client = new ApolloClient({
  uri: "http://localhost:4000"
  // uri: 'https://swapi.graph.cool/',
});

const avatar = (
  <React.Fragment>
    <Text component={TextVariants.p}>Ramakrishna Pattnaik</Text>
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
    <Router>
      <LastLocationProvider>
        <ApolloProvider client={client}>
          <AppLayout
            logoProps={logoProps}
            logo={logo}
            avatar={avatar}
            toolbar={<NavToolBar />}>
            <AppRoutes />
          </AppLayout>
        </ApolloProvider>
      </LastLocationProvider>
    </Router>
  );
};

export default App;

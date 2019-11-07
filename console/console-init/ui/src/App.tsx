import React from "react";
import "./App.css";
import { AppLayout, SwitchWith404, LazyRoute } from "use-patternfly";
import { useHistory } from "react-router-dom";
import { Avatar, Brand, Text, TextVariants } from "@patternfly/react-core";
import brandImg from "./brand_logo.svg";
import NavToolBar from "./Components/NavToolBar/NavToolBar";

const getIndexPage = () => import("./Pages/IndexPage");
const avatar = (
  <React.Fragment>
    <Text component={TextVariants.p}>Ramakrishna Pattnaik</Text>
  </React.Fragment>
);
const logo = <Brand src={brandImg} alt="Console Logo" />;

const App: React.FC = () => {
  const history = useHistory();
  return (
    <AppLayout
      logoProps={{
        onClick: () => history.push("/")
      }}
      logo={logo}
      avatar={avatar}
      toolbar={<NavToolBar />}
    >
      <SwitchWith404>
        <LazyRoute path="/" exact={true} getComponent={getIndexPage} />
      </SwitchWith404>
    </AppLayout>
  );
};

export default App;

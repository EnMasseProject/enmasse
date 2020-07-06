/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useHistory } from "react-router-dom";
import { StyleSheet, css } from "aphrodite";
import { Alert, PageSection, AlertActionLink } from "@patternfly/react-core";

const styles = StyleSheet.create({
  alert: {
    backgroundColor: "var(--pf-c-alert--m-inline--BackgroundColor)"
  }
});
interface INotFoundProps {
  updateState: (error: boolean) => void;
}

const NotFound: React.FunctionComponent<INotFoundProps> = ({ updateState }) => {
  const history = useHistory();

  const handleAlertActionLink = () => {
    history.push("/");
  };

  return (
    <PageSection>
      <Alert
        variant="danger"
        title="Unexpected Error"
        className={css(styles.alert)}
        actionLinks={
          <AlertActionLink onClick={handleAlertActionLink}>
            Take me home
          </AlertActionLink>
        }
      >
        Something went wrong. Please try again!
      </Alert>
    </PageSection>
  );
};

export { NotFound };

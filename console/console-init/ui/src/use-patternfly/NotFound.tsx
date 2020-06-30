/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useHistory } from "react-router-dom";
import { Alert, PageSection, AlertActionLink } from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { useA11yRouteChange } from "./useA11yRoute";
import { useDocumentTitle } from "./useDocumentTitle";

const styles = StyleSheet.create({
  alert: {
    backgroundColor: "var(--pf-c-alert--m-inline--BackgroundColor)"
  }
});

export const NotFound: React.FunctionComponent = () => {
  useA11yRouteChange();
  useDocumentTitle("Page not found");
  const history = useHistory();

  const handleRedirectLink = () => {
    history.push("/");
  };

  return (
    <PageSection>
      <Alert
        variant="danger"
        title="404! This view hasn't been created yet."
        className={css(styles.alert)}
        actionLinks={
          <AlertActionLink onClick={handleRedirectLink}>
            Take me home
          </AlertActionLink>
        }
      />
    </PageSection>
  );
};

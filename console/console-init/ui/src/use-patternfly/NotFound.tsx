/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { NavLink } from "react-router-dom";
import { Alert, PageSection } from "@patternfly/react-core";
import { useA11yRouteChange } from "./useA11yRoute";
import { useDocumentTitle } from "./useDocumentTitle";

export const NotFound: React.FunctionComponent = () => {
  useA11yRouteChange();
  useDocumentTitle("Page not found");
  return (
    <PageSection>
      <Alert variant="danger" title="404! This view hasn't been created yet." />
      <br />
      <NavLink to="/" className="pf-c-nav__link">
        Take me home
      </NavLink>
    </PageSection>
  );
};

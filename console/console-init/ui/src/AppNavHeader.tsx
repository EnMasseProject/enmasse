/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import styles from "@patternfly/react-styles/css/components/Page/page";
import { css } from "@patternfly/react-styles";
import { PageHeaderProps, PageContextConsumer } from "@patternfly/react-core";

export class AppNavHeader extends React.Component<PageHeaderProps> {
  render() {
    const {
      logo = null as React.ReactNode,
      logoProps = (null as unknown) as object,
      logoComponent = "a",
      toolbar = null as React.ReactNode,
      avatar = null as React.ReactNode,
      topNav = null as React.ReactNode,
      ...props
    } = this.props;

    const LogoComponent = logoComponent as any;

    return (
      <PageContextConsumer>
        {PageHeaderProps => {
          return (
            <header
              role="banner"
              className={`${css(styles.pageHeader)} `}
              {...props}
            >
              {logo && (
                <div className={css(styles.pageHeaderBrand)}>
                  <LogoComponent
                    className={css(styles.pageHeaderBrandLink)}
                    {...logoProps}
                  >
                    {logo}
                  </LogoComponent>
                </div>
              )}
              {topNav && (
                <div className={css(styles.pageHeaderNav)}>{topNav}</div>
              )}
              {(toolbar || avatar) && (
                <div className={css(styles.pageHeaderTools)}>
                  {toolbar}
                  {avatar}
                </div>
              )}
            </header>
          );
        }}
      </PageContextConsumer>
    );
  }
}

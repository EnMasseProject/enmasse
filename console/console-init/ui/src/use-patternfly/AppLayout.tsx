/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Nav,
  NavList,
  Page,
  PageHeader,
  PageSidebar,
  SkipToContent,
  PageHeaderProps
} from "@patternfly/react-core";
import { AppNavExpandable, IAppNavExpandableProps } from "./AppNavExpandable";
import { AppNavGroup, IAppNavGroupProps } from "./AppNavGroup";
import { AppNavItem, IAppNavItemProps } from "./AppNavItem";

export interface IAppLayoutContext {
  setBreadcrumb: (breadcrumb: React.ReactNode) => void;
}
export const AppLayoutContext = React.createContext<IAppLayoutContext>({
  setBreadcrumb: () => void 0
});

export interface IAppLayoutProps
  extends Pick<PageHeaderProps, "logo">,
    Pick<PageHeaderProps, "logoProps">,
    Pick<PageHeaderProps, "headerTools"> {
  navVariant?: "vertical" | "horizontal";
  navItems?: Array<
    IAppNavItemProps | IAppNavExpandableProps | IAppNavGroupProps | undefined
  >;
  navGroupsStyle?: "grouped" | "expandable";
  startWithOpenNav?: boolean;
  theme?: "dark" | "light";
  mainContainerId?: string;
}

export enum NavVariants {
  default = "default",
  horizontal = "horizontal",
  tertiary = "tertiary"
}

export const AppLayout: React.FunctionComponent<IAppLayoutProps> = ({
  logo,
  logoProps,
  navVariant = "horizontal",
  navItems = [],
  navGroupsStyle = "grouped",
  headerTools,
  startWithOpenNav = true,
  theme = "dark",
  mainContainerId = "main-container",
  children
}) => {
  const [isNavOpen, setIsNavOpen] = React.useState(startWithOpenNav);
  const [isMobileView, setIsMobileView] = React.useState(true);
  const [isNavOpenMobile, setIsNavOpenMobile] = React.useState(false);
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

  const onNavToggleMobile = React.useCallback(() => {
    setIsNavOpenMobile(!isNavOpenMobile);
  }, [setIsNavOpenMobile, isNavOpenMobile]);

  const onNavToggle = React.useCallback(() => {
    setIsNavOpen(!isNavOpen);
  }, [setIsNavOpen, isNavOpen]);

  const onPageResize = (props: { mobileView: boolean; windowSize: number }) => {
    setIsMobileView(props.mobileView);
  };

  React.useEffect(() => {
    setIsNavOpen(startWithOpenNav);
  }, [startWithOpenNav, setIsNavOpen]);

  const isVertical = navVariant === "vertical";
  const variant = isVertical ? NavVariants.default : NavVariants.horizontal;

  const Navigation = React.useMemo(
    () =>
      navItems.length > 0 ? (
        <Nav id="nav-primary-simple" theme={theme} variant={variant}>
          <NavList id="nav-list-simple">
            {navItems.map((navItem, idx) => {
              if (navItem && navItem.hasOwnProperty("items") && isVertical) {
                return navGroupsStyle === "expandable" ? (
                  <AppNavExpandable
                    {...(navItem as IAppNavExpandableProps)}
                    key={idx}
                  />
                ) : (
                  <AppNavGroup {...(navItem as IAppNavGroupProps)} key={idx} />
                );
              } else {
                return (
                  <AppNavItem {...(navItem as IAppNavItemProps)} key={idx} />
                );
              }
            })}
          </NavList>
        </Nav>
      ) : null,
    [isVertical, navGroupsStyle, navItems, theme, variant]
  );

  const Header = React.useMemo(
    () => (
      <PageHeader
        logo={logo}
        logoProps={logoProps}
        headerTools={headerTools}
        showNavToggle={isVertical}
        isNavOpen={isVertical ? isNavOpen : undefined}
        onNavToggle={isMobileView ? onNavToggleMobile : onNavToggle}
        topNav={isVertical ? undefined : Navigation}
      />
    ),
    [
      logo,
      logoProps,
      isVertical,
      isNavOpen,
      isMobileView,
      onNavToggle,
      onNavToggleMobile,
      Navigation,
      headerTools
    ]
  );

  const Sidebar = React.useMemo(
    () =>
      navVariant === "vertical" ? (
        <PageSidebar
          nav={Navigation}
          isNavOpen={isMobileView ? isNavOpenMobile : isNavOpen}
          theme={theme}
          data-testid="app-sidebar"
        />
      ) : (
        undefined
      ),
    [navVariant, Navigation, isMobileView, isNavOpenMobile, isNavOpen, theme]
  );

  return (
    <AppLayoutContext.Provider value={{ setBreadcrumb: handleSetBreadcrumb }}>
      <Page
        mainContainerId={mainContainerId}
        header={Header}
        sidebar={Sidebar}
        breadcrumb={breadcrumb}
        onPageResize={onPageResize}
        skipToContent={
          <SkipToContent href="#primary-app-container">
            Skip to Content
          </SkipToContent>
        }
      >
        {children}
      </Page>
    </AppLayoutContext.Provider>
  );
};

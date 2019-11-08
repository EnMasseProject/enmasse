import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Nav,
  NavList,
  NavVariants,
  NavItem
} from "@patternfly/react-core";
import { NavLink } from "react-router-dom";

export interface AddressSpaceNavigationProps {
  activeItem: string;
  onSelect: (item: any) => void;
}
export const AddressSpaceNavigation: React.FunctionComponent<
  AddressSpaceNavigationProps
> = ({ activeItem, onSelect }) => {
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Nav onSelect={onSelect}>
        <NavList variant={NavVariants.tertiary}>
          <NavItem
            key="addresses"
            itemId="addresses"
            isActive={activeItem === "addresses"}
          >
            <NavLink to={"address_space/addresses"} exact={true}>
              Addresses
            </NavLink>
          </NavItem>
          <NavItem
            key="connections"
            itemId="connections"
            isActive={activeItem === "connections"}
          >
            <NavLink to={"address_space/connections"} exact={true}>
              Connections
            </NavLink>
          </NavItem>
        </NavList>
      </Nav>
    </PageSection>
  );
};

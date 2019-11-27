import * as React from "react";
import { Nav, NavList, NavVariants, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";

export interface AddressSpaceNavigationProps {
  activeItem: string;
}
export const AddressSpaceNavigation: React.FunctionComponent<AddressSpaceNavigationProps> = ({
  activeItem
}) => {
  const [active, setActive] = React.useState(activeItem);
  const onSelect1 = (result: any) => {
    setActive(result.itemId);
  };
  return (
    <Nav onSelect={onSelect1}>
      <NavList variant={NavVariants.tertiary}>
        <NavItem
          key="addresses"
          itemId="addresses"
          isActive={active === "addresses"}
        >
          <NavLink to={`addresses`} style={{ color: "black" }}>
            Addresses
          </NavLink>
        </NavItem>
        <NavItem
          key="connections"
          itemId="connections"
          isActive={active === "connections"}
        >
          <NavLink to={`connections`} style={{ color: "black" }}>
            Connections
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};

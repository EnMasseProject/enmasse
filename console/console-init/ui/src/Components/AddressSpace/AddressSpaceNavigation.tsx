import * as React from "react";
import { PageSection, PageSectionVariants, Nav, NavList, NavVariants, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";

export interface AddressSpaceNavigationProps {
    activeItem:string;
    onSelect:(item:any)=>void;
}
export const AddressSpaceNavigation: React.FunctionComponent<AddressSpaceNavigationProps> = ({activeItem,onSelect}) => {
  return (
      <Nav onSelect={onSelect}>
        <NavList variant={NavVariants.tertiary}>
          <NavItem
            key="addresses"
            itemId="addresses"
            isActive={activeItem === "addresses"}>
            <NavLink to={"/address-space/456/addresses"} >
              Addresses
            </NavLink>
          </NavItem>
          <NavItem
            key="connections"
            itemId="connections"
            isActive={activeItem === "connections"}>
            <NavLink to={"/address-space/456/connections"}>
              Connections
            </NavLink>
          </NavItem>
        </NavList>
      </Nav>
  );
};

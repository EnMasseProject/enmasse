import * as React from "react";
import {
  Nav,
  NavList,
  NavVariants,
  NavItem
} from "@patternfly/react-core";
import { NavLink, useParams } from "react-router-dom";

export interface AddressSpaceNavigationProps {
  activeItem: string;
  name?:string;
  namespace?:string;
}
export const AddressSpaceNavigation: React.FunctionComponent<
  AddressSpaceNavigationProps
> = ({ activeItem , name, namespace }) => {
  const [active,setActive] = React.useState(activeItem);
  const onSelect1 = (result:any)=>{
    setActive(result.itemId)
  }
  return (
      <Nav onSelect={onSelect1}>
        <NavList variant={NavVariants.tertiary}>
          <NavItem
            key="addresses"
            itemId="addresses"
            isActive={active==="addresses"}>
            <NavLink to={`/address_space/name=${name}&namespace=${namespace}/addresses`} >
              Addresses
            </NavLink>
          </NavItem>
          <NavItem
            key="connections"
            itemId="connections"
            isActive={active==="connections"}>
            <NavLink to={`/address_space/name=${name}&namespace=${namespace}/connections`}>
              Connections
            </NavLink>
          </NavItem>
        </NavList>
      </Nav>
  );
};

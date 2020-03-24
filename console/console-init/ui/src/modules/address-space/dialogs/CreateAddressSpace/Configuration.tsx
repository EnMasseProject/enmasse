/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_NAMESPACES,
  RETURN_AUTHENTICATION_SERVICES
} from "graphql-module/queries";
import { Loading } from "use-patternfly";
import { dnsSubDomainRfc1123NameRegexp } from "schema/Configs";
import {
  AddressSpaceConfiguration,
  IAuthenticationServiceOptions
} from "modules/address-space/components";

export interface IConfiguration {
  name: string;
  setName: (name: string) => void;
  type: string;
  setType: (type: string) => void;
  plan: string;
  setPlan: (plan: string) => void;
  namespace: string;
  setNamespace: (namespace: string) => void;
  authenticationService: string;
  setAuthenticationService: (authenticationService: string) => void;
  isNameValid: boolean;
  setIsNameValid: (isNameValid: boolean) => void;
}
export interface IAddressSpacePlans {
  addressSpacePlans: Array<{
    metadata: {
      name: string;
      uid: string;
      creationTimestamp: Date;
    };
    spec: {
      addressSpaceType: string;
    };
  }>;
}

export interface IAddressSpaceAuthServiceResponse {
  addressSpaceSchema_v2: IAddressSpaceAuthService[];
}

export interface IAddressSpaceAuthService {
  metadata: {
    name: string;
  };
  spec: {
    authenticationServices: string[];
  };
}

export interface INamespaces {
  namespaces: Array<{
    metadata: {
      name: string;
    };
    status: {
      phase: string;
    };
  }>;
}

export const Configuration: React.FunctionComponent<IConfiguration> = ({
  name,
  setName,
  namespace,
  setNamespace,
  type,
  setType,
  plan,
  setPlan,
  authenticationService,
  setAuthenticationService,
  isNameValid,
  setIsNameValid
}) => {
  let namespaceOptions: any[];
  let planOptions: any[] = [];
  let authenticationServiceOptions: IAuthenticationServiceOptions[] = [];

  const [isStandardChecked, setIsStandardChecked] = useState(false);
  const [isBrokeredChecked, setIsBrokeredChecked] = useState(false);

  useEffect(() => {
    if (type === "standard") setIsStandardChecked(true);
    else if (type === "brokered") setIsBrokeredChecked(true);
  }, [type]);

  const onNameSpaceSelect = (value: string) => {
    setNamespace(value);
  };

  const onPlanSelect = (value: string) => {
    setPlan(value);
  };

  const onAuthenticationServiceSelect = (value: string) => {
    setAuthenticationService(value);
  };

  const { loading, data } = useQuery<INamespaces>(RETURN_NAMESPACES);

  const { data: authenticationServices } = useQuery<
    IAddressSpaceAuthServiceResponse
  >(RETURN_AUTHENTICATION_SERVICES) || { data: { addressSpaceSchema_v2: [] } };

  const { addressSpacePlans } = useQuery<IAddressSpacePlans>(
    RETURN_ADDRESS_SPACE_PLANS
  ).data || {
    addressSpacePlans: []
  };

  if (loading) return <Loading />;

  const { namespaces } = data || {
    namespaces: []
  };

  namespaceOptions = namespaces.map(namespace => {
    return {
      value: namespace.metadata.name,
      label: namespace.metadata.name
    };
  });
  if (type) {
    planOptions =
      addressSpacePlans
        .map(plan => {
          if (plan.spec.addressSpaceType === type) {
            return {
              value: plan.metadata.name,
              label: plan.metadata.name
            };
          }
        })
        .filter(plan => plan !== undefined) || [];
  }

  if (authenticationServices) {
    authenticationServices.addressSpaceSchema_v2.forEach(authService => {
      if (authService.metadata.name === type) {
        authenticationServiceOptions = authService.spec.authenticationServices.map(
          service => {
            return {
              value: service,
              label: service
            };
          }
        );
      }
    });
  }

  const handleBrokeredChange = () => {
    setIsBrokeredChecked(true);
    setIsStandardChecked(false);
    setPlan(" ");
    setAuthenticationService(" ");
    setType("brokered");
  };

  const handleStandardChange = () => {
    setIsStandardChecked(true);
    setIsBrokeredChecked(false);
    setPlan(" ");
    setAuthenticationService(" ");
    setType("standard");
  };

  const handleNameChange = (name: string) => {
    setName(name);
    !dnsSubDomainRfc1123NameRegexp.test(name)
      ? setIsNameValid(false)
      : setIsNameValid(true);
  };

  return (
    <>
      <AddressSpaceConfiguration
        onNameSpaceSelect={onNameSpaceSelect}
        handleNameChange={handleNameChange}
        handleBrokeredChange={handleBrokeredChange}
        onPlanSelect={onPlanSelect}
        handleStandardChange={handleStandardChange}
        onAuthenticationServiceSelect={onAuthenticationServiceSelect}
        namespace={namespace}
        namespaceOptions={namespaceOptions}
        name={name}
        isNameValid={isNameValid}
        isStandardChecked={isStandardChecked}
        isBrokeredChecked={isBrokeredChecked}
        type={type}
        plan={plan}
        planOptions={planOptions}
        authenticationService={authenticationService}
        authenticationServiceOptions={authenticationServiceOptions}
      />
    </>
  );
};

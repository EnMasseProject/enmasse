/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IMessagingProject } from "./CreateMessagingProject";
import {
  AddressSpaceConfiguration,
  IAuthenticationServiceOptions
} from "modules/address-space/components";
import { IOptionForKeyValueLabel } from "modules/address-space/utils";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_NAMESPACES,
  RETURN_AUTHENTICATION_SERVICES
} from "graphql-module/queries";
import { Loading } from "use-patternfly";

export interface IMessagingProjectConfigurationProps {
  projectDetail: IMessagingProject;
  setProjectDetail: (project: IMessagingProject) => void;
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
      displayName: string;
      longDescription: string;
      shortDescription: string;
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

const MessagingProjectConfiguration: React.FunctionComponent<IMessagingProjectConfigurationProps> = ({
  projectDetail,
  setProjectDetail
}) => {
  const {
    name,
    namespace,
    type,
    plan,
    authService,
    isNameValid,
    customizeEndpoint
  } = projectDetail && projectDetail;
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
  const onNameSpaceSelect = (value: string) => {
    setProjectDetail({ ...projectDetail, namespace: value });
  };
  const handleNameChange = (value: string) => {
    setProjectDetail({ ...projectDetail, name: value });
  };
  const handleBrokeredChange = () => {
    setProjectDetail({ ...projectDetail, type: "brokered" });
  };
  const handleStandardChange = () => {
    setProjectDetail({ ...projectDetail, type: "standard" });
  };
  const onPlanSelect = (value: string) => {
    setProjectDetail({ ...projectDetail, plan: value });
  };
  const onAuthenticationServiceSelect = (value: string) => {
    setProjectDetail({ ...projectDetail, authService: value });
  };

  const getNameSpaceOptions = () => {
    let nameSpaceOptions: IOptionForKeyValueLabel[];
    nameSpaceOptions = namespaces.map(namespace => ({
      value: namespace.metadata.name,
      label: namespace.metadata.name,
      key: namespace.metadata.name
    }));
    return nameSpaceOptions;
  };
  const handleCustomEndpointChange = (customizeSwitchCheckedValue: boolean) => {
    setProjectDetail({
      ...projectDetail,
      customizeEndpoint: customizeSwitchCheckedValue
    });
  };

  const getPlanOptions = () => {
    let planOptions: any[] = [];
    if (type) {
      planOptions =
        addressSpacePlans
          .map(plan => {
            if (plan.spec.addressSpaceType === type) {
              return {
                value: plan.metadata.name,
                label: plan.spec.displayName || plan.metadata.name,
                key: plan.spec.displayName || plan.metadata.name,
                description:
                  plan.spec.shortDescription || plan.spec.longDescription
              };
            }
          })
          .filter(plan => plan !== undefined) || [];
    }
    return planOptions;
  };

  const getAuthenticationServiceOptions = () => {
    let authenticationServiceOptions: IAuthenticationServiceOptions[] = [];
    if (authenticationServices) {
      authenticationServices.addressSpaceSchema_v2.forEach(authService => {
        if (authService.metadata.name === type) {
          authenticationServiceOptions = authService.spec.authenticationServices.map(
            service => ({
              value: service,
              label: service,
              key: service
            })
          );
        }
      });
    }
    return authenticationServiceOptions;
  };

  return (
    <AddressSpaceConfiguration
      onNameSpaceSelect={onNameSpaceSelect}
      handleNameChange={handleNameChange}
      handleBrokeredChange={handleBrokeredChange}
      onPlanSelect={onPlanSelect}
      handleStandardChange={handleStandardChange}
      onAuthenticationServiceSelect={onAuthenticationServiceSelect}
      namespace={namespace || ""}
      namespaceOptions={getNameSpaceOptions()}
      name={name || ""}
      isNameValid={isNameValid || false}
      isStandardChecked={type === "standard"}
      isBrokeredChecked={type === "brokered"}
      type={type || ""}
      plan={plan || ""}
      planOptions={getPlanOptions()}
      authenticationService={authService || ""}
      authenticationServiceOptions={getAuthenticationServiceOptions()}
      customizeEndpoint={customizeEndpoint}
      handleCustomEndpointChange={handleCustomEndpointChange}
    />
  );
};

export { MessagingProjectConfiguration };

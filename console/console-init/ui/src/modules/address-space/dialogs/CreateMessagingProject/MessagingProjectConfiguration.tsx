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
import { dnsSubDomainRfc1123NameRegexp } from "types/Configs";

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
  setShowConfigurationStep: (value: boolean) => void;
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
  setProjectDetail,
  setShowConfigurationStep
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
  const setField = (field: string, value: string) => {
    let project: IMessagingProject = JSON.parse(JSON.stringify(projectDetail));
    switch (field) {
      case "name":
        project.name = value.trim();
        project.isNameValid = dnsSubDomainRfc1123NameRegexp.test(value);
        break;
      case "namespace":
        project.namespace = value.trim();
        console.log("ad");
        break;
      case "isNameValid":
        project.isNameValid = value === "true" ? true : false;
        break;
      case "type":
        project.type = value.trim();
        project.plan = "";
        project.authService = "";
        break;
      case "plan":
        project.plan = value.trim();
        break;
      case "authService":
        project.authService = value.trim();
        break;
      case "customizeEndpoint":
        project.customizeEndpoint = value === "true" ? true : false;
        setShowConfigurationStep(project.customizeEndpoint);
        break;
    }
    console.log(project);
    setProjectDetail(project);
  };

  const onNameSpaceSelect = (value: string) => {
    console.log("Namespace", value);
    setField("namespace", value);
  };
  const handleNameChange = (value: string) => {
    setField("name", value);
  };
  const handleBrokeredChange = () => {
    // if (projectDetail.type !== "brokered") {
    setField("type", "brokered");
    // }
  };
  const handleStandardChange = () => {
    // if (projectDetail.type !== "standard") {
    setField("type", "standard");
    // }
  };
  const onPlanSelect = (value: string) => {
    setField("plan", value);
  };
  const onAuthenticationServiceSelect = (value: string) => {
    setField("authService", value);
  };

  const getNameSpaceOptions = () => {
    let nameSpaceOptions: any[];
    nameSpaceOptions = namespaces.map((namespace: any) => {
      return {
        value: namespace.metadata.name,
        label: namespace.metadata.name
      };
    });
    return nameSpaceOptions;
  };
  const hanldeCustomEndpointChange = (value: boolean) => {
    setField("customizeEndpoint", value.toString());
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
      authenticationServices.addressSpaceSchema_v2.forEach(
        (authService: any) => {
          if (authService.metadata.name === type) {
            authenticationServiceOptions = authService.spec.authenticationServices.map(
              (service: any) => {
                return {
                  value: service,
                  label: service
                };
              }
            );
          }
        }
      );
    }
    return authenticationServiceOptions;
  };

  return (
    <>
      <>
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
          hanldeCustomEndpointChange={hanldeCustomEndpointChange}
        />
      </>
    </>
  );
};

export { MessagingProjectConfiguration };

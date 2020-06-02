/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Loading } from "use-patternfly";
import { useQuery } from "@apollo/react-hooks";
import { ADDRESS_SPACE_COMMAND_REVIEW_DETAIL } from "graphql-module/queries";
import { AddressSpaceReview } from "modules/address-space/components";
import { IMessagingProject, IExposeEndPoint } from "./CreateMessagingProject";
import {
  EndPointProtocol,
  TlsCertificateType
} from "modules/address-space/utils";

interface IMessagingProjectReviewProps {
  projectDetail: IMessagingProject;
}

export const MessagingProjectReview: React.FunctionComponent<IMessagingProjectReviewProps> = ({
  projectDetail
}) => {
  const {
    name,
    namespace,
    type,
    plan,
    authService,
    protocols,
    customizeEndpoint,
    addRoutes,
    tlsCertificate
  } = projectDetail || {};

  const queryVariable = {
    variables: {
      as: {
        metadata: {
          name: name,
          namespace: namespace
        },
        spec: {
          plan: plan ? plan.toLowerCase() : "",
          type: type ? type.toLowerCase() : "",
          authenticationService: {
            name: authService
          }
        }
      }
    }
  };

  if (customizeEndpoint) {
    const { certValue, privateKey, routesConf } = projectDetail;
    const endpoints: IExposeEndPoint[] = [];
    if (protocols && protocols.length > 0) {
      protocols.map((protocol: string) => {
        const endpoint: IExposeEndPoint = { service: "messaging" };
        if (protocol === EndPointProtocol.AMQPS) {
          endpoint.name = "messaging";
        } else if (protocol === EndPointProtocol.AMQP_WSS) {
          endpoint.name = "messaging-wss";
        }
        if (tlsCertificate) {
          endpoint.certificate = {
            provider: tlsCertificate
          };
          if (
            tlsCertificate === TlsCertificateType.UPLOAD_CERT &&
            certValue &&
            certValue.trim() !== "" &&
            privateKey &&
            privateKey.trim() !== ""
          ) {
            endpoint.certificate = {
              ...endpoint.certificate,
              tlsKey: privateKey?.trim(),
              tlsCert: certValue?.trim()
            };
          }
        }
        if (addRoutes) {
          endpoint.expose = { type: "route", routeServicePort: protocol };
          const routeConf = routesConf?.filter(
            conf => conf.protocol === protocol
          );
          if (routeConf && routeConf.length > 0) {
            if (routeConf[0].hostname && routeConf[0].hostname.trim() !== "") {
              endpoint.expose.routeHost = routeConf[0].hostname.trim();
            }
            if (
              routeConf[0].tlsTermination &&
              routeConf[0].tlsTermination.trim() !== ""
            ) {
              endpoint.expose.routeTlsTermination = routeConf[0].tlsTermination;
            }
          }
        }
        endpoints.push(endpoint);
        return endpoint;
      });
    }
    Object.assign(queryVariable.variables.as.spec, { endpoints: endpoints });
  }

  const { data, loading } = useQuery(
    ADDRESS_SPACE_COMMAND_REVIEW_DETAIL,
    queryVariable
  );

  if (loading) return <Loading />;

  return (
    <AddressSpaceReview
      name={name}
      plan={plan}
      type={type}
      namespace={namespace || ""}
      authenticationService={authService || ""}
      data={data}
      protocols={protocols}
      customizeEndpoint={customizeEndpoint}
      addRoutes={addRoutes}
      tlsCertificate={tlsCertificate}
    />
  );
};

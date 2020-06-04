/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Title, Grid, GridItem } from "@patternfly/react-core";
import { InputText, StatusLabelWithIcon } from "components";
import { Protocols } from "constant";

export interface IAdapterConfig {
  url?: string;
  host?: string;
  port?: number;
  tlsEnabled?: boolean;
}

export interface IAdapter {
  type:
    | Protocols.HTTP
    | Protocols.HTTPS
    | Protocols.MQTT
    | Protocols.AMQP
    | Protocols.AMQPS
    | Protocols.COAP;
  value: IAdapterConfig;
}

export interface IAdapterListProps {
  id: string;
  adapters: IAdapter[];
}

export const AdapterList: React.FC<IAdapterListProps> = ({ id, adapters }) => {
  return (
    <div id={id}>
      {adapters &&
        adapters.map((adapter: IAdapter, index: number) => (
          <Adapter adapter={adapter} key={`adapter-${adapter.type}`} />
        ))}
    </div>
  );
};

const Adapter: React.FC<{ adapter: IAdapter }> = ({ adapter }) => {
  const { type, value } = adapter || {};
  const { tlsEnabled, host, port, url } = value || {};

  return (
    <>
      <Title size="xl" headingLevel="h2">
        {type?.toUpperCase() + " Adapter"}
      </Title>
      {tlsEnabled && (
        <>
          <br />
          <Grid>
            <GridItem span={4}>
              <b>TLS</b>
            </GridItem>
            <GridItem span={8}>
              <StatusLabelWithIcon
                id="adapter-list-tls-status-label"
                isEnabled={tlsEnabled}
              />
            </GridItem>
          </Grid>
        </>
      )}
      {url && (
        <>
          <br />
          <InputText
            label={"URL"}
            type={"text"}
            value={url}
            isReadOnly={true}
            enableCopy={true}
            id={`adapter-${type}-api-url-input`}
            key={`adapter-${type}-api-url-input`}
            ariaLabel={`adapter-${type}-api-url`}
            isExpandable={false}
          />
        </>
      )}
      {host && (
        <>
          <br />
          <InputText
            label={"Host"}
            type={"number"}
            value={host}
            isReadOnly={true}
            enableCopy={true}
            id={`adapter-${type}-api-host-input`}
            ariaLabel={`adapter ${type} api host`}
            isExpandable={false}
          />
        </>
      )}
      {port && (
        <>
          <br />
          <InputText
            label={"Port"}
            type={"number"}
            value={port}
            isReadOnly={true}
            enableCopy={true}
            id={`adapter-${type}-api-port-input`}
            ariaLabel={`adapter ${type} api port`}
            isExpandable={false}
          />
        </>
      )}
    </>
  );
};

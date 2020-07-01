/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Title, Grid, GridItem } from "@patternfly/react-core";
import { InputText, StatusLabelWithIcon } from "components";
import { Protocols } from "constant";
export interface IAdapter {
  name?:
    | Protocols.HTTP
    | Protocols.HTTPS
    | Protocols.MQTT
    | Protocols.AMQP
    | Protocols.AMQPS
    | Protocols.COAP
    | string;
  url?: string;
  host?: string;
  port?: number;
  tls?: boolean;
}

export interface IAdapterListProps {
  id: string;
  adapters: IAdapter[];
}

export const AdapterList: React.FC<IAdapterListProps> = ({ id, adapters }) => {
  return (
    <div id={id}>
      {adapters &&
        adapters.map((adapter: IAdapter) => (
          <>
            <Adapter adapter={adapter} key={`adapter-${adapter.name}`} />
            <br />
          </>
        ))}
    </div>
  );
};

const Adapter: React.FC<{ adapter: IAdapter }> = ({ adapter }) => {
  const { name, tls, host, port, url } = adapter || {};

  const getAdapterHeader = () => {
    const splitName = name?.split("Adapter");
    if (splitName && splitName?.length === 2) {
      return `${splitName[0].toUpperCase() + " Adapter"}`;
    }
    return name;
  };

  return (
    <>
      <Title size="xl" headingLevel="h2">
        {getAdapterHeader()}
      </Title>
      {tls && (
        <>
          <br />
          <Grid>
            <GridItem span={4}>
              <b>TLS</b>
            </GridItem>
            <GridItem span={8}>
              <StatusLabelWithIcon
                id="adapter-list-tls-status-label"
                enabledTitle="Required"
                isEnabled={tls}
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
            id={`adapter-${name}-api-url-input`}
            key={`adapter-${name}-api-url-input`}
            ariaLabel={`adapter-${name}-api-url`}
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
            id={`adapter-${name}-api-host-input`}
            ariaLabel={`adapter ${name} api host`}
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
            id={`adapter-${name}-api-port-input`}
            ariaLabel={`adapter ${name} api port`}
            isExpandable={false}
          />
        </>
      )}
    </>
  );
};

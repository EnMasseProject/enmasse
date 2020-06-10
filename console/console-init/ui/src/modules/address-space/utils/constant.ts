/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
enum TlsCertificateType {
  AUTO_GENERATE = "selfsigned",
  UPLOAD_CERT = "certBundle",
  USE_OPENSHIFT_CERT = "openshift"
}

enum EndPointProtocol {
  AMQPS = "amqps",
  AMQP_WSS = "https"
}

enum TlsTerminationType {
  RE_ENCRYPT = "reencrypt",
  PASSTHROUGH = "passthrough"
}

export interface IOptionForKeyValueLabel {
  key: string;
  value: string;
  label: string;
}

const endpointProtocolOptions: IOptionForKeyValueLabel[] = [
  {
    key: EndPointProtocol.AMQPS,
    value: EndPointProtocol.AMQPS,
    label: "Messaging (AMQPS)"
  },
  {
    key: EndPointProtocol.AMQP_WSS,
    value: EndPointProtocol.AMQP_WSS,
    label: "Websocket Messaging (AMQP-WSS)"
  }
];

export {
  TlsCertificateType,
  endpointProtocolOptions,
  EndPointProtocol,
  TlsTerminationType
};

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
enum TlsCertificateType {
  AUTO_GENERATE = "auto-generate",
  UPLOAD_CERT = "upload-cert",
  USE_OPENSHIFT_CERT = "use-openshift-cert",
  NONE = "none"
}

enum EndPointProtocol {
  AMQP = "amqp",
  AMQP_WS = "amqpws"
}

enum TlsTerminationType {
  RE_ENCRYPT = "re-encrypt",
  PASSTHROUGH = "passthrough"
}

export interface IOptionForKeyValueLabel {
  key: string;
  value: string;
  label: string;
}

const tlsCertificateOptions: IOptionForKeyValueLabel[] = [
  {
    key: TlsCertificateType.AUTO_GENERATE,
    value: TlsCertificateType.AUTO_GENERATE,
    label: "Automatic generate a self signed certificate"
  },
  {
    key: TlsCertificateType.UPLOAD_CERT,
    value: TlsCertificateType.UPLOAD_CERT,
    label: "Upload Certificates"
  },
  {
    key: TlsCertificateType.USE_OPENSHIFT_CERT,
    value: TlsCertificateType.USE_OPENSHIFT_CERT,
    label: "Use OpenShift provided certificate"
  },
  {
    key: TlsCertificateType.NONE,
    value: TlsCertificateType.NONE,
    label: "None"
  }
];

const endpointProtocolOptions: IOptionForKeyValueLabel[] = [
  {
    key: EndPointProtocol.AMQP,
    value: EndPointProtocol.AMQP,
    label: "Messaging (AMQP)"
  },
  {
    key: EndPointProtocol.AMQP_WS,
    value: EndPointProtocol.AMQP_WS,
    label: "Websocket Messaging (AMQP-WS)"
  }
];

const tlsTerminationOptions: IOptionForKeyValueLabel[] = [
  {
    key: TlsTerminationType.RE_ENCRYPT,
    value: TlsTerminationType.RE_ENCRYPT,
    label: "Re-encrypt"
  },
  {
    key: TlsTerminationType.PASSTHROUGH,
    value: TlsTerminationType.PASSTHROUGH,
    label: "Passthrough"
  }
];

export {
  tlsCertificateOptions,
  TlsCertificateType,
  endpointProtocolOptions,
  EndPointProtocol,
  tlsTerminationOptions,
  TlsTerminationType
};

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Title,
  Radio,
  Button,
  Modal,
  FileUpload
} from "@patternfly/react-core";
import { UploadIcon } from "@patternfly/react-icons";
import { TemplateType } from "constant";
import { getFormattedJsonString } from "utils";

export const directlyConnectedDeviceTemplate = {
  id: "<optional-id>",
  registration: {
    enabled: true,
    defaults: {},
    ext: {},
    credentials: []
  }
};
export const connectedViaGatewayDeviceTemplate = {
  id: "<optional-id>",
  registration: {
    enabled: true,
    defaults: {
      "content-type": "text/plain"
    },
    via: [],
    ext: {}
  }
};
interface IAddJsonUsingTemplate {
  setDetail: (value: string) => void;
  selectedTemplate: string;
  setSelectedTemplate: (value: string) => void;
}
const AddJsonUsingTemplate: React.FunctionComponent<IAddJsonUsingTemplate> = ({
  setDetail,
  selectedTemplate,
  setSelectedTemplate
}) => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const onChange = (event: any) => {
    const name: string = event.target.name;
    if (name === "device-connected-directly") {
      setSelectedTemplate(TemplateType.DIRECTLY_CONNECTED);
    } else if (name === "device-via-gateway") {
      setSelectedTemplate(TemplateType.VIA_GATEWAY);
    }
  };
  const onClickTry = () => {
    if (selectedTemplate === TemplateType.DIRECTLY_CONNECTED) {
      setDetail(getFormattedJsonString(directlyConnectedDeviceTemplate));
    } else if (selectedTemplate === TemplateType.VIA_GATEWAY) {
      setDetail(getFormattedJsonString(connectedViaGatewayDeviceTemplate));
    }
  };
  const onReaderLoad = (event: any) => {
    try {
      const detail = JSON.parse(event.target.result);
      setDetail(getFormattedJsonString(detail));
    } catch {
      //TODO: Error handling for invalid json
      console.log("Invalid Json");
    }
  };

  const onClickUpload = () => {
    setShowModal(true);
  };

  const onUploadFile = (value: string | File, filename: string, event: any) => {
    var reader = new FileReader();
    reader.onload = onReaderLoad;
    reader.readAsText(event.target.files[0]);
    setShowModal(false);
  };

  return (
    <>
      <Title headingLevel="h1" size="lg" id="title-select-json">
        Select a JSON template for quick start
      </Title>
      <br />
      <Radio
        style={{ marginLeft: 10 }}
        isLabelWrapped
        isChecked={selectedTemplate === TemplateType.DIRECTLY_CONNECTED}
        onClick={onChange}
        label="Device connected directly"
        id="add-json-template-device-connected-directly-radio"
        name="device-connected-directly"
      />
      <br />
      <Radio
        style={{ marginLeft: 10 }}
        isLabelWrapped
        isChecked={selectedTemplate === TemplateType.VIA_GATEWAY}
        onClick={onChange}
        label="Device connected via gateway"
        id="add-json-template-device-via-gateway-radio"
        name="device-via-gateway"
      />
      <br />
      <Button
        variant="secondary"
        onClick={onClickTry}
        id="add-json-template-try-it-button"
      >
        Try it
      </Button>
      <br />
      <br />
      <Title headingLevel="h3" size="md" id="title-upload-json">
        or Upload a JSON file
      </Title>
      <br />
      <Button
        variant="secondary"
        onClick={onClickUpload}
        id="add-json-template-upload-json-button"
      >
        <UploadIcon /> Upload a JSON file
      </Button>
      {showModal && (
        <Modal title="" isOpen={showModal} onClose={() => setShowModal(false)}>
          <FileUpload
            id="add-json-template-file-upload"
            value={""}
            filename={""}
            onChange={onUploadFile}
            dropzoneProps={{
              accept: ".json"
            }}
          />
        </Modal>
      )}
    </>
  );
};

export { AddJsonUsingTemplate };

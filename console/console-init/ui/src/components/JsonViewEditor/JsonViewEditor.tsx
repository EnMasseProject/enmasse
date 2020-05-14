/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Editor } from "components";
import {
  PageSection,
  PageSectionVariants,
  Button,
  Tooltip,
  TooltipPosition,
  ButtonVariant
} from "@patternfly/react-core";
import { CopyIcon } from "@patternfly/react-icons";

interface IJsonViewEditorProps {
  detailInJson?: any;
  readOnly: boolean;
  style?: React.CSSProperties;
}

const JsonViewEditor: React.FunctionComponent<IJsonViewEditorProps> = ({
  detailInJson,
  readOnly,
  style
}) => {
  const value = detailInJson && JSON.stringify(detailInJson, undefined, 2);
  const [isCopied, setIsCopied] = useState<boolean>(false);
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Tooltip
        id="tooltip-feedback-for-succes-copy"
        position={TooltipPosition.bottom}
        enableFlip={false}
        trigger={"manual"}
        content={<div>Successfully copied to the clipboard</div>}
        isVisible={isCopied}
      >
        <span>
          <Tooltip
            id="tooltip-with-copy-info"
            position={TooltipPosition.bottom}
            enableFlip={false}
            content={<div>Copy data to the clipboard</div>}
          >
            <Button
              id="preview-addr-copy-configuration-button"
              variant={ButtonVariant.link}
              aria-label="copy-configuration"
              onClick={() => {
                navigator.clipboard.writeText(value);
                setIsCopied(true);
              }}
              onMouseLeave={() => {
                setIsCopied(false);
              }}
            >
              <CopyIcon id="preview-addr-copy-btn" size="md" /> Copy to
              clipboard
            </Button>
          </Tooltip>
        </span>
      </Tooltip>
      <br />
      <Editor
        mode="json"
        readOnly={readOnly}
        value={value}
        style={style}
        // className={className}
      />
    </PageSection>
  );
};

export { JsonViewEditor };

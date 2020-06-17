/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Editor } from "components";
import {
  Tooltip,
  TooltipPosition,
  Flex,
  FlexItem,
  FlexModifiers
} from "@patternfly/react-core";
import { CopyIcon } from "@patternfly/react-icons";
import { IAceEditorProps } from "react-ace";

interface IJsonEditorProps extends IAceEditorProps {
  setDetail?: (detail: string) => void;
  tooltipKey?: string;
}

const JsonEditor: React.FunctionComponent<IJsonEditorProps> = ({
  value,
  readOnly,
  style,
  name,
  height,
  width,
  setDetail,
  tooltipKey = "",
  className
}) => {
  const onChange = (value: string) => {
    setDetail && setDetail(value.trim());
  };
  const [isCopied, setIsCopied] = useState<boolean>(false);
  return (
    <>
      <Flex>
        <FlexItem breakpointMods={[{ modifier: FlexModifiers["align-right"] }]}>
          <Tooltip
            id={`tooltip-feedback-for-success-copy-${tooltipKey}`}
            position={TooltipPosition.left}
            enableFlip={false}
            trigger={"manual"}
            content={<div>Successfully copied to the clipboard</div>}
            isVisible={isCopied}
          >
            <span>
              <Tooltip
                id={`tooltip-with-copy-info-${tooltipKey}`}
                position={TooltipPosition.left}
                enableFlip={false}
                content={<div>Copy data to the clipboard</div>}
              >
                <CopyIcon
                  id={`copy-to-clipboard-${tooltipKey}`}
                  size="md"
                  onClick={() => {
                    navigator.clipboard.writeText(value ? value.trim() : "");
                    setIsCopied(true);
                  }}
                />
              </Tooltip>
            </span>
          </Tooltip>
        </FlexItem>
      </Flex>
      <br />
      <Editor
        mode="json"
        readOnly={readOnly}
        onChange={onChange}
        value={value}
        style={style}
        name={name}
        height={height}
        width={width || "auto"}
        className={className}
      />
    </>
  );
};

export { JsonEditor };

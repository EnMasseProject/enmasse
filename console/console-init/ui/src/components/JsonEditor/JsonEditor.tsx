/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { Editor } from "components";
import {
  Tooltip,
  TooltipPosition,
  Flex,
  FlexItem
} from "@patternfly/react-core";
import { CopyIcon } from "@patternfly/react-icons";
import { IAceEditorProps } from "react-ace";

interface IJsonEditorProps extends IAceEditorProps {
  setDetail?: (detail: string | undefined) => void;
  manageState?: boolean;
  tooltipKey?: string;
}

const JsonEditor: React.FunctionComponent<IJsonEditorProps> = ({
  value: jsonValue,
  readOnly,
  manageState = true,
  style,
  name,
  height,
  width,
  setDetail,
  tooltipKey = "",
  className
}) => {
  const [isCopied, setIsCopied] = useState<boolean>(false);
  const [value, setValue] = useState<string | undefined>(jsonValue);

  const onChange = (value: string) => {
    if (manageState) {
      setValue(value);
    } else {
      setDetail && setDetail(value);
    }
  };

  useEffect(() => {
    setDetail && setDetail(value);
  }, [value]);

  return (
    <>
      <Flex>
        <FlexItem align={{ default: "alignRight" }}>
          <Tooltip
            id={`json-editor-successfully-copied-tooltip-${tooltipKey}`}
            position={TooltipPosition.left}
            enableFlip={false}
            trigger={"manual"}
            content={<div>Successfully copied to the clipboard</div>}
            isVisible={isCopied}
          >
            <span>
              <Tooltip
                id={`json-editor-copy-data-tooltip-${tooltipKey}`}
                position={TooltipPosition.left}
                enableFlip={false}
                content={<div>Copy data to the clipboard</div>}
              >
                <CopyIcon
                  id={`json-editor-clipboard-copyicon-${tooltipKey}`}
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
        value={manageState ? value : jsonValue}
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

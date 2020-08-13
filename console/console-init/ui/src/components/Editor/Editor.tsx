/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import AceEditor, { IAceEditorProps } from "react-ace";
import { StyleSheet, css } from "aphrodite";
const styles = StyleSheet.create({
  editor_border: {
    border: "1px solid var(--pf-global--BorderColor--100)"
  }
});

const Editor: React.FunctionComponent<IAceEditorProps> = ({
  value,
  readOnly,
  mode,
  style,
  theme = "github",
  name,
  fontSize = 14,
  width = "auto",
  onChange,
  enableBasicAutocompletion,
  enableLiveAutocompletion,
  className,
  height
}) => {
  return (
    <AceEditor
      mode={mode}
      theme={theme}
      width={width}
      fontSize={fontSize}
      readOnly={readOnly}
      onChange={onChange}
      value={value}
      name={name}
      enableBasicAutocompletion={enableBasicAutocompletion}
      enableLiveAutocompletion={enableLiveAutocompletion}
      style={style}
      height={height}
      className={
        className
          ? className && css(styles.editor_border)
          : css(styles.editor_border)
      }
    />
  );
};

export { Editor };

import React from "react";
import AceEditor, { IAceEditorProps } from "react-ace";

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
  enableLiveAutocompletion
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
    />
  );
};

export { Editor };

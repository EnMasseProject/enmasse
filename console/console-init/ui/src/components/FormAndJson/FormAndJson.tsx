import React, { useState } from "react";
import { ToggleOffIcon, ToggleOnIcon, IconSize } from "@patternfly/react-icons";
import { Editor } from "components";
interface IFormAndJsonProps {
  detail?: string;
  detailInJson?: any;
  setDetail: (value: string) => void;
  setDetailInJson: (value: any) => void;
}
const FormAndJson: React.FunctionComponent<IFormAndJsonProps> = ({
  detail,
  detailInJson,
  setDetail,
  setDetailInJson
}) => {
  const [isValid, setIsValid] = useState<boolean>(true);
  const [showJson, setShowJson] = useState<boolean>(true);
  //   const setDetail1 = () => {
  //     const obj = {
  //       persons: [
  //         { name: "John", age: 30, city: "New York" },
  //         { name: "John", age: 30, city: "New York" },
  //         { name: "John", age: 30, city: "New York" }
  //       ]
  //     };
  //     setDetail(JSON.stringify(obj, undefined, 2));
  //     setDetailJson(obj);
  //   };
  //   if (!detail) {
  //     setDetail1();
  //   }
  const onToggle = () => {
    if (showJson) {
      try {
        if (detail) {
          const obj = JSON.parse(detail);
          setDetail(JSON.stringify(obj, undefined, 2));
          setDetailInJson(obj);
          setShowJson(false);
          setIsValid(true);
        }
      } catch {
        setIsValid(false);
      }
    } else {
      setShowJson(!showJson);
    }
  };
  const onChange = (value: string) => {
    setDetail(value);
  };
  return (
    <>
      {!isValid && <h1 style={{ color: "red" }}>inValid</h1>}
      {showJson ? (
        <>
          <ToggleOnIcon color="blue" size={IconSize.lg} onClick={onToggle} />
          <Editor
            mode="json"
            value={detail}
            onChange={onChange}
            enableBasicAutocompletion={true}
            enableLiveAutocompletion={true}
          />
        </>
      ) : (
        <>
          <ToggleOffIcon color="black" size={IconSize.lg} onClick={onToggle} />
          {console.log(detailInJson)}
        </>
      )}
    </>
  );
};

export { FormAndJson };

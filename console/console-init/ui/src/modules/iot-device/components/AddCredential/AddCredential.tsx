/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { CredentialList, ICredential } from "modules/iot-device/components";
import { uniqueId, findIndexByProperty } from "utils";
import { CredentialsType } from "constant";
import {
  getCredentialsFieldsInitialState,
  getFormInitialStateByProperty
} from "modules/iot-device/utils";
import { OperationType } from "constant";

export interface IAddCredentialProps {
  id?: string;
  setCredentialList?: (credentials: ICredential[]) => void;
  operation?: OperationType.ADD | OperationType.EDIT;
  credentials?: ICredential[];
}

export const AddCredential: React.FC<IAddCredentialProps> = ({
  id = "add-credential",
  setCredentialList,
  operation = OperationType.ADD,
  credentials: credentialList = []
}) => {
  const [activeCredentialFormId, setActiveCredentialFormId] = useState<string>(
    ""
  );
  const [operationType, setOperationType] = useState(operation);
  const [credentials, setCredentials] = useState(
    operationType === OperationType.EDIT
      ? credentialList
      : [getCredentialsFieldsInitialState()]
  );
  const defaultSelectedType =
    operationType === OperationType.EDIT ? "" : CredentialsType.PASSWORD;
  const [type, setType] = useState<string>(defaultSelectedType);

  function getInitialSecretState(credentials: ICredential[] = []) {
    let newCredentials: ICredential[] = [...credentials];
    let initialStateSecret = {};
    const activeFormId =
      activeCredentialFormId || newCredentials[newCredentials?.length - 1]?.id;
    const credIndex = findIndexByProperty(credentials, "id", activeFormId);
    const initialState = getFormInitialStateByProperty(
      newCredentials,
      "secrets",
      credIndex > 0 ? credIndex : 0
    );
    initialStateSecret = { id: uniqueId(), ...initialState };

    return initialStateSecret;
  }

  const setSecretsInitialFormState = () => {
    let newCredentials: ICredential[] = [...credentials];
    const activeFormId =
      activeCredentialFormId || newCredentials[newCredentials?.length - 1]?.id;

    const credIndex = findIndexByProperty(credentials, "id", activeFormId);
    if (credIndex >= 0) {
      const initialStateSecret = getInitialSecretState(newCredentials);
      if (operationType !== OperationType.EDIT) {
        newCredentials[credIndex]["secrets"] = [initialStateSecret];
      }
      setCredentials(newCredentials);
    }
  };

  useEffect(() => {
    setSecretsInitialFormState();
  }, [type]);

  useEffect(() => {
    setCredentialList && setCredentialList(credentials);
  }, [credentials]);

  const onSelectType = (id: string, event: any, value: string) => {
    setOperationType(OperationType.ADD);
    setType(value);
    setActiveCredentialFormId(id);
    handleInputChange(id, event, value);
  };

  const addMoreSecretOrExt = (id?: string, property?: string) => {
    let newCredentials: ICredential[] = [];
    if (property && id) {
      newCredentials = [...credentials];
      const credIndex: number = findIndexByProperty(credentials, "id", id);
      if (credIndex >= 0) {
        const initialState = getFormInitialStateByProperty(
          newCredentials,
          property,
          credIndex
        );
        const secrets = (newCredentials[credIndex] as any)[property];
        (newCredentials[credIndex] as any)[property] = [
          ...secrets,
          { id: uniqueId(), ...initialState }
        ];
      }
    }
    return newCredentials;
  };

  const addMoreCredentials = () => {
    let newCredentials: ICredential[] = [];
    newCredentials = [...credentials, getCredentialsFieldsInitialState()];
    return newCredentials;
  };

  const addMoreItem = (id?: string, property?: string) => {
    let newCredentials: ICredential[] = [];
    /**
     * add more items for child fields i.e. secrets and extensions
     */
    if (property && id) {
      newCredentials = addMoreSecretOrExt(id, property);
    } else {
      /**
       * add more credentials
       */
      newCredentials = addMoreCredentials();
    }
    setCredentials(newCredentials);
  };

  const deleteSecretOrExt = (
    id: string,
    credentials: ICredential[],
    credIndex: number,
    property?: string,
    childObjId?: string
  ) => {
    if (id && property && childObjId && credIndex >= 0) {
      const items = (credentials[credIndex] as any)[property];
      const itemIndex = findIndexByProperty(items, "id", childObjId);
      itemIndex >= 0 &&
        (credentials[credIndex] as any)[property].splice(itemIndex, 1);
    }
  };

  const deleteCredential = (credentials: ICredential[], credIndex: number) => {
    credentials.splice(credIndex, 1);
  };

  const onDeleteItem = (id: string, property?: string, childObjId?: string) => {
    let newCredentials: ICredential[] = [...credentials];
    const credIndex: number = findIndexByProperty(credentials, "id", id);
    /**
     * delete items for child fields i.e. secrets and extensions
     */
    if (id && property && childObjId && credIndex >= 0) {
      deleteSecretOrExt(id, newCredentials, credIndex, property, childObjId);
    } else if (id && credIndex >= 0) {
      /**
       * delete credentilas
       */
      deleteCredential(newCredentials, credIndex);
    }
    setCredentials(newCredentials);
  };

  const handleSecretOrExtInputChange = (
    credentials: ICredential[],
    childObjId: string,
    property: string,
    credIndex: number,
    elementName: string,
    value: string | boolean
  ) => {
    const items = (credentials[credIndex] as any)[property];
    const itemIndex = findIndexByProperty(items, "id", childObjId);
    if (itemIndex >= 0) {
      (credentials[credIndex] as any)[property][itemIndex][elementName] = value;
    }
  };

  const handleCredentialsInputChange = (
    credentials: ICredential[],
    credIndex: number,
    elementName: string,
    value: string | boolean
  ) => {
    (credentials[credIndex] as any)[elementName] = value;
  };

  const handleInputChange = (
    id: string,
    evt: any,
    value: string | boolean,
    childObjId?: string,
    property?: string
  ) => {
    const elementName: string = evt.target.name;
    const newCredentials: ICredential[] = [...credentials];
    const credIndex: number = findIndexByProperty(newCredentials, "id", id);
    /**
     * save child object's fields value i.e. secrets and ext
     */
    if (
      credIndex >= 0 &&
      elementName?.trim() &&
      childObjId?.trim() &&
      property?.trim()
    ) {
      handleSecretOrExtInputChange(
        newCredentials,
        childObjId,
        property,
        credIndex,
        elementName,
        value
      );
    } else {
      /**
       * save crentials (parent object) fields value
       */
      handleCredentialsInputChange(
        newCredentials,
        credIndex,
        elementName,
        value
      );
    }
    setCredentials(newCredentials);
  };

  const onToggleAdvancedSetting = (id: string) => {
    const newCredentials: ICredential[] = [...credentials];
    const index = findIndexByProperty(newCredentials, "id", id);
    if (index >= 0) {
      const isExpandedAdvancedSetting =
        newCredentials[index]["isExpandedAdvancedSetting"];
      newCredentials[index][
        "isExpandedAdvancedSetting"
      ] = !isExpandedAdvancedSetting;
      setCredentials(newCredentials);
    }
  };

  return (
    <PageSection id={id} variant={PageSectionVariants.light}>
      <CredentialList
        credentials={credentials}
        handleInputChange={handleInputChange}
        onToggleAdvancedSetting={onToggleAdvancedSetting}
        onSelectType={onSelectType}
        addMoreItem={addMoreItem}
        onDeleteItem={onDeleteItem}
      />
    </PageSection>
  );
};

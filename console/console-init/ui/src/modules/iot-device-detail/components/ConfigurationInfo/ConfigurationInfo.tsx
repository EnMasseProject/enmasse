/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Page,
  PageSection,
  Grid,
  GridItem,
  Card,
  CardBody,
  Title,
  DropdownPosition,
  Tooltip,
  TooltipPosition,
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { FilterIcon, ExclamationCircleIcon } from "@patternfly/react-icons";
import { StyleSheet } from "@patternfly/react-styles";
import { AdapterList, IAdapterListProps } from "components";
import {
  CredentialsView,
  ICredentialsViewProps
} from "modules/iot-device-detail/components";
import { DropdownWithToggle, IDropdownOption } from "components";
import {
  credentialsTypeOptions,
  getDefaultCredentialsFiterOption
} from "modules/iot-device-detail/utils";
import { CredentialsType } from "constant";

const style = StyleSheet.create({
  filter_dropdown: {
    paddingLeft: 20
  }
});

export interface IConfigurationInfoProps
  extends Pick<IAdapterListProps, "adapters">,
    Pick<ICredentialsViewProps, "credentials"> {
  id: string;
  onSelectFilterType?: (value: string) => void;
  onSelectFilterValue?: (value: string) => void;
}

export const ConfigurationInfo: React.FC<IConfigurationInfoProps> = ({
  id,
  adapters,
  credentials,
  onSelectFilterType,
  onSelectFilterValue
}) => {
  const [credentialType, setCredentialType] = useState<string>("all");
  const [filterOptions, setFilterOptions] = useState<IDropdownOption[]>([]);
  const [selectedFilterValue, setSelectedFilterValue] = useState();

  useEffect(() => {
    getFilterOptions();
  }, [credentialType, credentials]);

  const onSelectCredentialType = (value: string) => {
    setCredentialType(value);
    /**
     * reset default selected value of filter dropdown
     */
    onSelectFilterValue && onSelectFilterValue("all");
    onSelectFilterType && onSelectFilterType(value);
  };

  const onSelectFilterItem = (value: string) => {
    setSelectedFilterValue(value);
    onSelectFilterValue && onSelectFilterValue(value);
  };

  const shouldDisplayChildDropdown = () => {
    if (
      credentialType === CredentialsType.PASSWORD ||
      credentialType == CredentialsType.PSK ||
      credentialType === CredentialsType.X509_CERTIFICATE
    ) {
      return true;
    }
    return false;
  };

  const getFilterOptions = () => {
    const filterOptions = getDefaultCredentialsFiterOption(credentialType);
    const defaultSelectedOption = shouldDisplayChildDropdown()
      ? filterOptions[0]?.value
      : "";
    if (shouldDisplayChildDropdown() && credentials) {
      const newCredentials = [...credentials];
      newCredentials
        ?.filter((item: any) => item?.type === credentialType)
        .forEach((item: any) => {
          const { "auth-id": authId } = item;
          filterOptions.push({
            key: authId,
            value: authId,
            label: `Auth ID: ${authId}`
          });
        });
    }
    setSelectedFilterValue(defaultSelectedOption);
    setFilterOptions(shouldDisplayChildDropdown() ? filterOptions : []);
  };

  return (
    <>
      <Tooltip
        id="ci-help-tooltip"
        position={TooltipPosition.bottom}
        enableFlip={false}
        content={
          <>
            This info section provides a quick view of the information needed to
            configure a device connection on the device side.
          </>
        }
      >
        <Button
          id="ci-help-icon-button"
          icon={<ExclamationCircleIcon />}
          variant={ButtonVariant.link}
        >
          What is the device configuration info for?
        </Button>
      </Tooltip>
      <Page id={id}>
        <PageSection>
          <Grid gutter="sm">
            <GridItem span={6}>
              <Card>
                <CardBody>
                  <Title size="xl" headingLevel="h1">
                    <b>Adapters</b>
                  </Title>
                  <br />
                  <AdapterList id="ci-adapter" adapters={adapters} />
                </CardBody>
              </Card>
            </GridItem>
            <GridItem span={6}>
              <Grid gutter="sm">
                <GridItem>
                  <Card>
                    <CardBody>
                      <DropdownWithToggle
                        id="ci-credential-type-dropdown"
                        toggleId={"ci-credential-type-dropdown"}
                        position={DropdownPosition.left}
                        onSelectItem={onSelectCredentialType}
                        dropdownItems={credentialsTypeOptions}
                        value={credentialType && credentialType.trim()}
                        isLabelAndValueNotSame={true}
                        toggleIcon={
                          <>
                            <FilterIcon />
                            &nbsp;
                          </>
                        }
                      />
                      {filterOptions?.length > 0 && (
                        <DropdownWithToggle
                          id="ci-filter-dropdown"
                          toggleId={"ci-filter-dropdown"}
                          position={DropdownPosition.left}
                          onSelectItem={onSelectFilterItem}
                          dropdownItems={filterOptions}
                          value={selectedFilterValue}
                          isLabelAndValueNotSame={true}
                          className={style.filter_dropdown}
                        />
                      )}
                    </CardBody>
                  </Card>
                </GridItem>
              </Grid>
              <CredentialsView
                id="ci-credentials-view"
                credentials={credentials}
              />
            </GridItem>
          </Grid>
        </PageSection>
      </Page>
    </>
  );
};

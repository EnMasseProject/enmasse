import React, { useState } from "react";
import {
  Split,
  SplitItem,
  Title,
  Flex,
  FlexItem,
  DropdownItem,
  DropdownPosition,
  PageSection,
  PageSectionVariants,
  Badge,
  Switch
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithKebabToggle } from "components";

interface IIoTProjectDetailHeaderProps {
  projectName: string;
  type: string;
  status: string;
  isEnabled: boolean;
  changeEnable: (value: boolean) => void;
  onEdit: (name: string) => void;
  onDelete: (name: string) => void;
}

const styles = StyleSheet.create({
  flex_left_border: {
    paddingLeft: "1em",
    borderLeft: "0.05em solid",
    borderRightColor: "lightgrey"
  },
  address_space_icon_margin: {
    backgroundColor: "#EC7A08",
    fontSize: 25
  },
  address_icon_align: {
    paddingTop: 5,
    paddingRight: 16
  },
  kebab_toggle_margin: {
    marginTop: 20,
    marginLeft: 10,
    fontSize: 15
  },
  namespace_info_margin: {
    marginTop: 16,
    marginBottom: 24
  },
  no_bottom_padding: {
    paddingBottom: 0
  }
});

const IoTProjectDetailHeader: React.FunctionComponent<IIoTProjectDetailHeaderProps> = ({
  projectName,
  type,
  status,
  isEnabled,
  changeEnable,
  onEdit,
  onDelete
}) => {
  const AddressTitle = () => (
    <Split gutter="md">
      <SplitItem>
        <Title headingLevel="h1" size="4xl" id="adheader-name">
          {projectName}
        </Title>
      </SplitItem>
    </Split>
  );

  const AddressDetailInFlex = () => (
    <Flex className={css(styles.namespace_info_margin)}>
      <FlexItem id="adheader-plans">
        Type : <b>{type}</b>
      </FlexItem>
      <FlexItem
        id="adheader-subscription-topic"
        className={css(styles.flex_left_border)}
      >
        Status : <b>{status}</b>
      </FlexItem>
    </Flex>
  );

  const AddressDetailLayout = () => (
    <>
      <SplitItem className={css(styles.address_icon_align)}>
        <Badge
          style={{
            backgroundColor: "#EC7A08",
            fontSize: 25,
            paddingLeft: 15,
            paddingRight: 15,
            paddingTop: 5,
            paddingBottom: 5
          }}
        >
          IoT
        </Badge>
      </SplitItem>
      <SplitItem>
        <AddressTitle />
        <AddressDetailInFlex />
      </SplitItem>
    </>
  );
  const onChange = () => {
    changeEnable(!isEnabled);
  };
  const EnabledIcon = (
    <Switch
      id="simple-switch"
      label="View in Json"
      labelOff="View in Json"
      isChecked={isEnabled}
      onChange={onChange}
    />
  );

  const KebabOptionsLayout = () => {
    const dropdownItems = [
      <DropdownItem
        id="adheader-dropdown-item-edit"
        key="download"
        aria-label="download"
        onClick={() => onEdit(projectName)}
      >
        Edit
      </DropdownItem>,
      <DropdownItem
        id="adheader-dropdown-item-delete"
        key="delete"
        aria-label="delete"
        onClick={() => onDelete(projectName)}
      >
        Delete
      </DropdownItem>
    ];
    return (
      <DropdownWithKebabToggle
        id="adheader-dropdown"
        isPlain={true}
        position={DropdownPosition.right}
        toggleId="iot-project-header-kebab"
        dropdownItems={dropdownItems}
      />
    );
  };

  return (
    <PageSection
      variant={PageSectionVariants.light}
      className={css(styles.no_bottom_padding)}
    >
      <Split>
        <AddressDetailLayout />
        <SplitItem isFilled />
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          {EnabledIcon}
        </SplitItem>
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          <KebabOptionsLayout />
        </SplitItem>
      </Split>
    </PageSection>
  );
};

export { IoTProjectDetailHeader };

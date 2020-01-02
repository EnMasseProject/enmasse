import * as React from "react";
import {
  DataToolbarContent,
  DataToolbar,
  DataToolbarToggleGroup,
  DataToolbarChip,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  KebabToggle
} from "@patternfly/react-core";
import { CreateAddressSpace } from "../CreateAddressSpace/CreateAddressSpacePage";
import {
  AddressSpaceListFilter,
  AddressSpaceListKebab
} from "src/Components/AddressSpaceList/AddressSpaceListFilter";
import { ISortBy } from "@patternfly/react-table";
import { SortForMobileView } from "../../Components/Common/SortForMobileView";
import useWindowDimensions from "src/Components/Common/WindowDimension";

interface IAddressSpaceListFilterPageProps {
  filterValue?: string | null;
  setFilterValue: (value: string | null) => void;
  filterNames: string[];
  setFilterNames: (value: Array<string>) => void;
  filterNamespaces: string[];
  setFilterNamespaces: (value: Array<string>) => void;
  filterType?: string | null;
  setFilterType: (value: string | null) => void;
  totalAddressSpaces: number;
  setOnCreationRefetch?: (value: boolean) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
}
export const AddressSpaceListFilterPage: React.FunctionComponent<IAddressSpaceListFilterPageProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterNamespaces,
  setFilterNamespaces,
  filterType,
  setFilterType,
  totalAddressSpaces,
  setOnCreationRefetch,
  sortValue,
  setSortValue
}) => {
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);
  const { width } = useWindowDimensions();
  const onClearAllFilters = () => {
    setFilterValue("Name");
    setFilterNamespaces([]);
    setFilterNames([]);
    setFilterType(null);
  };
  const createAddressSpaceOnClick = async () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
  };
  const sortMenuItems = [{ key: "name", value: "Name", index: 1 }];
  const toolbarItems = (
    <>
      <AddressSpaceListFilter
        filterValue={filterValue}
        setFilterValue={setFilterValue}
        filterNames={filterNames}
        setFilterNames={setFilterNames}
        filterNamespaces={filterNamespaces}
        setFilterNamespaces={setFilterNamespaces}
        filterType={filterType}
        setFilterType={setFilterType}
        totalAddressSpaces={totalAddressSpaces}
      />
      {width < 769 && (
        <SortForMobileView
          sortMenu={sortMenuItems}
          sortValue={sortValue}
          setSortValue={setSortValue}
        />
      )}
      <DataToolbarItem>
        {isCreateWizardOpen && (
          <CreateAddressSpace
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
            setOnCreationRefetch={setOnCreationRefetch}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <AddressSpaceListKebab
          createAddressSpaceOnClick={createAddressSpaceOnClick}
        />
      </DataToolbarItem>
    </>
  );
  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}>
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};

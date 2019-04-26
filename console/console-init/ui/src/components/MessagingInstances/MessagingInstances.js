import React from 'react';
import {
  Badge,
  PageSection, PageSectionVariants, Pagination, PaginationVariant,
  Text,
  Toolbar, ToolbarGroup, ToolbarItem
} from '@patternfly/react-core';
import {Table, TableVariant, TableHeader, TableBody} from '@patternfly/react-table';

import {css} from '@patternfly/react-styles';
import spacingStyles from '@patternfly/patternfly/utilities/Spacing/spacing.css';
import flexStyles from '@patternfly/patternfly/utilities/Flex/flex.css';

import {deleteMessagingInstances} from './MessagingInstance/Enmasse/EnmasseAddressSpaces';

import moment from 'moment';

import Aux from '../../hoc/Aux/Aux';
import CreateAddressSpace from './MessagingInstance/Enmasse/CreateAddressSpace/CreateAddressSpace';
import InstancesActionKebab from './InstancesActionKebab/InstancesActionKebab';
import DeleteInstanceModal from './Delete/DeleteInstanceModal';
import {NotificationConsumer} from "../../context/notification-manager";

import {CheckCircleIcon, TimesCircleIcon, InProgressIcon, HourglassStartIcon} from '@patternfly/react-icons';

class MessagingInstances extends React.Component {

  constructor(props) {
    super(props);

    this.state = {
      page: 1,
      perPage: 5,
      columns: [{title: 'Name/Namespace'}, {title: 'Type'}, 'Status', 'Time created'],
      rows: [],
      actions: [
        {
          title: 'Delete',
          onClick: (event, rowId, rowData, extra) => {
            let name = this.state.rows[rowId].instanceName;
            let namespace = rowData.instanceNamespace;
            return this.openDeleteModal([{name, namespace}]);
          }
        }
      ],
      hasSelectedRows: false,
      isDeleteModalOpen: false,
      deleteInstances: [],
      allMessagingInstances: props.messagingInstances
    };
  };

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevProps.messagingInstances !== this.props.messagingInstances) {
      this.setState({allMessagingInstances: this.props.messagingInstances});
      this.updateRows(this.props.messagingInstances, this.state.page, this.state.perPage);
    }
  };

  getSelectedInstances = () => {
    let selectedItems = this.state.rows.filter(row => row.selected);
    return this.state.rows.filter(row => row.selected).map(row => {
      let name = row.instanceName;
      let namespace = row.instanceNamespace;
      return {name, namespace}
    });
  };

  onDeleteToggle = () => {
    this.setState(({isDeleteModalOpen: prevIsOpen}) => {
      return {isDeleteModalOpen: !prevIsOpen};
    });
  };

  handleDelete = (addNotification) => {
    this.state.deleteInstances.forEach(instance => {
      deleteMessagingInstances(instance.name, instance.namespace)
        .catch(error => {
          console.log('FAILED to delete name <' + instance.name + '> namespace <' + instance.namespace + '>', error);
          if (error.response) {
            addNotification('danger', 'Failed to delete '+ instance.name, error.response.data.message);
          } else {
            addNotification('danger', 'Failed to delete '+ instance.name);
          }
        });
    });
    this.reload();
    this.setState({deleteInstances: []});
    this.onDeleteToggle();
  };

  openDeleteModal = (instances) => {
    this.setState({'deleteInstances': instances});
    this.onDeleteToggle();
  }

  onSetPage = (_event, pageNumber) => {
    this.setState({
      page: pageNumber
    });
    this.updateRows(this.props.messagingInstances, pageNumber, this.state.perPage);
  };

  onPerPageSelect = (_event, perPage) => {
    this.setState({
      perPage: perPage
    });
    this.updateRows(this.props.messagingInstances, this.state.page, perPage);
  };

  updateRows(instances, page, perPage) {
    let currentSelectedInstances = this.getSelectedInstances();
    let visibleInstances = this.getVisibleMessagingInstances(instances, page, perPage);
    currentSelectedInstances.forEach(selectedInstance => {
      let instance = visibleInstances.find(instance => instance.name == selectedInstance.name && instance.namespace == selectedInstance.namespace);
      if (instance) {
        instance.selected = true;
      }
    });

    this.setState({rows: this.getMessagingInstanceCells(visibleInstances)});
  };

  getVisibleMessagingInstances(messagingInstances, page, perPage) {
    let instances = [...messagingInstances];
    let end = page * perPage;
    let start = end - perPage;
    let visibleInstances = instances.slice(start, Math.min(end, instances.length));
    return visibleInstances;
  };

  reload = () => {
    this.props.reloadMessagingInstances();
  };

  getMessagingInstanceCells(instances, page, perPage) {
    var styleOrange = {
      backgroundColor: '#FFA300',
      fontSize: 'var(--pf-c-table-cell--FontSize)',
      fontweight: 'var(--pf-c-table-cell--FontWeight)',
    };
    var nameFont = {
      lineHeight: '0.125em',
      fontSize: 'var(--pf-global--FontSize--md)'
    }
    var namespaceFont = {
      fontSize: 'var(--pf-global--FontSize--sm)'

    }
    //https://github.com/patternfly/patternfly-react/issues/1482 no verticle align
    if (instances) {
      let newMap = instances.map(instance => {
        let icon;

        if (instance.phase == 'Active') {
          icon = <CheckCircleIcon style={{color: 'var(--pf-global--success-color--100)'}}/>;
        } else if (instance.phase == 'Pending') {
          icon = <HourglassStartIcon />;
        } else if (instance.phase == 'Configuring') {
          icon = <InProgressIcon/>;
        } else {
          icon = <TimesCircleIcon style={{color: 'var(--pf-global--danger-color--100)'}}/>;
        }
        let status = <Aux>{(icon)} {instance.phase}</Aux>;

        let nameLink = (instance.isReady) ?
          <a style={nameFont} href={instance.consoleUrl}>{instance.name}</a> :
          <Text style={nameFont} >{instance.name}</Text>;

        let type = (instance.type == 'standard') ? 'Standard' : 'Brokered';
        return {
          cells: [
            {title: <Aux>{(nameLink)}<Text style={namespaceFont} >{instance.namespace}</Text></Aux>},
            {title: <Aux><Badge style={styleOrange}>{instance.component}</Badge>  {type}</Aux>},
            (status),
            <Aux>{moment(instance.timeCreated).fromNow()}</Aux>],
          instanceName: instance.name,
          instanceNamespace: instance.namespace,
          selected: instance.selected
        }
      });
      return newMap;
    }
    return [];
  };

  onMessagingInstanceSelect = (event, isSelected, rowId) => {
    let rows;
    if (rowId === -1) {
       rows = this.state.rows.map(row => {
        row.selected = isSelected;
        return row;
      });
    } else {

      rows = [...this.state.rows];
      rows[rowId].selected = isSelected;
    }
    this.setState({
      rows
    });
    this.setState({hasSelectedRows: rows.filter(row => row.selected).length !== 0});
  };

  render() {

    const {actions, columns, rows} = this.state;
    return (
      <React.Fragment>
        <NotificationConsumer>
          {({add}) => (
            <DeleteInstanceModal
              id="modal-delete"
              isOpen={this.state.isDeleteModalOpen}
              handleDeleteModalToggle={this.onDeleteToggle}
              handleDelete={this.handleDelete}
              deleteInstances={() => this.state.deleteInstances}
              addNotification={add}
            />
          )}
        </NotificationConsumer>
        <PageSection variant={PageSectionVariants.light}>
            <Toolbar className={"pf-u-justify-content-space-between pf-u-mx-xl pf-u-my-md"}>
              <ToolbarGroup>
                <ToolbarItem className={css(spacingStyles.mxMd)}>
                  <CreateAddressSpace reload={this.reload}/>
                </ToolbarItem>
                <ToolbarItem className={css(spacingStyles.mxMd)}>
                  <InstancesActionKebab
                    id="action-debab-top"
                    hasSelectedRows={this.state.hasSelectedRows}
                    openDeleteModal={() => this.openDeleteModal(this.getSelectedInstances())}
                  />
                </ToolbarItem>
              </ToolbarGroup>
              <ToolbarGroup>
                <ToolbarItem>
                  <Pagination
                    id="pagination-bottom-top"
                    itemCount={this.state.allMessagingInstances.length}
                    perPage={this.state.perPage}
                    page={this.state.page}
                    onSetPage={this.onSetPage}
                    widgetId="pagination-options-menu-top"
                    onPerPageSelect={this.onPerPageSelect}
                  />
                </ToolbarItem>
              </ToolbarGroup>
            </Toolbar>
            <Table id="table-instances" aria-label="table of messaging instances" onSelect={this.onMessagingInstanceSelect}
                   cells={columns} rows={rows} actions={actions}>
              <TableHeader id="table-header"/>
              <TableBody id="table-body"/>
            </Table>

            <Toolbar className={"pf-u-justify-content-flex-end pf-u-mx-xl pf-u-my-md"}>
              <ToolbarGroup>
                <ToolbarItem>
                  <Pagination
                    id="pagination-bottom"
                    itemCount={this.state.allMessagingInstances.length}
                    perPage={this.state.perPage}
                    page={this.state.page}
                    onSetPage={this.onSetPage}
                    widgetId="pagination-options-menu-bottom"
                    variant={PaginationVariant.bottom}
                    onPerPageSelect={this.onPerPageSelect}
                  />
                </ToolbarItem>
              </ToolbarGroup>
            </Toolbar>
        </PageSection>
      </React.Fragment>
    );
  }
};

export default MessagingInstances;

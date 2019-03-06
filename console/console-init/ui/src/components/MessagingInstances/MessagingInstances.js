import React from 'react';
import {
  Badge,
  Card, CardBody, CardHeader,
  Pagination, PaginationVariant,
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


import { CheckCircleIcon, TimesCircleIcon } from '@patternfly/react-icons';

class MessagingInstances extends React.Component {


  constructor(props) {
    super(props);

    this.state = {
      page: 1,
      perPage: 5,
      columns: [{title: 'Name/Namespace'}, {title: 'Type'}, 'Status', 'Time Created'],
      messagingInstances: [],
      rows: [],
      actions: [
        {
          title: 'Delete',
          onClick: (event, rowId, rowData, extra) => {
            let name = this.state.rows[rowId].instanceName;
            let name1 = rowData.cells[0].title.props.children[0].props.children
            let namespace = rowData.instanceNamespace;
            let namespace1 = rowData.cells[0].title.props.children[1].props.children;
            return this.openDeleteModal([{name, namespace}]);
          }
        }
      ],
      hasSelectedRows: false,
      isDeleteModalOpen: false,
      deleteInstances: []
    };
    this.onMessagingInstanceSelect = this.onMessagingInstanceSelect.bind(this);
  };

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevProps.messagingInstances !== this.props.messagingInstances) {
      this.setState({messagingInstances: this.props.messagingInstances});
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
            console.log(error.response.data);
            console.log(error.response.status);
            console.log(error.response.headers);
            addNotification('Failed to delete <' + instance.name + '>: ' + error.response.data.message, 'danger');
          } else {
            addNotification('Failed to delete <'+instance.name+'>', 'danger');
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
    let visibleInstances = this.getVisibleMessagingInstances(instances, page, perPage);
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
    //https://github.com/patternfly/patternfly-react/issues/1482 no verticle align
    if (instances) {
      let newMap = instances.map(instance => {
        let status = (instance.isReady) ? <Aux><CheckCircleIcon style={{color: 'green'}}/> Ready</Aux> : <Aux><TimesCircleIcon style={{color: 'red'}}/> Unavailable</Aux>;

        return {
        cells: [
          {title: <Aux><a href={instance.consoleUrl}>{instance.name}</a><Text>{instance.namespace}</Text></Aux>},
          {title: <Aux><Badge style={styleOrange}>{instance.component}</Badge> {instance.type}</Aux>},
          (status),
          <Aux>{moment(instance.timeCreated).fromNow()}</Aux>],
        instanceName: instance.name,
        instanceNamespace: instance.namespace
      }});
      return newMap;
    }
    return [];
  };

  onMessagingInstanceSelect(event, isSelected, rowId) {
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
      <Aux>
        <NotificationConsumer>
          {({add}) => (
            <DeleteInstanceModal
              isOpen={this.state.isDeleteModalOpen}
              handleDeleteModalToggle={this.onDeleteToggle}
              handleDelete={this.handleDelete}
              deleteInstances={() => this.state.deleteInstances}
              addNotification={add}
            />
          )}
        </NotificationConsumer>
        <Card>
          <CardHeader>
            <Toolbar className={"pf-u-justify-content-space-between pf-u-mx-xl pf-u-my-md"}>
              <ToolbarGroup>
                <ToolbarItem className={css(spacingStyles.mxMd)}>
                  <CreateAddressSpace reload={this.reload}/>
                </ToolbarItem>
                <ToolbarItem className={css(spacingStyles.mxMd)}>
                  <InstancesActionKebab
                    hasSelectedRows={this.state.hasSelectedRows}
                    openDeleteModal={() => this.openDeleteModal(this.getSelectedInstances())}
                  />
                </ToolbarItem>
              </ToolbarGroup>
              <ToolbarGroup>
                <ToolbarItem>
                  <Pagination
                    itemCount={this.state.messagingInstances.length}
                    perPage={this.state.perPage}
                    page={this.state.page}
                    onSetPage={this.onSetPage}
                    widgetId="pagination-options-menu-top"
                    onPerPageSelect={this.onPerPageSelect}
                  />
                </ToolbarItem>
              </ToolbarGroup>
            </Toolbar>
          </CardHeader>
          <CardBody>
            <Table aria-label="table of messaging instances" onSelect={this.onMessagingInstanceSelect}
                   cells={columns} rows={rows} actions={actions}>
              <TableHeader/>
              <TableBody/>
            </Table>

            <Toolbar className={"pf-u-justify-content-flex-end pf-u-mx-xl pf-u-my-md"}>
              <ToolbarGroup>
                <ToolbarItem>
                  <Pagination
                    itemCount={this.state.messagingInstances.length}
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
          </CardBody>
        </Card>
      </Aux>
    );
  }
};

export default MessagingInstances;

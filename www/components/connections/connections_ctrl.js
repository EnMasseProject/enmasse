angular.module('patternfly.toolbars').controller('ConnectionViewCtrl', ['$scope', '$timeout', 'pfViewUtils', 'address_service',
    function ($scope, $timeout, pfViewUtils, address_service) {

        var connectionGridConfig = function () {
          this.data = []
          this.columnDefs = [
            {field: 'name', displayName: 'Name'},
            {field: 'address', displayName: 'Address'},
            {field: 'deliveries', displayName: 'Deliveries', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'accepted', displayName: 'Accepted', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'rejected', displayName: 'Rejected', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'released', displayName: 'Released', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'modified', displayName: 'Modified', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'presettled', displayName: 'Presettled', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'undelivered', displayName: 'Undelivered', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'}
          ]
          this.enableHorizontalScrollbar = 0
          this.enableVerticalScrollbar = 0
          this.enableColumnMenus = false
        }

        $scope.getTableHeight = function(item, direction) {
          var rowHeight = 30;   // default row height
          var headerHeight = 30;
          return {
            height: (item[direction].length * rowHeight + headerHeight) + "px"
          };
        };

        var connectionGridConfigs = {}
        // make sure each item that has senders or receivers has a ui-grid config
        var ensureGridConfigs = function (items) {
            var getConfig = function (item, dir) {
                if (!connectionGridConfigs[item.host])
                  connectionGridConfigs[item.host] = {}
                if (!connectionGridConfigs[item.host][dir])
                  connectionGridConfigs[item.host][dir] = new connectionGridConfig()
              return connectionGridConfigs[item.host][dir]
            }
            items.forEach( function (item) {
              if (item.senders.length > 0) {
                item.senders_config = getConfig(item, 'senders')
                item.senders_config.data = item.senders
              }
              if (item.receivers.length > 0) {
                item.receivers_config = getConfig(item, 'receivers')
                item.receivers_config.data = item.receivers
              }
            })
        }

        address_service.on_update(function (reason) {
          if (reason.split('_')[0] !== 'connection') {
            return
          }
          ensureGridConfigs($scope.items)
          $timeout(() => {}) // safely apply any changes to scope variables
        });

        function get_filter_function(filter) {
            if (filter.id === 'host' || filter.id === 'container' || filter.id === 'user') {
                return function (item) {
                    return item[filter.id] && item[filter.id].match(filter.value) !== null;
                };
            } else if (filter.id === 'encrypted') {
                if (filter.value === 'encrypted') {
                    return function (item) { return item.encrypted; };
                } else if (filter.value === 'unencrypted') {
                    return function (item) { return !item.encrypted; };
                };
            } else {
                return function () {
                    console.log('unhandled filter: ' + JSON.stringify(filter));
                    return true;
                };
            }
        }

        function all(predicates) {
            return function (o) {
                return predicates.every(function (p) { return p(o); });
            }
        }

        // keep track of which items are filtered out (hidden)
        var hiddenItems = {}
        // called by pf-list-view directive to get disabled items
        // this sets the disabled class on our filtered items. a css rule hides disabled items
        var checkHiddenItem = function (item) {
          return hiddenItems[item.host]
        }
        var filterChange = function (filters) {
            hiddenItems = {}
            $scope.filtersText = filters.map(function (filter) { return  filter.title + " : " + filter.value + "\n"; }).join();
            var visibleItems = address_service.connections.filter(all(filters.map(get_filter_function)))
            // we can't set $scope.items to visiblItems since visibleItems is a filtered list which is actually a
            // separate array and would not get updated when the connection's data changes
            $scope.items.forEach( function (item) {
              if (!visibleItems.some ( (vis) => item.host === vis.host )) {
                hiddenItems[item.host] = true
              }
            })
            $scope.toolbarConfig.filterConfig.resultsCount = $scope.items.length - Object.keys(hiddenItems).length;
        };

        $scope.filtersText = '';
        $scope.items = address_service.connections;
        ensureGridConfigs($scope.items)
        $scope.filterConfig = {
            fields: [
                {
                    id: 'container',
                    title:  'Container',
                    placeholder: 'Filter by Container ID...',
                    filterType: 'text'
                },
                {
                    id: 'host',
                    title:  'Hostname',
                    placeholder: 'Filter by Hostname...',
                    filterType: 'text'
                },
                {
                    id: 'user',
                    title:  'User',
                    placeholder: 'Filter by username...',
                    filterType: 'text'
                },
                {
                    id: 'encrypted',
                    title:  'Encrypted',
                    placeholder: 'Filter by encrypted/unencrypted...',
                    filterType: 'select',
                    filterValues: ['encrypted', 'unencrypted']
                }
            ],
          resultsCount: $scope.items.length,
          appliedFilters: [],
          onFilterChange: filterChange
        };
        var compareFn = function(item1, item2) {
          var compValue = 0;
          if ($scope.sortConfig.currentField.id === 'container') {
            compValue = item1.container.localeCompare(item2.container);
          } else if ($scope.sortConfig.currentField.id === 'host') {
            compValue = item1.host.localeCompare(item2.host);
          } else if ($scope.sortConfig.currentField.id === 'senders') {
              compValue = item1.senders.length - item2.senders.length;
          } else if ($scope.sortConfig.currentField.id === 'receivers') {
              compValue = item1.receivers.length - item2.receivers.length;
          }

          if (!$scope.sortConfig.isAscending) {
            compValue = compValue * -1;
          }

          return compValue;
        };

        var sortChange = function (sortId, isAscending) {
          $scope.items.sort(compareFn);
        };

        $scope.sortConfig = {
          fields: [
            {
              id: 'container',
              title:  'Container ID',
              sortType: 'alpha'
            },
            {
              id: 'host',
              title:  'Hostname',
              sortType: 'alpha'
            },
            {
              id: 'senders',
              title:  'Senders',
              sortType: 'numeric'
            },
            {
              id: 'receivers',
              title:  'Receivers',
              sortType: 'numeric'
            }
          ],
          onSortChange: sortChange
        };

        $scope.toolbarConfig = {
          viewsConfig: $scope.viewsConfig,
          filterConfig: $scope.filterConfig,
          sortConfig: $scope.sortConfig,
          actionsConfig: $scope.actionsConfig
        };

        $scope.connectionListConfig = {
            showSelectBox: false,
            useExpandingRows: true,
            checkDisabled: false,
            checkDisabled: checkHiddenItem
        };
      }
    ]);

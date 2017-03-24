
function extended_tooltip (colour, rows) {
    var html = '<table class="c3-tooltip">';
    for (var i = 0; i < rows.length; i++) {
        if (i === 0) {
            html += '<tr><td><span style="background-color:' + colour + '"></span><strong>'
                + rows[i][0] + '</strong></td><td><strong>' + rows[i][1] + '</strong></td></tr>';
        } else {
            html += '<tr><td>' + rows[i][0] + '</td><td>' + rows[i][1] + '</td></tr>';
        }
    }
    html += '</table>';
    return html;
}

function get_tooltip_for_shard (address) {
    return function (datum, defaultTitleFormat, defaultValueFormat, colour) {
        var shard = address.shards[datum[0].index];
        var rows = [[shard.name, shard.messages]];
        if (!address.multicast) {
            return extended_tooltip(colour(datum[0]), rows.concat(['enqueued', 'acknowledged', 'expired', 'killed'].map(function (key) { return [key, shard[key]]; })));
        } else {
            return extended_tooltip(colour(datum[0]), rows);
        }
    };
}

function create_donut_chart(title, tooltip, size) {
    var chart = patternfly.c3ChartDefaults().getDefaultDonutConfig(title);
    chart.tooltip = {show: true};
    chart.tooltip.contents = tooltip || $().pfDonutTooltipContents;
    chart.size = size || { width: 250, height: 115 };
    return chart;
}

function get_donut_chart(address, property, title, tooltip, size) {
    var chart = address[property];
    if (chart === undefined) {
        chart = create_donut_chart(title, tooltip, size);
        address[property] = chart;
    }
    return chart;
}

function create_map(names, values) {
    var result = {};
    for (var i = 0; i < names.length; i++) {
        result[names[i]] = values[i];
    }
    return result;
}

function get_variations(basenames, suffixes) {
    return suffixes.map(function (suffix) {
        return basenames.map(function (basename) {
            return basename + suffix;
        });
    }).reduce(function (a, b) {
        return a.concat(b);
    });
}

var colours = get_variations(['blue', 'gold', 'red', 'green', 'purple', 'orange','lightBlue', 'lightGreen', 'cyan', 'black'], ['', '300', '100', '500', '200', '400']);

function get_colours(count) {
    if (count <= colours.length) {
        return colours.slice(0, count);
    } else {
        return colours.slice(0).concat(get_colours(count - colours.length));
    }
}

function get_colour_map(keys, palette) {
    return create_map(keys, palette ? get_colours(keys.length).map(function (c) { return palette[c]; }) : get_colours(keys.length));
}

function get_total(objects, property) {
    return objects.map(function (s) { return s[property]; }).reduce(function (a, b) { return a + b; });
}

angular.module('patternfly.toolbars').controller('ViewCtrl', ['$scope', 'pfViewUtils', 'address_service',
    function ($scope, pfViewUtils, address_service) {
        $scope.get_stored_chart_config = function (address) {
            var chart = get_donut_chart(address, 'shard_depth_chart', 'Stored', get_tooltip_for_shard(address));
            if (address.shards) {
                var shard_names = address.shards.map(function (s) { return s.name; });
                chart.data = {
                    type: "donut",
                    colors: get_colour_map(shard_names, $.pfPaletteColors),
                    columns: address.shards.map(function (s) { return [s.name, s.messages]; }),
                    groups: [shard_names],
                    order: null
                };
                //chart.title = address.depth;//doesn't work...
            }
            return chart;
        };

        $scope.get_outcomes_chart_config = function (address, direction) {
            var chart = get_donut_chart(address, direction + '_outcomes_chart', direction);
            var outcomes = address.outcomes[direction];
            var outcome_names = Object.keys(outcomes);
            chart.data = {
                type: "donut",
                colors: get_colour_map(outcome_names, $.pfPaletteColors),
                columns: outcome_names.map(function (name) { return [name, outcomes[name]]; }),
                groups: [outcome_names],
                order: null
            };
            return chart;
        };

        $scope.get_subscribers_chart_config = function (address) {
            var chart = get_donut_chart(address, 'subscribers_chart', 'Subscribers');
            var subscribers = get_total(address.shards, 'subscription_count');
            var durable = get_total(address.shards, 'durable_subscription_count');
            var inactive = get_total(address.shards, 'inactive_durable_subscription_count');
            chart.data = {
                type: "donut",
                colors: {'non-durable':$.pfPaletteColors['blue'], 'durable (active)': $.pfPaletteColors['orange'], 'durable (inactive)': $.pfPaletteColors['red']},
                columns: [['non-durable', subscribers - durable],['durable (active)', durable - inactive],['durable (inactive)', inactive]],
                groups: [['non-durable','durable (active)','durable (inactive)']],
                order: null
            };
            return chart;
        };

        address_service.on_update(function () { $scope.$apply(); });

        $scope.filtersText = '';
        $scope.items = address_service.addresses;

        var matchesFilter = function (item, filter) {
          var match = true;

          if (filter.id === 'address') {
              match = item.address.match(filter.value) !== null;
          } else if (filter.id === 'type') {
              if (filter.value === 'queue') {
                  match = item.store_and_forward && !item.multicast;
              } else if (filter.value === 'topic') {
                  match = item.store_and_forward && item.multicast;
              } else if (filter.value === 'multicast') {
                  match = !item.store_and_forward && item.multicast;
              } else if (filter.value === 'anycast') {
                  match = !item.store_and_forward && !item.multicast;
              }
          }
          return match;
        };

        var matchesFilters = function (item, filters) {
          var matches = true;

          filters.forEach(function(filter) {
            if (!matchesFilter(item, filter)) {
              matches = false;
              return false;
            }
          });
          return matches;
        };

        var applyFilters = function (filters) {
          $scope.items = [];
          if (filters && filters.length > 0) {
            address_service.addresses.forEach(function (item) {
              if (matchesFilters(item, filters)) {
                $scope.items.push(item);
              }
            });
          } else {
            $scope.items = address_service.addresses;
          }
        };

        var filterChange = function (filters) {
            $scope.filtersText = "";
            filters.forEach(function (filter) {
                $scope.filtersText += filter.title + " : " + filter.value + "\n";
            });
            applyFilters(filters);
            $scope.toolbarConfig.filterConfig.resultsCount = $scope.items.length;
        };

        $scope.filterConfig = {
          fields: [
            {
              id: 'address',
              title:  'Name',
              placeholder: 'Filter by Name...',
              filterType: 'text'
            },
            {
              id: 'type',
              title:  'Type',
              placeholder: 'Filter by Type...',
                filterType: 'select',
                filterValues: ['queue', 'topic', 'multicast', 'anycast']
            }
          ],
          resultsCount: $scope.items.length,
          appliedFilters: [],
          onFilterChange: filterChange
        };
        var compareFn = function(item1, item2) {
          var compValue = 0;
          if ($scope.sortConfig.currentField.id === 'address') {
            compValue = item1.address.localeCompare(item2.address);
          } else if ($scope.sortConfig.currentField.id === 'senders') {
              compValue = item1.senders - item2.senders;
          } else if ($scope.sortConfig.currentField.id === 'receivers') {
              compValue = item1.receivers - item2.receivers;
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
              id: 'address',
              title:  'Name',
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

        $scope.actionsText = "";
        var performAction = function (action) {
          $scope.actionsText = action.name + "\n" + $scope.actionsText;
        };

          $scope.delete_address = function (action) {
              address_service.delete_selected();
          };
          var suspend_address = function (action) {
              window.alert('Suspending not yet implemented!');
          };
          var purge_address = function (action) {
              window.alert('Purging not yet implemented!');
          };

          $scope.actionsConfig = {
          moreActions: [
              {
                  name: 'Suspend',
                  title: 'Suspend address',
                  actionFn: suspend_address
              },
              {
                  name: 'Purge',
                  title: 'Purge stored messages',
                  actionFn: purge_address
              }
          ],
            actionsInclude: true
        };

        $scope.toolbarConfig = {
          viewsConfig: $scope.viewsConfig,
          filterConfig: $scope.filterConfig,
          sortConfig: $scope.sortConfig,
          actionsConfig: $scope.actionsConfig
        };

        var handleSelectionChange = function () {
            var itemsSelected = $scope.addresses.find(function (item) {
                return item.selected;
            });
            console.log(itemsSelected + ' addresses selected');
            $scope.actionsConfig.primaryActions[0].isDisabled = !itemsSelected;
            $scope.actionsConfig.moreActions[0].isDisabled = !itemsSelected;
            $scope.actionsConfig.moreActions[1].isDisabled = !itemsSelected;
        };

        $scope.listConfig = {
            selectionMatchProp: 'address',
            onSelectionChange: handleSelectionChange,
            useExpandingRows: true,
            checkDisabled: false
        };
        $scope.exampleChartConfig = {
            'chartId': 'pctChart',
            'units': 'GB',
            'thresholds': {
                'warning':'60',
                'error':'90'
            }
        };
      }
    ]);

angular.module('patternfly.wizard').controller('WizardModalController', ['$scope', '$timeout', '$uibModal', '$rootScope',
   function ($scope, $timeout, $uibModal, $rootScope) {
        $scope.openWizardModel = function () {
            var wizardDoneListener;
            var modalInstance = $uibModal.open({
                animation: true,
                backdrop: 'static',
                templateUrl: 'address_wizard.html',
                controller: 'WizardController',
                size: 'lg'
            });

            var closeWizard = function (e, reason) {
                modalInstance.dismiss(reason);
                wizardDoneListener();
            };

            //modalInstance.result.then(function () { window.alert('all good!'); }, function () { window.alert('oops!'); });

            wizardDoneListener = $rootScope.$on('wizard.done', closeWizard);
        };
      }
]);

    angular.module('patternfly.wizard').controller('WizardController', ['$scope', '$timeout', '$rootScope', 'address_service',
        function ($scope, $timeout, $rootScope, address_service) {

        var initializeWizard = function () {
            $scope.data = {
                address: '',
                pattern: 'queue',
                flavor: address_service.flavors.filter(function (f) { return f.type === $scope.data.pattern; }),
                transactional: false,
                pooled: false
            };
            $scope.semantics_complete = false;
            $scope.valid_flavors = function () {
                return address_service.flavors.filter(function (f) { return f.type === $scope.data.pattern; });
            };

            $scope.updateName = function() {
                $scope.semantics_complete = angular.isDefined($scope.data.address) && $scope.data.address.length > 0 && address_service.is_unique_name($scope.data.address);
            };
            $scope.nextButtonTitle = "Next >";
        };

        var startDeploy = function () {
            $scope.deployInProgress = true;
        };

        $scope.data = {};

        $scope.nextCallback = function (step) {
            if (step.stepId === 'review') {
                address_service.create_address($scope.data);
            } else if (step.stepId === 'semantics') {
                var f = $scope.valid_flavors();
                if (f.length) {
                    $scope.data.flavor = $scope.valid_flavors()[0].name;
                    $scope.no_plan = false;
                } else {
                    $scope.data.flavor = undefined;
                    $scope.no_plan = true;
                }
                
            }
            return true;
        };

        $scope.backCallback = function (step) {
            return true;
        };

        $scope.$on("wizard:stepChanged", function (e, parameters) {
            if (parameters.step.stepId === 'review') {
                $scope.nextButtonTitle = "Create";
            } else {
                $scope.nextButtonTitle = "Next >";
            }
        });

        $scope.cancelDeploymentWizard = function () {
            $rootScope.$emit('wizard.done', 'cancel');
        };

        $scope.finishedWizard = function () {
            $rootScope.$emit('wizard.done', 'done');
            return true;
        };

        initializeWizard();
      }
    ]);

angular.module('patternfly.wizard').controller('SemanticsController', ['$rootScope', '$scope', 'address_service',
        function ($rootScope, $scope, address_service) {
            'use strict';
            $scope.isValidationDisabled = false;
            $scope.unique_address_name = function (input) {
                return address_service.is_unique_name(input);
            }
        }
    ]);

    angular.module('patternfly.wizard').controller('DetailsReviewController', ['$rootScope', '$scope',
      function ($rootScope, $scope) {
        'use strict';

        // Find the data!
        var next = $scope;
        while (angular.isUndefined($scope.data)) {
          next = next.$parent;
          if (angular.isUndefined(next)) {
            $scope.data = {};
          } else {
            $scope.data = next.wizardData;
          }
        }
      }
    ]);

    angular.module('patternfly.wizard').controller('SummaryController', ['$rootScope', '$scope', '$timeout',
      function ($rootScope, $scope, $timeout) {
        'use strict';
        $scope.pageShown = false;

        $scope.onShow = function () {
          $scope.pageShown = true;
          $timeout(function () {
            $scope.pageShown = false;  // done so the next time the page is shown it updates
          });
        }
      }
    ]);

angular.module('patternfly.toolbars').controller('ConnectionViewCtrl', ['$scope', 'pfViewUtils', 'address_service',
    function ($scope, pfViewUtils, address_service) {
        address_service.on_update(function () { $scope.$apply(); });

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

        var filterChange = function (filters) {
            $scope.filtersText = filters.map(function (filter) { return  filter.title + " : " + filter.value + "\n"; }).join();
            $scope.items = address_service.connections.filter(all(filters.map(get_filter_function)));
            $scope.toolbarConfig.filterConfig.resultsCount = $scope.items.length;
        };

        $scope.filtersText = '';
        $scope.items = address_service.connections;
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
            checkDisabled: false
        };
      }
    ]);

angular.module('myapp', ['patternfly.navigation', 'ui.router', 'patternfly.views', 'patternfly.toolbars', 'patternfly.charts', 'patternfly.wizard', 'patternfly.validation', 'address_service']).config(
    function ($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/addresses');
        $stateProvider.state('addresses',
                             { url: '/addresses',
                               templateUrl: 'addresses.html'
                             }
                            );
        $stateProvider.state('connections',
                             { url: '/connections',
                               templateUrl: 'connections.html'
                             }
                            );
        $stateProvider.state('users',
                             { url: '/users',
                               templateUrl: 'users.html'
                             }
                            );
    }).controller('NavCtrl', ['$scope',
    function ($scope) {
        $scope.navigationItems = [
            {
                title: "Addresses",
                iconClass: "fa pficon-topology",
                uiSref: "addresses"
            },
            {
                title: "Connections",
                iconClass : "fa pficon-route",
                uiSref: "connections",
            },
            {
                title: "Users",
                iconClass: "fa pficon-users",
                uiSref: "users"
            }
        ];
    }
]);

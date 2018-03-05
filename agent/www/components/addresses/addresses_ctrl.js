angular.module('patternfly.toolbars').controller('ViewCtrl', ['$scope', '$timeout', '$sce', '$templateCache', 'pfViewUtils', 'address_service',
    function ($scope, $timeout, $sce, $templateCache, pfViewUtils, address_service) {
        $scope.admin_disabled = address_service.admin_disabled;
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

        $scope.get_plan_display_name = function (type, plan) {
            return address_service.get_plan_display_name(type, plan);
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

        var lastLinkInfo = {}
        var saveLastLinkInfo = function (item) {
          lastLinkInfo[item.address] =
            {
              ingress: angular.copy(item.outcomes.ingress.links),
              egress: angular.copy(item.outcomes.egress.links)
            }
        }
        var calcRates = function (item) {
          var lastInfo = lastLinkInfo[item.address]
          if (!lastInfo) {
            saveLastLinkInfo(item)
            lastInfo = lastLinkInfo[item.address]
          }
          if (lastInfo) {
            var calc = function (iLinks, curLinks) {
              curLinks.forEach( function (curLink) {
                var matched = iLinks.some( function (iLink) {
                  if (iLink.routerName === curLink.routerName && iLink.identity === curLink.identity && iLink.name === curLink.name) {
                    var elapsed = curLink.lastUpdated - iLink.lastUpdated
                    var rate = iLink.deliveryRate || 0
                    if (elapsed) {
                      rate = (curLink.deliveryCount - iLink.deliveryCount) / (elapsed/1000)
                      rate = rate.toFixed(2)
                    }
                    if (isNaN(rate) || rate < 0)
                      rate = 0
                    curLink.deliveryRate = rate
                    return true
                  }
                  return false
                })
                if (!matched) {
                  curLink.deliveryRate = 0
                }
              })
            }
            calc(lastInfo.ingress, item.outcomes.ingress.links)
            calc(lastInfo.egress, item.outcomes.egress.links)
          }
        }
        // construct the html tooltop for a link details row
        $scope.linkTooltip = function (row) {
          var tipHtml = "<table>"
          Object.keys(row).forEach( function (key) {
            if (['$$hashKey', 'lastUpdated', 'backlog', 'name', 'deliveryRate', 'routerName'].indexOf(key) == -1) {
              tipHtml += "<tr><td>" + key + "&nbsp;</td><td align='right'>" + (isNaN(row[key]) ? row[key] : row[key].toLocaleString()) + "</td></tr>"
            }
          })
          tipHtml += "</table>"
          return tipHtml
        }
        // each address that has senders and/or receivers gets a link detail table
        var linkTableConfig = function () {
          this.data = []
          this.columnDefs = [
            {field: 'routerName', displayName: 'Id'},
            {field: 'deliveryRate', displayName: 'Delivery Rate', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
            {field: 'backlog', displayName: 'Backlog', cellClass: 'text-right', headerCellClass: 'ui-grid-cell-right-align'},
          ]
          this.enableHorizontalScrollbar = 0
          this.enableVerticalScrollbar = 0
          this.enableColumnMenus = false
          this.rowTemplate = "link-grid-row.html"
          this.minRowsToShow = 0
        }
        var rateTimer = null
        var forceDeliveryRateUpdate = function () {
            rateTimer = null
            $timeout( function () {
              $scope.items.forEach( function (item) {
                calcRates(item)
              })
            })
        }

        var on_update = function (reason) {
          if ($scope.filterConfig) {
            $scope.filterConfig.resultsCount = $scope.items.length;
          }
          if (reason.split('_')[0] !== 'address') {
            return
          }
            $scope.admin_disabled = address_service.admin_disabled;
          $scope.items.forEach( function (item) {
            if (item.senders + item.receivers > 0) {
              if (!item.ingress_outcomes_link_table) {
                item.ingress_outcomes_link_table = new linkTableConfig()
                item.egress_outcomes_link_table = new linkTableConfig()
              }
              calcRates(item)
              saveLastLinkInfo(item)

              // needed to force the grid to update the data
              item.ingress_outcomes_link_table.data = item.outcomes.ingress.links
              item.egress_outcomes_link_table.data = item.outcomes.egress.links
            }
          })
          $timeout(() => {}) // safely apply any changes to scope variables
          // force a rate updated even if there was no change in the address
          if (rateTimer)
            clearTimeout(rateTimer)
          rateTimer = setTimeout(forceDeliveryRateUpdate, 7500)
        };
        address_service.on_update(on_update);

        $scope.getTableHeight = function(item, xgress) {
          var rowHeight = 30;   // default row height
          var headerHeight = 30;
          return {
            height: (item.outcomes[xgress].links.length * rowHeight + headerHeight) + "px"
          };
        };

        $scope.filtersText = '';
        $scope.items = address_service.addresses;
        on_update("address")

        var matchesFilter = function (item, filter) {
          var match = true;

          if (filter.id === 'address') {
              match = item.address.match(filter.value) !== null;
          } else if (filter.id === 'type') {
              match = item.type.match(filter.value) !== null;
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
            if ($scope.admin_disabled) {
              window.alert('Delete disabled!');
            } else {
                address_service.delete_selected();
            }
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
                  isDisabled: true,
                  actionFn: suspend_address
              },
              {
                  name: 'Purge',
                  title: 'Purge stored messages',
                  isDisabled: true,
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

        $scope.$on('$destroy', function() {
            if (rateTimer) {
                clearTimeout( rateTimer )
                rateTimer = null
            }
        });
      }
    ]);

angular.module('patternfly.wizard').controller('WizardModalController', ['$scope', '$timeout', '$uibModal', '$rootScope',
   function ($scope, $timeout, $uibModal, $rootScope) {
        $scope.openWizardModel = function () {
            var wizardDoneListener;
            var modalInstance = $uibModal.open({
                animation: true,
                backdrop: 'static',
                templateUrl: 'components/addresses/address_wizard.html',
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
                type: '',
                plan: ''
            };
            $scope.semantics_complete = false;
            $scope.valid_plans = function () {
                return address_service.get_valid_plans($scope.data.type);
            };
            $scope.valid_address_types = function () {
                return address_service.get_valid_address_types();
            };

            $scope.updateName = function() {
                $scope.semantics_complete = angular.isDefined($scope.data.address) && $scope.data.address.length > 0 && address_service.is_unique_name($scope.data.address)
                    && $scope.data.type.length > 0;
            };
            $scope.nextButtonTitle = "Next >";

            $scope.tooltip = address_service.tooltip
            var space = address_service.tooltip[address_service.address_space_type];
            if (space) {
                for (var key in space.address) {
                    $scope.tooltip.address[key] = space.address[key];
                }
            }
            $scope.getTooltip = function (addr) {
              return $scope.tooltip.address[addr].longDescription
            }
            $scope.getLabel = function (addr) {
              return $scope.tooltip.address[addr].shortDescription
            }
            $scope.hasExternal = function (addr) {
              return angular.isDefined($scope.tooltip.address[addr].external)
            }
            $scope.hasTooltip = function (addr) {
              return angular.isDefined($scope.tooltip.address[addr].longDescription)
            }
            $scope.getExternal = function (addr) {
              return $scope.tooltip.address[addr].external
            }
        };

        var startDeploy = function () {
            $scope.deployInProgress = true;
        };

        $scope.data = {};

        $scope.nextCallback = function (step) {
            if (step.stepId === 'review') {
                address_service.create_address($scope.data);
            } else if (step.stepId === 'semantics') {
                var f = $scope.valid_plans();
                if (f && f.length) {
                    $scope.data.plan = $scope.valid_plans()[0].name;
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


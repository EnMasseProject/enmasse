angular.module('patternfly.toolbars').controller('ViewCtrl', ['$scope', '$timeout', '$sce', '$templateCache', 'pfViewUtils', 'address_service',
    function ($scope, $timeout, $sce, $templateCache, pfViewUtils, address_service) {

        $scope.clickNavigationItem("Addresses");
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

        $scope.notification = {show_alert:false, alert_msg:''};
        $scope.removeNotification=function() {
            $scope.notification.show_alert=false;
          };

        address_service.add_additional_listener(function(reason) {
            if (reason === 'request_error' && address_service.error_queue.length > 0) {
                var msg = '';
                while(address_service.error_queue.length > 0) {
                    msg += address_service.error_queue.pop();
                }
                $scope.$apply(() => {
                    $scope.notification.alert_msg = msg;
                    $scope.notification.show_alert = true;
                });
            }
        });

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

        $scope.get_address_space_type = function () {
            return address_service.address_space_type;
        }

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
            if (item.outcomes) {
                var record = {}
                if (item.outcomes.ingress) {
                    record.ingress = angular.copy(item.outcomes.ingress.links)
                }
                if (item.outcomes.egress) {
                    record.egress = angular.copy(item.outcomes.egress.links)
                }
                lastLinkInfo[item.address] = record
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
            if (item.outcomes) {
              if (item.outcomes.ingress) calc(lastInfo.ingress, item.outcomes.ingress.links)
              if (item.outcomes.egress) calc(lastInfo.egress, item.outcomes.egress.links)
            }
          }
        }
        // construct the html tooltip for a link details row
        $scope.linkTooltip = function (row) {
          var tipHtml = "<table>"
          Object.keys(row).forEach( function (key) {
            if (['$$hashKey', 'lastUpdated', 'backlog', 'name', 'deliveryRate', 'clientName'  ].indexOf(key) == -1) {
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
            {field: 'clientName', displayName: 'Container ID', cellTemplate:'<div class="ui-grid-cell-contents"><a ng-href="#/connections?containerId={{row.entity.clientName}}">{{row.entity.clientName}}</a></div>' },
            {field: 'name', displayName: 'Name'},
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
          var params = reason.split('_');
          if (params[0] !== 'address') {
            return
          }
          if (params[1] === 'added' || params[1] === 'deleted') {
            applyFilters();
            $scope.items.sort(compareFn);
          }
          $scope.admin_disabled = address_service.admin_disabled;
          $scope.setUser(address_service.user);
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
          rateTimer = setTimeout(forceDeliveryRateUpdate, 7500);
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

        var applyFilters = function () {
            var filters = $scope.filterConfig.appliedFilters;
            if (filters && filters.length > 0) {
                $scope.items = address_service.addresses.filter(function (item) {
                    return matchesFilters(item, filters);
                });
            } else {
                $scope.items = address_service.addresses;
            }
            $scope.toolbarConfig.filterConfig.resultsCount = $scope.items.length;
        };

        var filterChange = function (filters) {
            console.log('filters changed');
            $scope.filtersText = "";

            $scope.notification.show_alert = false;
            var messages = [];
            var valid = filters.filter(function (filter) {
                if (filter.title === 'Name') {
                    try {
                        var dummy = new RegExp(filter.value);
                        return true;
                    } catch (error) {
                        messages.push('"' + filter.value + '": is not valid.  Only regular expressions are supported.');
                        return false;
                    }
                } else {
                    return true;
                }
            });

            if (messages.length > 0) {
                $scope.notification.show_alert = true;
                $scope.notification.alert_msg = messages.join(" ");
            }

            valid.forEach(function (filter) {
                $scope.filtersText += filter.title + " : " + filter.value + "\n";
            });
            $scope.filterConfig.appliedFilters = valid;
            applyFilters();
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
                filterValues: ['queue', 'topic', 'multicast', 'anycast', 'subscription']
            }
          ],
          resultsCount: $scope.items.length,
          appliedFilters: [],
          onFilterChange: filterChange
        };
        var compareFn = function(item1, item2) {
          var compValue = 0;
          if ($scope.sortConfig.currentField === undefined || $scope.sortConfig.currentField.id === 'address') {
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
        $scope.selected_queues=[];
        $scope.cancel_purge = function (action) {
            $("#purge-confirmation-modal").modal('toggle');
        };
        $scope.purge_address = function (action) {
            if ($scope.admin_disabled) {
                window.alert('Purge disabled!');
            } else {
                $scope.selected_queues = address_service.get_selected_address_names();
                $("#purge-confirmation-modal").modal('toggle');
            }
        };
        $scope.do_purge = function (action) {
            $("#purge-confirmation-modal").modal('toggle');
            if ($scope.admin_disabled) {
                window.alert('Purge disabled!');
            } else {
                address_service.purge_selected();
            }
        };

        $scope.actionsConfig = {
          moreActions: [
              {
                  name: 'Purge',
                  title: 'Purge stored messages',
                  isDisabled: true,
                  actionFn: $scope.purge_address
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


        $scope.handleSelectionChange = function (item) {
            var itemsSelected = $scope.items.find(function (item) {
                return item.selected;
            });
            var onlyQueuesSelected = $scope.items.every(function (item) {
                return !item.selected || ['queue', 'subscription'].includes(item.type);
            });
            $scope.actionsConfig.moreActions[0].isDisabled = !(onlyQueuesSelected && itemsSelected);
        };
        $scope.listConfig = {

            selectionMatchProp: 'address',
            onCheckBoxChange: $scope.handleSelectionChange,
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
        on_update("address_added")
      }
    ]);

angular.module('patternfly.modals').controller('PeerLostController', ['$scope', '$uibModal', 'address_service',
    function ($scope, $uibModal, address_service) {

        $scope.openWizardModel = function (templateUrl) {
            $scope.modalInstance = $uibModal.open({
                animation: true,
                backdrop: 'static',
                templateUrl: templateUrl,
                controller: function ($scope, $uibModalInstance) {
                },
                size: 'lg'
            });
        };

        address_service.add_additional_listener(function(reason) {
            if (reason === 'peer_disconnected') {
                if (!$scope.modalInstance) {
                    var code = address_service.closeEvent && address_service.closeEvent.code ? address_service.closeEvent.code : -1;
                    console.log("disconnected - close event code : %d", code);
                    if (code === 4403) {
                        $scope.openWizardModel('/forbidden_list_address.html');
                    } else {
                        $scope.openWizardModel('/peer_lost.html');
                    }
                }
            } else if (reason === 'peer_connected') {
                if ($scope.modalInstance) {
                    console.log("connected");
                    $scope.modalInstance.close();
                    $scope.modalInstance = null;
                }
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
            $scope.list_topic_names = function () {
                return address_service.list_topic_names();
            };

            $scope.updateName = function() {
                $scope.semantics_complete = angular.isDefined($scope.data.address) && $scope.data.address.length > 0 && address_service.is_unique_valid_name($scope.data.address)
                    && $scope.data.type.length > 0 && ($scope.data.type !== 'subscription' || ($scope.data.topic && $scope.data.topic.length));
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
              return $scope.tooltip.address[addr] ? $scope.tooltip.address[addr].shortDescription : addr;
            }
            $scope.hasExternal = function (addr) {
              return $scope.tooltip.address[addr] && angular.isDefined($scope.tooltip.address[addr].external)
            }
            $scope.hasTooltip = function (addr) {
              return $scope.tooltip.address[addr] && angular.isDefined($scope.tooltip.address[addr].longDescription)
            }
            $scope.getExternal = function (addr) {
              return $scope.tooltip.address[addr].external
            }
        };

        var startDeploy = function () {
            $scope.deployInProgress = true;
        };

        $scope.data = {};


        function hasItemWithName(name, list) {
            if (!name) return false;
            var matched = list.filter(function (item) { return item.name === name; });
            return matched && matched.length;
        }

        function adjustForType() {
            var f = $scope.valid_plans();
            if (f && f.length) {
                if (!hasItemWithName($scope.data.plan, f)) {
                    $scope.data.plan = $scope.valid_plans()[0].name;
                }
            }
            if ($scope.data.type !== 'subscription') {
                $scope.data.topic = undefined;
            }
        }

        $scope.nextCallback = function (step) {
            if (step.stepId === 'review') {
                address_service.create_address($scope.data);
            } else if (step.stepId === 'semantics') {
                adjustForType();
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
            if (parameters.step.stepId !== 'semantics') {
                adjustForType();
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
            $scope.valid_address_name = function (input) {
                return address_service.is_unique_valid_name(input);
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

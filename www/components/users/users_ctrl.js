angular.module('patternfly.toolbars').controller('UserViewCtrl', ['$scope', 'pfViewUtils', 'address_service',
    function ($scope, pfViewUtils, address_service) {
        address_service.on_update(function () { $scope.$apply(); });

        function get_filter_function(filter) {
            if (filter.id === 'name') {
                return function (item) {
                    return item[filter.id] && item[filter.id].match(filter.value) !== null;
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
            $scope.items = address_service.users.filter(all(filters.map(get_filter_function)));
            $scope.toolbarConfig.filterConfig.resultsCount = $scope.items.length;
        };

        $scope.filtersText = '';
        $scope.items = address_service.users;
        $scope.filterConfig = {
            fields: [
                {
                    id: 'name',
                    title:  'User',
                    placeholder: 'Filter by username...',
                    filterType: 'text'
                }
            ],
          resultsCount: $scope.items.length,
          appliedFilters: [],
          onFilterChange: filterChange
        };
        var compareFn = function(item1, item2) {
          var compValue = 0;
          if ($scope.sortConfig.currentField.id === 'name') {
            compValue = item1.name.localeCompare(item2.name);
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
              id: 'name',
              title:  'User ID',
              sortType: 'alpha'
            }
          ],
          onSortChange: sortChange
        };
        $scope.delete_user = function () {
              address_service.delete_selected_users();
        };
        $scope.actionsConfig = {
            actionsInclude: true
        };
        $scope.toolbarConfig = {
          viewsConfig: $scope.viewsConfig,
          filterConfig: $scope.filterConfig,
          sortConfig: $scope.sortConfig,
          actionsConfig: $scope.actionsConfig
        };

        $scope.userListConfig = {
            showSelectBox: true,
            useExpandingRows: true,
            checkDisabled: false
        };
      }
    ]);

angular.module('patternfly.wizard').controller('UserWizardModalController', ['$scope', '$timeout', '$uibModal', '$rootScope',
   function ($scope, $timeout, $uibModal, $rootScope) {
        $scope.openUserWizard = function () {
            var wizardDoneListener;
            var modalInstance = $uibModal.open({
                animation: true,
                backdrop: 'static',
                templateUrl: 'components/users/user_wizard.html',
                controller: 'UserWizardController',
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

angular.module('patternfly.wizard').controller('UserWizardController', ['$scope', '$timeout', '$rootScope', 'address_service',
    function ($scope, $timeout, $rootScope, address_service) {

        $scope.isValidationDisabled = false;
        $scope.unique_user_name = function (input) {
            return true;//address_service.is_unique_user(input);
        }
        var initializeWizard = function () {
            $scope.data = {
                name: '',
                password: ''
            };
            $scope.data_complete = false;

            $scope.updateName = function() {
                $scope.data_complete = angular.isDefined($scope.data.name) && $scope.data.name.length > 0 && angular.isDefined($scope.data.password) && $scope.data.password.length > 0;
            };
            $scope.nextButtonTitle = "Create";
        };

        var startDeploy = function () {
            $scope.deployInProgress = true;
        };

        $scope.data = {};

        $scope.nextCallback = function (step) {
            address_service.create_user($scope.data);
            return true;
        };

        $scope.backCallback = function (step) {
            return true;
        };

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

angular.module('patternfly.wizard').controller('UserFormController', ['$rootScope', '$scope', 'address_service',
        function ($rootScope, $scope, address_service) {
            'use strict';
            $scope.isValidationDisabled = false;
            $scope.unique_user_name = function (input) {
                return true;//address_service.is_unique_user(input);
            }
        }
    ]);

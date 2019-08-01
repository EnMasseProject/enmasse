angular.module('enmasse', ['patternfly.navigation', 'ui.router', 'patternfly.views', 'ui.grid', 'ui.grid.autoResize',
        'ui.grid.resizeColumns', 'ui.bootstrap', 'patternfly.toolbars', 'patternfly.charts', 'patternfly.wizard',
        'patternfly.validation', 'patternfly.modals', 'address_service']).config(
    function ($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/addresses');
        $stateProvider.state('addresses',
                             { url: '/addresses',
                               templateUrl: 'components/addresses/addresses.html'
                             }
                            );
        $stateProvider.state('connections',
                             { url: '/connections?containerId',
                               templateUrl: 'components/connections/connections.html'
                             }
                            );
    }).controller('NavCtrl', ['$scope', '$timeout',
    function ($scope, $timeout) {
        $scope.user = "<unknown>";
        $scope.navigationItems = [
            {
                title: "Addresses",
                uiSref: "addresses",
            },
            {
                title: "Connections",
                uiSref: "connections",
            }
        ];
        $scope.clickNavigationItem = function (title) {
            $timeout(function() {
                angular.forEach(angular.element("li.list-group-item"), (element) => {
                    if (element.innerText === title) {
                        element.children[0].click()
                    }
                });
            }, 0);
        };
        $scope.setUser = function (user) {
            $scope.user = user;
        };
    }
]);

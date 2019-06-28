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
            var connectionsNavItem = $scope.navigationItems.find(item => item.title === title);
            $timeout(function() {
                //If already navigated to the page, the click will only mark the menu item as active.
                angular.element(selector).parent().click();
            }, 0);
        };
        $scope.setUser = function (user) {
            $scope.user = user;
        };
    }
]);

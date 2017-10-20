angular.module('enmasse', ['patternfly.navigation', 'ui.router', 'patternfly.views', 'ui.grid', 'ui.grid.autoResize',
        'ui.grid.resizeColumns', 'ui.bootstrap', 'patternfly.toolbars', 'patternfly.charts', 'patternfly.wizard',
        'patternfly.validation', 'address_service']).config(
    function ($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/dashboard');
        $stateProvider.state('dashboard',
                             { url: '/dashboard',
                               templateUrl: 'components/dashboard/dashboard.html'
                             }
                            );
        $stateProvider.state('addresses',
                             { url: '/addresses',
                               templateUrl: 'components/addresses/addresses.html'
                             }
                            );
        $stateProvider.state('connections',
                             { url: '/connections',
                               templateUrl: 'components/connections/connections.html'
                             }
                            );
    }).controller('NavCtrl', ['$scope',
    function ($scope) {
        $scope.navigationItems = [
            {
                title: "Dashboard",
                iconClass: "fa fa-tachometer",
                uiSref: "dashboard"
            },
            {
                title: "Addresses",
                iconClass: "fa pficon-topology",
                uiSref: "addresses"
            },
            {
                title: "Connections",
                iconClass : "fa pficon-route",
                uiSref: "connections",
            }
        ];
    }
]);

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
            }
        ];
    }
]);

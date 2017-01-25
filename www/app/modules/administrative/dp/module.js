angular.module('os.administrative.dp', 
  [ 
    'ui.router',
    'os.administrative.dp.list',
    'os.administrative.dp.detail',
    'os.administrative.dp.addedit',
    'os.administrative.dp.history',
    'os.administrative.dp.requirement',
    'os.administrative.dp.consents'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('dp-list', {
        url: '/dps',
        templateUrl: 'modules/administrative/dp/list.html',     
        controller: 'DpListCtrl',
        parent: 'signed-in'
      })
      .state('dp-addedit', {
        url: '/dp-addedit/:dpId',
        templateUrl: 'modules/administrative/dp/addedit.html',
        resolve: {
          distributionProtocol: function($stateParams , DistributionProtocol) {
            if ($stateParams.dpId) {
              return DistributionProtocol.getById($stateParams.dpId);
            }
            return new DistributionProtocol();
          },
          extensionCtxt: function(DistributionProtocol) {
            return DistributionProtocol.getExtensionCtxt();
          }
        },
        controller: 'DpAddEditCtrl',
        parent: 'signed-in'
      })
      .state('dp-detail', {
        url: '/dps/:dpId',
        templateUrl: 'modules/administrative/dp/detail.html',
        resolve: {
          distributionProtocol: function($stateParams , DistributionProtocol) {
            return DistributionProtocol.getById($stateParams.dpId);
          }
        },
        controller: 'DpDetailCtrl',
        parent: 'signed-in'
      })
      .state('dp-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/dp/overview.html',
        parent: 'dp-detail'
      })
      .state('dp-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/administrative/dp/consents.html',
        parent: 'dp-detail',
        resolve: {
          consentTiers: function(distributionProtocol) {
            return distributionProtocol.getConsentTiers();
          }
        },
        controller: 'DpConsentsCtrl'
      })
      .state('dp-detail.history', {
        url: '/history',
        templateUrl: 'modules/administrative/dp/history.html',
        controller: 'DpHistoryCtrl',
        parent: 'dp-detail'
      });
  });

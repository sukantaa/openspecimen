angular.module('os.administrative.order', 
  [ 
    'ui.router',
    'os.administrative.order.list',
    'os.administrative.order.detail',
    'os.administrative.order.addedit',
    'os.administrative.order.returnspecimens'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('order-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope) {
          // Storage Container Authorization Options
          $scope.orderResource = {
            createOpts: {resource: 'Order', operations: ['Create']},
            updateOpts: {resource: 'Order', operations: ['Update']},
            deleteOpts: {resource: 'Order', operations: ['Delete']},
            importOpts: {resource: 'Order', operations: ['Bulk Import']}
          }
        },
        parent: 'signed-in'
      })
      .state('order-list', {
        url: '/orders?filters',
        templateUrl: 'modules/administrative/order/list.html',     
        controller: 'OrderListCtrl',
        parent: 'order-root'
      })
      .state('order-addedit', {
        url: '/order-addedit/:orderId?requestId&specimenListId',
        templateUrl: 'modules/administrative/order/addedit.html',
        controller: 'OrderAddEditCtrl',
        resolve: {
          specimenList: function($stateParams, SpecimenList) {
            if ($stateParams.specimenListId) {
              return SpecimenList.getById($stateParams.specimenListId);
            }

            return null;
          },

          order: function($stateParams, specimenList, DistributionOrder) {
            if ($stateParams.orderId) {
              return DistributionOrder.getById($stateParams.orderId);
            }

            return new DistributionOrder({status: 'PENDING', orderItems: [], specimenList: specimenList});
          },

          spmnRequest: function($stateParams, $injector, order) {
            var catalog;
            if ($injector.has('scCatalog')) {
              var scCatalog = $injector.get('scCatalog');
              catalog = new scCatalog({id: -1});
            }

            if (!catalog) {
              return null;
            }

            var reqId = undefined;
            if (angular.isDefined(order.id)) {
              reqId = !!order.request ? order.request.id : undefined;
            } else if (angular.isDefined($stateParams.requestId)) {
              reqId = $stateParams.requestId;
            }

            return !reqId ? null : catalog.getRequest(reqId);
          },

          requestDp: function(spmnRequest, DistributionProtocol) {
            if (spmnRequest && spmnRequest.dpId) {
              return DistributionProtocol.getById(spmnRequest.dpId);
            }

            return null;
          }
        },
        parent: 'order-root'
      })
      .state('order-import', {
        url: '/orders-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'order-list', title: 'orders.list'}],
              objectType: 'distributionOrder',
              csvType: 'MULTIPLE_ROWS_PER_OBJ',
              title: 'orders.bulk_import',
              onSuccess: {state: 'order-list'}
            };
          }
        },
        parent: 'signed-in'
      })
      .state('order-import-jobs', {
        url: '/orders-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'order-list', title: 'orders.list'}],
              title: 'orders.bulk_import_jobs',
              objectTypes: ['distributionOrder']
            };
          }
        },
        parent: 'signed-in'
      })
      .state('order-detail', {
        url: '/orders/:orderId',
        templateUrl: 'modules/administrative/order/detail.html',
        controller: 'OrderDetailCtrl',
        resolve: {
          order: function($stateParams , DistributionOrder) {
            return DistributionOrder.getById($stateParams.orderId);
          }
        },
        parent: 'order-root'
      })
      .state('order-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/order/overview.html',
        parent: 'order-detail'
      })
      .state('order-detail.items', {
        url: '/items',
        templateUrl: 'modules/administrative/order/items.html',
        controller: 'OrderItemsCtrl',
        parent: 'order-detail'
      })
      .state('order-return-specimens', {
        url: '/return-specimens',
        templateUrl: 'modules/administrative/order/return-specimens.html',
        controller: 'OrderReturnSpecimensCtrl',
        resolve: {
          barcodingEnabled: function(CollectionProtocol) {
            return CollectionProtocol.getBarcodingEnabled();
          }
        },
        parent: 'order-root'
      });
  }).run(function(UrlResolver) {
    UrlResolver.regUrlState('order-overview', 'order-detail.overview', 'orderId');
  });;

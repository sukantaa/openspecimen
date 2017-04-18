angular.module('os.administrative.container')
  .controller('ContainerSpecimensCtrl', function($scope, container, Util, CollectionProtocol, Container) {
    function init() {
      $scope.ctx.showTree = true;
      $scope.ctx.viewState = 'container-detail.specimens';

      $scope.lctx = {
        filterOpts: {},
        specimens: [],
        cps: [],
        containers: []
      };

      loadSpecimens($scope.lctx.filterOpts);
      Util.filter($scope, 'lctx.filterOpts', loadSpecimens);
    }

    function loadSpecimens(filterOpts) {
      container.getSpecimens(filterOpts).then(
        function(specimens) {
          $scope.lctx.specimens = specimens;
        }
      );
    }

    function loadCps(shortTitle) {
      var params = {query: shortTitle, repositoryName: container.siteName};
      CollectionProtocol.list(params).then(
        function(cps) {
          $scope.lctx.cps = cps;
        }
      );
    }

    function loadContainers(name) {
      container.getDescendantContainers({name: name}).then(
        function(containers) {
          $scope.lctx.containers = containers;
        }
      );
    }

    $scope.toggleSearch = function() {
      $scope.ctx.showTree = !$scope.ctx.showTree;
    }

    $scope.downloadReport = function() {
      Util.downloadReport(container, "container.specimens");
    }

    $scope.loadCps = loadCps;

    $scope.loadContainers = loadContainers;

    init();
  });

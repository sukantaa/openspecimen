
angular.module('os.administrative.user.detail', ['os.administrative.models'])
  .controller('UserDetailCtrl', function($scope, $q, $state, $modal, $translate, user, User, PvManager, Alerts) {
    $scope.user = user;
    $scope.domains = PvManager.getPvs('domains');
    $scope.sites = PvManager.getSites();

    $scope.editUser = function(property, value) {
      var d = $q.defer();
      d.resolve({});
      return d.promise;
    }

    $scope.activate = function() {
      var user = angular.copy($scope.user);
      User.activate(user.id).then(
        function(user) {
          $scope.user = user;
          $translate('user.user_request_approved').then(function(msg) {
            Alerts.success(msg);
          })
        }
      );
    }

    $scope.deleteUser = function() {
      var modalInstance = $modal.open({
        templateUrl: 'modules/common/delete/delete-entity-template.html',
        controller: 'entityDeleteCtrl',
        resolve: {
          entityProps: function() {
            return {
              entity: $scope.user,
              name: $scope.user.firstName + ' ' + $scope.user.lastName,
            }
          },
          entityDependencyStat: function() {
            return User.getDependencyStat($scope.user.id);
          }
        }
      });

      modalInstance.result.then(function () {
        $state.go('user-list');
      });
    }
  });

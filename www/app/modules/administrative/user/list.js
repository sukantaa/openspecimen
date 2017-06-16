
angular.module('os.administrative.user.list', ['os.administrative.models'])
  .controller('UserListCtrl', function(
    $scope, $state, $modal, currentUser,
    osRightDrawerSvc, Institute, User, ItemsHolder, PvManager,
    Util, DeleteUtil, CheckList, Alerts, ListPagerOpts) {

    var pagerOpts;
    var pvInit = false;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getUsersCount});
      $scope.ctx = {
        exportDetail: {objectType: 'user'}
      };
      initPvsAndFilterOpts();
      loadUsers($scope.userFilterOpts);
      ItemsHolder.setItems('users', undefined);
    }
  
    function initPvsAndFilterOpts() {
      $scope.userFilterOpts = {includeStats: true, maxResults: pagerOpts.recordsPerPage + 1};
      $scope.$on('osRightDrawerOpen', function() {
        if (pvInit) {
          return;
        }

        loadActivityStatuses();
        loadInstitutes().then(
          function(institutes) {
            if (institutes.length == 1) {
              $scope.userFilterOpts.institute = institutes[0].name;
            }

            Util.filter($scope, 'userFilterOpts', loadUsers);
          }
        );

        pvInit = true;
      });
    }
   
    function loadActivityStatuses() {
      PvManager.loadPvs('activity-status').then(
        function(result) {
          $scope.activityStatuses = [].concat(result);
          $scope.activityStatuses.push('Locked');
          var idx = $scope.activityStatuses.indexOf('Disabled');
          if (idx != -1) {
            $scope.activityStatuses.splice(idx, 1);
          }
        }
      );
    }

    function loadInstitutes() {
      var q = undefined;
      if (currentUser.admin) {
        q = Institute.query();
      } else {
        q = currentUser.getInstitute();
      }

      return q.then(
        function(result) {
          if (result instanceof Array) {
            $scope.institutes = result;
          } else {
            $scope.institutes = [result];
          }
 
          return $scope.institutes;
        }
      );
    }

    function loadUsers(filterOpts) {
      User.query(filterOpts).then(function(result) {
        if (!$scope.users && result.length > 12) {
          //
          // Show search options when # of users are more than 12
          //
          osRightDrawerSvc.open();
        }

        $scope.users = result;
        pagerOpts.refreshOpts(result);
        $scope.ctx.checkList = new CheckList($scope.users);
      });
    };

    function getUsersCount() {
      return User.getCount($scope.userFilterOpts)
    }

    function activateUsers(msgKey) {
      var users = $scope.ctx.checkList.getSelectedItems();
      User.bulkUpdate({detail: {activityStatus: 'Active'}, ids: getUserIds(users)}).then(
        function(savedUsers) {
          Alerts.success(msgKey);

          angular.forEach(users, function(user) { user.selected = false; });
          $scope.ctx.checkList = new CheckList($scope.users);
        }
      );
    }

    function getUserIds(users) {
      return users.map(function(user) { return user.id; });
    }
    
    $scope.showUserOverview = function(user) {
      $state.go('user-detail.overview', {userId:user.id});
    };

    $scope.broadcastAnnouncement = function() {
      $modal.open({
        templateUrl: 'modules/administrative/user/announcement.html',
        controller: 'AnnouncementCtrl'
      }).result.then(
        function(announcement) {
          User.broadcastAnnouncement(announcement).then(
            function(resp) {
              Alerts.success('user.announcement.success');
            }
          );
        }
      );
    }

    $scope.deleteUsers = function() {
      var users = $scope.ctx.checkList.getSelectedItems();

      if (!currentUser.admin) {
        var admins = users.filter(function(user) { return !!user.admin; })
          .map(function(user) { return user.getDisplayName(); });

        if (admins.length > 0) {
          Alerts.error('user.admin_access_req', {adminUsers: admins});
          return;
        }
      }

      var opts = {
        confirmDelete: 'user.delete_users',
        successMessage: 'user.users_deleted',
        onBulkDeletion: function() {
          loadUsers($scope.userFilterOpts);
        }
      }

      DeleteUtil.bulkDelete({bulkDelete: User.bulkDelete}, getUserIds(users), opts);
    }

    $scope.editUsers = function() {
       var users = $scope.ctx.checkList.getSelectedItems();
       ItemsHolder.setItems('users', users);
       $state.go('user-bulk-edit');
    }

    $scope.unlockUsers = function() {
      activateUsers('user.users_unlocked');
    }

    $scope.approveUsers = function() {
      activateUsers('user.users_approved');
    }

    init();
  });

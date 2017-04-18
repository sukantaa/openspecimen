
angular.module('os.administrative.user.list', ['os.administrative.models'])
  .controller('UserListCtrl', function(
    $scope, $state, $modal, currentUser,
    osRightDrawerSvc, Institute, User, ItemsHolder, PvManager, Util, DeleteUtil, Alerts, ListPagerOpts) {

    var pagerOpts;
    var pvInit = false;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getUsersCount});
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
        initCtx();
      });
    };

    function initCtx() {
      $scope.ctx = {
        selection: {all: false, any: false, users: []}
      }
    }

    function getUsersCount() {
      return User.getCount($scope.userFilterOpts)
    }

    function activateUsers(msgKey) {
      var users = $scope.ctx.selection.users;
      User.bulkUpdate({detail: {activityStatus: 'Active'}, userIds: getUserIds(users)}).then(
        function(savedUsers) {
          Alerts.success(msgKey);

          angular.forEach(users, function(user) { user.selected = false; });
          initCtx();
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

    $scope.toggleSelectAll = function() {
      $scope.ctx.selection.any = $scope.ctx.selection.all;

      if (!$scope.ctx.selection.all) {
        $scope.ctx.selection.users = [];
      } else {
        $scope.ctx.selection.users = [].concat($scope.users);
      }

      angular.forEach($scope.users,
        function(user) {
          user.selected = $scope.ctx.selection.all;
        }
      );
    }

    $scope.toggleSelect = function(user) {
      var users = $scope.ctx.selection.users;
      if (user.selected) {
        users.push(user);
      } else {
        var idx = users.indexOf(user);
        if (idx != -1) {
          users.splice(idx, 1);
        }
      }

      $scope.ctx.selection.all = (users.length == $scope.users.length);
      $scope.ctx.selection.any = (users.length > 0);
    };

    $scope.deleteUsers = function() {
      var users = $scope.ctx.selection.users;

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
        onBulkDeletion: loadUsers
      }

      DeleteUtil.bulkDelete({bulkDelete: User.bulkDelete}, getUserIds(users), opts);
    }

    $scope.editUsers = function() {
       var users = $scope.ctx.selection.users;
       ItemsHolder.setItems('users', users);
       $state.go('user-bulk-edit');
    }

    $scope.unlockUsers = function() {
      activateUsers('user.users_unlocked');
    }

    $scope.approveUsers = function() {
      activateUsers('user.users_approved');
    }

    $scope.editUsers = function() {
       var users = $scope.ctx.selection.users;
       ItemsHolder.setItems('users', users);
       $state.go('user-bulk-edit');
    }

    init();
  });

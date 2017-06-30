
angular.module('os.common.notif')
  .controller('NotifAlertCtrl', function($scope, $interval, UserNotification) {

    var ctx = {
      notifsOpen: false, 
      unreadCount: 0,
      openTime: undefined
    };

    function init() {
      $scope.ctx = ctx;
      syncUnreadCount();

      $scope.$watch('ctx.notifsOpen',
        function(opened) {
          if (opened === true) {
            if (ctx.unreadCount > 0) {
              ctx.openTime = new Date();
            }
          } else if (opened === false && !!ctx.openTime) {
            UserNotification.markAsRead(ctx.openTime).then(
              function() {
                ctx.unreadCount = 0;
              }
            );
          }
        }
      );
    }

    function syncUnreadCount() {
      if (ctx.notifsOpen) {
        scheduleNextSync();
        return;
      }

      UserNotification.getUnreadCount().then(
        function(count) {
          ctx.unreadCount = count;
          scheduleNextSync();
        },

        function() {
          scheduleNextSync();
        }
      );
    }

    function scheduleNextSync() {
      $interval(syncUnreadCount, 5000, 1);
    }

    init();
  });

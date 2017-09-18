
angular.module('openspecimen')
  .directive('osManualSearch', function($window, SettingUtil) {

    function linker(scope, element, attrs) {
      scope.ctx = {link: ''};
      SettingUtil.getSetting('training', 'manual_search_link').then(
        function(setting) {
          scope.ctx.link = setting.value;
        }
      );

      scope.search = function(query) {
        //
        // assumes the manual search link is configured with
        // search string query param
        //
        $window.open(scope.ctx.link + query);
      }
    }

    return {
      restrict: 'E',
      scope: {},
      replace: true,
      link : linker,
      templateUrl: 'modules/common/manual-search.html'
    };
  });

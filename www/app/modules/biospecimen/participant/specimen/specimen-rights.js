angular.module('openspecimen')
  .directive('showIfSpmnOpAllowed', function($q, AuthorizationService, SettingUtil) {

    function showIfContainerReadAllowed(spmn, element) {
      var opts = {resource: 'StorageContainer', operations: ['Read'], sites: [spmn.storageSite]}
      if (!AuthorizationService.isAllowed(opts)) {
        element.remove();
      }
    }

    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watchGroup([attrs.showIfSpmnOpAllowed, attrs.cp, attrs.spmn], function(newValues) {
          var opts = newValues[0];
          var cp   = newValues[1];
          var spmn = newValues[2];

          if (!AuthorizationService.isAllowed(opts)) {
            element.remove();
            return;
          }

          if (!cp || !spmn || !spmn.storageSite) {
            return;
          }

          var q;
          if (cp.containerBasedAccess === true) {
            q = $q.defer();
            q.resolve({value: 'true'});
            q = q.promise;
          } else {
            q = SettingUtil.getSetting('biospecimen', 'container_based_access');
          }

          q.then(
            function(setting) {
              if (setting.value.toLowerCase() == 'true') {
                showIfContainerReadAllowed(spmn, element);
              }
            }
          );
        });
      }
    }
  });

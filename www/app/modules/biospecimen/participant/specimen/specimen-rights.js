angular.module('openspecimen')
  .directive('showIfSpmnOpAllowed', function(AuthorizationService, SettingUtil) {
    function ensureEditAllowed(spmn, element) {
      var containerReadOpt = {resource: 'StorageContainer', operations: ['Read'], sites: [spmn.storageSite]}
      if (!AuthorizationService.isAllowed(containerReadOpt)) {
        element.remove();
      }
    }

    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watchGroup([attrs.showIfSpmnOpAllowed, attrs.cp, attrs.spmn], function(newValues) {
          var opAllowed = newValues[0];
          var cp = newValues[1];
          var spmn = newValues[2];

          if (!opAllowed) {
            element.remove();
            return;
          }

          if (!spmn.storageSite) {
            return;
          }

          if (cp.containerBasedAccess != undefined && cp.containerBasedAccess != null) {
            if (cp.containerBasedAccess) {
              ensureEditAllowed(spmn, element);
            }
          } else {
            SettingUtil.getSetting('biospecimen', 'container_based_access').then(
              function(setting) {
                if (setting.value.toLowerCase() == 'true') {
                  ensureEditAllowed(spmn, element);
                }
              }
            );
          }
        });
      }
    }
  });

angular.module('openspecimen')
  .directive('showIfSpmnEditAllowed', function(AuthorizationService, SettingUtil) {
    function ensureEditAllowed(spmn, element) {
      var containerReadOpt = {resource: 'StorageContainer', operations: ['Read'], sites: [spmn.storageSite]}
      if (!AuthorizationService.isAllowed(containerReadOpt)) {
        element.remove();
      }
    }

    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watchGroup([attrs.cp, attrs.spmn], function(newValues) {
          var cp = newValues[0];
          var spmn = newValues[1];

          if (!spmn.storageSite) {
            return;
          }

          if (cp.containerBasedAccess != undefined || cp.containerBasedAccess != null) {
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

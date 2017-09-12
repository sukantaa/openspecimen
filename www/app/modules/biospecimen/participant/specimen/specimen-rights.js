angular.module('openspecimen')
  .directive('showIfSpmnOpAllowed', function($q, $rootScope, AuthorizationService, SettingUtil) {

    function showIfContainerReadAllowed(spmn, element) {
      var opts = {resource: 'StorageContainer', operations: ['Read'], sites: [spmn.storageSite]}
      if (!AuthorizationService.isAllowed(opts)) {
        element.remove();
      }
    }

    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watchGroup([attrs.showIfSpmnOpAllowed, attrs.cp, attrs.cpr, attrs.spmn], function(newValues) {
          var opts = {
            showIfSpmnOpAllowed: newValues[0],
            cp: newValues[1],
            cpr: newValues[2],
            spmn: newValues[3],
            op: attrs.op
          };

          var resourceOpts = opts.showIfSpmnOpAllowed;
          if (!resourceOpts) {
            var sites = undefined;
            var cp = undefined;
            if (opts.cp && opts.cpr) {
              cp = opts.cp.shortTitle;
              sites = opts.cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
              if ($rootScope.global.appProps.mrn_restriction_enabled) {
                sites = sites.concat(opts.cpr.getMrnSites());
              }
            }

            resourceOpts = {resource: 'VisitAndSpecimen', operations: [opts.op], cp: cp, sites: sites};
          }

          if (!AuthorizationService.isAllowed(resourceOpts)) {
            element.remove();
            return;
          }

          if (!opts.cp || !opts.spmn || !opts.spmn.storageSite) {
            return;
          }

          var q;
          if (opts.cp.containerBasedAccess !== null && opts.cp.containerBasedAccess !== undefined) {
            //
            // Give precedence to CP specific setting and then system level setting. If CP level setting is given (defined) then use it.
            //
            q = $q.defer();
            q.resolve({value: opts.cp.containerBasedAccess ? 'true' : 'false'});
            q = q.promise;
          } else {
            q = SettingUtil.getSetting('biospecimen', 'container_based_access');
          }

          q.then(
            function(setting) {
              if (setting.value.toLowerCase() == 'true') {
                showIfContainerReadAllowed(opts.spmn, element);
              }
            }
          );
        });
      }
    }
  });

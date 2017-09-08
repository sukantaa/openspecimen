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
        var watchAttrs = ['showIfSpmnOpAllowed', 'cp', 'cpr', 'spmn'];
        for (var i = watchAttrs.length - 1; i >= 0; --i) {
          if (!attrs[watchAttrs[i]]) {
            watchAttrs.splice(i, 1);
          }
        }

        scope.$watchGroup(watchAttrs, function(newValues) {
          var opts = {op: attrs.op};
          angular.forEach(watchAttrs,
            function(attr, index) {
              opts[attr] = newValues[index];
            }
          );

          var resourceOpts = opts.showIfSpmnOpAllowed;
          if (!resourceOpts) {
            var sites = undefined;
            var cp = undefined;
            if (opts.cp && opts.cpr) {
              cp = opts.cp.shortTitle;
              sites = opts.cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
              if ($root.global.appProps.mrn_restriction_enabled) {
                sites = sites.concat(opts.cpr.getMrnSites());
              }
            }

            resourceOpts = {resource: 'VisitAndSpecimen', operations: [op], cp: cp, sites: sites};
          }

          if (!AuthorizationService.isAllowed(resourceOpts)) {
            element.remove();
            return;
          }

          if (!opts.cp || !opts.spmn || !opts.spmn.storageSite) {
            return;
          }

          var q;
          if (opts.cp.containerBasedAccess === true) {
            q = $q.defer();
            q.resolve({value: 'true'});
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

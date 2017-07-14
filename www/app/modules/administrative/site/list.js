angular.module('os.administrative.site.list', ['os.administrative.models'])
  .controller('SiteListCtrl', function($scope, $state, currentUser,
    Institute, Site, Util, DeleteUtil, ListPagerOpts, CheckList) {

    var pagerOpts;
    var defInstitutes;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getSitesCount});
      $scope.siteFilterOpts = {includeStats: true, maxResults: pagerOpts.recordsPerPage + 1};
      $scope.ctx = {
        exportDetail: {objectType: 'site'},
        institutes: []
      };

      loadInstitutes();
      loadSites($scope.siteFilterOpts);
      Util.filter($scope, 'siteFilterOpts', loadSites);
    }

    function loadInstitutes(searchString) {
      if (!currentUser.admin) {
        return;
      }

      if (defInstitutes && defInstitutes.length < 100) {
        return;
      }

      Institute.query({name : searchString}).then(
        function(institutes) {
          $scope.ctx.institutes = institutes;

          if (!searchString) {
            defInstitutes = institutes;
          }
        }
      );
    }

    function loadSites(filterOpts) {
      Site.query(filterOpts).then(
        function(siteList) {
          $scope.siteList = siteList;
          $scope.ctx.checkList = new CheckList(siteList);
          pagerOpts.refreshOpts(siteList);
        }
      );
    };

    function getSiteIds(sites) {
      return sites.map(function(site) { return site.id; });
    }

    function getSitesCount() {
      return Site.getCount($scope.siteFilterOpts);
    }

    $scope.loadInstitutes = loadInstitutes;

    $scope.showSiteOverview = function(site) {
      $state.go('site-detail.overview', {siteId: site.id});
    };

    $scope.deleteSites = function() {
      var sites = $scope.ctx.checkList.getSelectedItems();

      var opts = {
        confirmDelete:  'site.delete_sites',
        successMessage: 'site.sites_deleted',
        onBulkDeletion: function() {
          loadSites($scope.siteFilterOpts);
        }
      }

      DeleteUtil.bulkDelete({bulkDelete: Site.bulkDelete}, getSiteIds(sites), opts);
    }

    init();
  });


angular.module('openspecimen')
  .directive('osExport', function(ExportJob, Alerts) {
    function linker(scope, element, attrs) {
      scope.export = function() {
        new ExportJob(scope.detail).$saveOrUpdate().then(
          function(savedJob) {
            Alerts.success('export.job_submitted', savedJob);
          }
        );
      }
    }

    return {
      restrict: 'E',
      scope: {
        detail: '='
      },
      replace: true,
      link : linker,
      template: '<button ng-click="export()">' +
                '  <span class="fa fa-download"></span>' +
                '  <span translate="common.buttons.export">Export</span>' +
                '</a>'
    };
  });

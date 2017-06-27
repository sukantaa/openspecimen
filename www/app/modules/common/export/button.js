
angular.module('openspecimen')
  .directive('osExport', function(ExportJob, Alerts) {
    function linker(scope, element, attrs) {
      scope.export = function() {
        var recordIds = null;
        if (scope.checkList) {
          recordIds = scope.checkList.getSelectedItems().map(function(item) { return item.id; });
        }

        var detail = angular.copy(scope.detail);
        detail.recordIds = recordIds;
        new ExportJob(detail).$saveOrUpdate().then(
          function(savedJob) {
            Alerts.success('export.job_submitted', savedJob);
          }
        );
      }
    }

    return {
      restrict: 'E',
      scope: {
        detail: '=',
        checkList: '=?'
      },
      replace: true,
      link : linker,
      template: '<button ng-click="export()">' +
                '  <span class="fa fa-download"></span>' +
                '  <span translate="common.buttons.export">Export</span>' +
                '</button>'
    };
  });

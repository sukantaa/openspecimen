
angular.module('os.common.export')
  .controller('AddEditExportJobCtrl', function($scope, $state, exportDetail, ExportJob, Alerts, Util) {
    function init() {
      $scope.exportDetail = exportDetail;
      exportDetail.type   = exportDetail.type || {};
      exportDetail.params = exportDetail.params || {};

      if (exportDetail.types && exportDetail.types.length > 0) {
        onTypeSelect(exportDetail.types[0]);
      }
    }

    function onTypeSelect(type) {
      exportDetail.type = type;
    }

    $scope.export = function() {
      var type = exportDetail.type;
      var job  = new ExportJob({objectType: type.type, params: type.params || {}});
      if (!!exportDetail.inputCsv) {
        job.params[type.$$input.var] = exportDetail.inputCsv;
      }

      var msg = Alerts.info('export.initiated');
      job.$saveOrUpdate().then(
        function(savedJob) {
          Alerts.remove(msg);
          if (savedJob.status == 'COMPLETED') {
            Alerts.info('export.downloading_file');
            Util.downloadFile(savedJob.fileUrl());
          } else if (savedJob.status == 'FAILED') {
            Alerts.error('export.failed', savedJob);
          } else {
            Alerts.info('export.file_will_be_emailed', savedJob);
          }
          $state.go(exportDetail.onSuccess.state, exportDetail.onSuccess.params);
        }
      );
    }

    $scope.onTypeSelect = onTypeSelect;

    init();
  });


angular.module('os.common.export')
  .factory('ExportJob', function(osModel) {
    var ExportJob = new osModel('export-jobs');

    return ExportJob;
  });

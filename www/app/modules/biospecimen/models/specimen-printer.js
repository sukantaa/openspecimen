angular.module('os.biospecimen.models.specimenlabelprinter', ['os.common.models'])
  .factory('SpecimenLabelPrinter', function(osModel, $http, Alerts) {
    var SpecimenLabelPrinter = osModel('specimen-label-printer');
 
    SpecimenLabelPrinter.getTokens = function() {
      return $http.get(SpecimenLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    SpecimenLabelPrinter.printLabels = function(detail) {
      return $http.post(SpecimenLabelPrinter.url(), detail).then(
        function(resp) {
          Alerts.success("specimens.labels_print_job_created", {jobId: resp.data.id});
          return resp.data;
        }
      );
    };

    return SpecimenLabelPrinter;
  });

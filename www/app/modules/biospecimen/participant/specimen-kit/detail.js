angular.module('os.biospecimen.specimenkit')
  .controller('SpecimenKitDetailCtrl', function($scope, specimenKit, Util) {
    $scope.kit = specimenKit;

    $scope.downloadReport = function() {
      Util.downloadReport(specimenKit, 'specimen_kit');
    }
  });

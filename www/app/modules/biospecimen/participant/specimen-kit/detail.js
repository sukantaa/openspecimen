angular.module('os.biospecimen.specimenkit')
  .controller('SpecimenKitDetailCtrl', function($scope, specimenKit) {
    $scope.kit = specimenKit;
  });

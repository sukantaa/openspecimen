
angular.module('os.biospecimen.participant.overview', ['os.biospecimen.models'])
  .controller('ParticipantOverviewCtrl', function(
    $scope, $state, $stateParams, hasFieldsFn, cp, cpr, visits,
    Visit, CollectSpecimensSvc, ExtensionsUtil, Util, Alerts) {

    function init() {
      $scope.occurredVisits    = Visit.completedVisits(visits);
      $scope.anticipatedVisits = Visit.anticipatedVisits(visits);
      $scope.missedVisits      = Visit.missedVisits(visits);

      ExtensionsUtil.createExtensionFieldMap($scope.cpr.participant);
      $scope.partCtx = {
        obj: {cpr: $scope.cpr},
        inObjs: ['cpr'],
        showEdit: hasFieldsFn(['cpr'], [])
      }
    }

    $scope.isOtherProtocol = function(other) {
      return other.cpShortTitle != $scope.cpr.cpShortTitle;
    }

    $scope.anonymize = function() {
      Util.showConfirm({
        title: "participant.anonymize",
        confirmMsg: "participant.confirm_anonymize",
        isWarning: true,
        ok: function() {
          $scope.cpr.anonymize().then(
            function(savedCpr) {
              angular.extend($scope.cpr, savedCpr);
              ExtensionsUtil.createExtensionFieldMap($scope.cpr.participant);
              Alerts.success("participant.anonymized_successfully");
            }
          )
        }
      });
    }

    $scope.collect = function(visit) {
      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectVisit(retSt, cp, cpr.id, visit);
    }

    $scope.collectPending = function(visit) {
      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectPending(retSt, cp, cpr.id, visit);
    }

    init();
  });

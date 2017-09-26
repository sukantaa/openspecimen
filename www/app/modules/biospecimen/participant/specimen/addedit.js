
angular.module('os.biospecimen.specimen.addedit', [])
  .controller('AddEditSpecimenCtrl', function(
    $scope, $state, cp, cpr, visit, specimen, extensionCtxt, hasDict,
    Specimen, Container, CollectSpecimensSvc, PvManager, Util, ExtensionsUtil) {

    function init() {
      var currSpecimen = $scope.currSpecimen = angular.copy(specimen);
      delete currSpecimen.children;

      currSpecimen.cpId = currSpecimen.cpId || cp.id;
      currSpecimen.visitId = visit && visit.id;
      currSpecimen.createdOn = currSpecimen.createdOn || new Date();

      if (currSpecimen.lineage == 'Aliquot') {
        currSpecimen.anatomicSite = currSpecimen.laterality = undefined;
      }

      if (currSpecimen.status != 'Collected') {
        if (!currSpecimen.id) {
          currSpecimen.status = 'Collected';
        }

        currSpecimen.availableQty = currSpecimen.initialQty;
      }

      if (!currSpecimen.labelFmt) {
        if (specimen.lineage == 'New') {
          currSpecimen.labelFmt = cpr.specimenLabelFmt;
        } else if (specimen.lineage == 'Aliquot') {
          currSpecimen.labelFmt = cpr.aliquotLabelFmt;
        } else if (specimen.lineage == 'Derived') {
          currSpecimen.labelFmt = cpr.derivativeLabelFmt;
        }
      }

      var exObjs = ['specimen.lineage', 'specimen.parentLabel', 'specimen.events'];
      if (!$scope.currSpecimen.id && !$scope.currSpecimen.reqId) {
        var currentDate = new Date();
        $scope.currSpecimen.collectionEvent = {
          user: $scope.currentUser,
          time: currentDate
        };

        $scope.currSpecimen.receivedEvent = {
          user: $scope.currentUser,
          time: currentDate
        };

        $scope.currSpecimen.collectionEvent.container = "Not Specified";
        $scope.currSpecimen.collectionEvent.procedure = "Not Specified";
        $scope.currSpecimen.receivedEvent.receivedQuality = "Acceptable";
      } else {
        exObjs.push('specimen.collectionEvent', 'specimen.receivedEvent');
      }

      $scope.currSpecimen.initialQty = Util.getNumberInScientificNotation($scope.currSpecimen.initialQty);
      $scope.currSpecimen.availableQty = Util.getNumberInScientificNotation($scope.currSpecimen.availableQty);
      $scope.currSpecimen.concentration = Util.getNumberInScientificNotation($scope.currSpecimen.concentration);

      $scope.spmnCtx = {
        obj: {specimen: $scope.currSpecimen, cp: cp}, inObjs: ['specimen'], exObjs: exObjs,
        isVirtual: specimen.showVirtual(),
        manualSpecLabelReq: !!currSpecimen.label || !currSpecimen.labelFmt || cp.manualSpecLabelEnabled,
        mode: 'single'
      }

      $scope.deFormCtrl = {};
      $scope.extnOpts = ExtensionsUtil.getExtnOpts(currSpecimen, extensionCtxt);

      if (!hasDict) {
        loadPvs();
      }
    }

    function loadPvs() {
      $scope.biohazards = PvManager.getPvs('specimen-biohazard');
      $scope.specimenStatuses = PvManager.getPvs('specimen-status');
    };

    function saveSpecimen() {
      $scope.currSpecimen.$saveOrUpdate().then(
        function(result) {
          angular.extend($scope.specimen, result);
          var params = {specimenId: result.id, cprId: result.cprId, visitId: result.visitId, srId: result.reqId};
          $state.go('specimen-detail.overview', params);
        }
      );
    }

    function getSpecimensToSave(specimen) {
      var labels = [], numOfSpecimens = specimen.numOfSpecimens;
      if (!numOfSpecimens) {
        labels = Util.splitStr(specimen.labels, /,|\t|\n/);
        numOfSpecimens = labels.length;
      }

      specimen.status = 'Pending';
      delete specimen.numOfSpecimens;
      delete specimen.labels;

      // Create multiple specimens
      var specimensToSave = [];
      for (var i = 0; i < numOfSpecimens; ++i) {
        var toSave = angular.copy(specimen);
        if (labels.length > 0) {
          toSave.label = labels[i];
        }

        specimensToSave.push(toSave);
      }

      return specimensToSave;
    }

    function getState() {
      return {state: $state.get('visit-detail.overview'), params: {visitId: visit.id}};
    };

    $scope.save = function() {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      if (formCtrl) {
         $scope.currSpecimen.extensionDetail = formCtrl.getFormData();
      }

      if ($scope.spmnCtx.mode == 'single') {
        saveSpecimen();
      } else if ($scope.spmnCtx.mode == 'multiple') {
        var opts = {showCollVisitDetails: false};
        var specimensToSave = getSpecimensToSave($scope.currSpecimen);
        Specimen.save(specimensToSave).then(
          function(specimens) {
            angular.forEach(specimens,
              function(spmn) {
                spmn.selected = true;
                spmn.collectionEvent = $scope.currSpecimen.collectionEvent;
                spmn.receivedEvent = $scope.currSpecimen.receivedEvent;
              }
            );

            CollectSpecimensSvc.collect(getState(), visit, specimens, opts);
          }
        );
      }
    }

    init();
  });

angular.module('os.biospecimen.specimen.bulkaddevent', ['os.biospecimen.models'])
  .controller('BulkAddEventCtrl', function(
    $scope, $translate, $q, events, Form, SpecimensHolder,
    Specimen, SpecimenEvent, Alerts, Util, SpecimenUtil) {

    var formCtx = {};
    function init() {
      $scope.selectedEvent = {op: 'ADD'};
      $scope.eventTableCtrl = {};
      
      $scope.specimens = SpecimensHolder.getSpecimens() || [];
      $scope.showVisit = showVisit($scope.specimens);

      SpecimensHolder.setSpecimens(null);
    }
    
    function filterEvents() {
      var showCollRecvEvent =  ($scope.selectedEvent.op != 'ADD');
      if (showCollRecvEvent) {
        showCollRecvEvent = $scope.specimens.every(function(spmn) { return spmn.lineage == 'New'; });
      }

      return events.filter(
        function(event) {
          return !event.sysForm ||
            (showCollRecvEvent && ['SpecimenCollectionEvent', 'SpecimenReceivedEvent'].indexOf(event.name) != -1)
        }
      );
    }
    
    function getSpecimenOpts(eventOpts, specimens) {
      var opts = [], optsMap = {}, spmnIds = [];
      angular.forEach(specimens,
        function(spmn) {
          var opt = {
            key: {
              id: spmn.id,
              objectId: spmn.id,
              label: spmn.label
            },
            appColumnsData: {},
            records: []
          };
          opts.push(opt);
          optsMap[spmn.id] = opt;
          spmnIds.push(spmn.id);
        }
      );

      if (eventOpts.op == 'ADD') {
        var q = $q.defer();
        q.resolve(opts);
        return q.promise;
      } else {
        var params = {entityType: 'SpecimenEvent', objectId: spmnIds};
        return Form.getLatestRecords(eventOpts.formId, 'SpecimenEvent', spmnIds).then(
          function(records) {
            angular.forEach(records,
              function(record) {
                optsMap[record.appData.objectId].records.push(record);
              }
            );

            return opts;
          }
        );
      }
    }

    function onValidationError() {
      Alerts.error('common.form_validation_error');
    }

    function showVisit(specimens) {
      return specimens.some(function(spmn) { return !spmn.$$specimenCentricCp; });
    }

    $scope.passThrough = function() {
      return true;
    }

    $scope.addSpecimens = function(specimens) {
      if (!specimens) {
        return false;
      }

      if (!$scope.showVisit) {
        $scope.showVisit = showVisit(specimens);
      }

      Util.addIfAbsent($scope.specimens, specimens, 'id');
      return true;
    }
    
    $scope.removeSpecimen = function(index) {
      $scope.specimens.splice(index, 1);
      $scope.showVisit = showVisit($scope.specimens);
    }
    
    $scope.initEventOpts = function() {
      $scope.specimenEvents = filterEvents();
      if (!$scope.selectedEvent.formId) {
        return;
      }

      var promises = [];
      if (formCtx.lastFormId != $scope.selectedEvent.formId) {
        promises.push(Form.getDefinition($scope.selectedEvent.formId));
      } else {
        var q = $q.defer();
        q.resolve(formCtx.lastFormDef);
        promises.push(q.promise);
      }

      promises.push(getSpecimenOpts($scope.selectedEvent, $scope.specimens));

      $q.all(promises).then(
        function(result) {
          formCtx = {lastFormId: $scope.selectedEvent.formId, formDef: result[0]};

          var opts = {
            formId            : $scope.selectedEvent.formId,
            formDef           : result[0],
            appColumns        : [],
            tableData         : result[1],
            idColumnLabel     : $translate.instant('specimens.title'),
            mode              : 'add',
            allowRowSelection : false,
            onValidationError : onValidationError
          };

          $scope.eventOpts = opts;
        }
      );
    }

    $scope.saveEvent = function() {
      var tableCtrl = $scope.eventTableCtrl.ctrl;
      var data = tableCtrl.getData();
      if (!data) {
        return;
      }

      SpecimenEvent.save($scope.selectedEvent.formId, data).then(
        function(savedData) {
          $scope.back();
          Alerts.success("specimens.bulk_events.events_saved");
        }
      );
    }

    $scope.copyFirstToAll = function () {
      $scope.eventTableCtrl.ctrl.copyFirstToAll();
    }
    
    init();
  });


angular.module('os.biospecimen.extensions.list', ['os.biospecimen.models', 'os.biospecimen.extensions.util'])
  .controller('FormsListCtrl', function($scope, $state, $stateParams, forms, ExtensionsUtil) {
    function init() {
      $scope.forms   = forms;
      $scope.fctx = {
        inited: false,
        state: $scope.extnState + 'list',
        record: undefined
      };

      if (forms.length > 0 && !$stateParams.formCtxtId && !$stateParams.formId) {
        $state.go($state.current.name, {formCtxtId: forms[0].formCtxtId, formId: forms[0].formId}, {replace: true});
      } else {
        var selectedForm = $scope.selectedForm = null;
        for (var i = 0; i < forms.length; ++i) {
          if (forms[i].formCtxtId == $stateParams.formCtxtId) {
            selectedForm = $scope.selectedForm = forms[i];
            break;
          }
        }

        if ($stateParams.recordId && selectedForm) {
          selectedForm.getRecord($stateParams.recordId, {includeMetadata: true}).then(
            function(record) {
              var selectedRec;
              for (var i = 0; i < selectedForm.records.length; ++i) {
                if (selectedForm.records[i].recordId == record.id) {
                  selectedRec = selectedForm.records[i];
                  break;
                }
              }

              angular.extend($scope.fctx, {record: record, selectedRec: selectedRec, inited: true});
            }
          );
        } else if (selectedForm && selectedForm.records.length == 1) {
          var recordId = selectedForm.records[0].recordId;
          var params = {formCtxtId: selectedForm.formCtxtId, formId: selectedForm.formId, recordId: recordId};
          $state.go($state.current.name, params, {replace: true});
        } else {
          $scope.fctx.inited = true;
        }
      }
    }

    $scope.showRecord = function(record) {
      $state.go($scope.extnState + 'list', {formId: record.formId, recordId: record.recordId, formCtxtId: record.fcId});
    }

    $scope.deleteRecord = function(record, gotoListView) {
      ExtensionsUtil.deleteRecord(record,
        function(record) {
          var selForm = $scope.selectedForm;
          var idx = selForm.records.indexOf(record);
          selForm.records.splice(idx, 1);

          if (selForm.records.length == 1) {
            var recId = selForm.records[0].recordId;
            $state.go($state.current.name, {formCtxtId: selForm.formCtxtId, formId: selForm.formId, recordId: recId});
          } else if (gotoListView) {
            $state.go($state.current.name, {formCtxtId: selForm.formCtxtId, formId: selForm.formId, recordId: null});
          }
        }
      ); 
    }

    init();
  });

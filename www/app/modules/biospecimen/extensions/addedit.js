
angular.module('os.biospecimen.extensions.addedit-record', [])
  .controller('FormRecordAddEditCtrl', function(
    $scope, $state, $stateParams, forms, records, formDef,
    postSaveFilters, viewOpts,
    LocationChangeListener, ExtensionsUtil, Alerts) {

    var recId = $stateParams.recordId;
    if (!!recId) {
      recId = parseInt(recId);
    }

    $scope.formOpts = {
      formId: $stateParams.formId,
      formDef: formDef,
      recordId: recId,
      formCtxtId: parseInt($stateParams.formCtxId),
      objectId: $scope.object.id,
      showActionBtns: true,
      showSaveNext: viewOpts.showSaveNext,

      onSave: function(formData, next) {
        angular.forEach(postSaveFilters, function(filter) {
          filter($scope.object, formDef.name, formData);
        });

        Alerts.success("extensions.record_saved");

        var nextForm = undefined;
        if (next) {
          var anyForm = false;
          for (var i = 0; i < forms.length - 1; ++i) {
            var f = forms[i], nf = forms[i + 1];
            if (!anyForm && f.formId == $stateParams.formId) {
              anyForm = true;
            }

            if (anyForm && (nf.multiRecord || nf.records.length == 0)) {
              nextForm = nf;
              break;
            }
          }
        }

        if (nextForm) {
          var params = angular.extend({}, $stateParams);
          params.formCtxId = nextForm.formCtxtId;
          params.formId = nextForm.formId;
          params.recordId = undefined;
          LocationChangeListener.allowChange();
          $state.go($state.current.name, params);
        } else {
          gotoRecsList();
        }
      },

      onError: function() {
        alert("Error");
      },

      onCancel: function() {
        gotoRecsList();
      },

      onPrint: function(html) {
        alert(html);
      },

      onDelete: function() {
        var record = {recordId: recId, formId: $stateParams.formId, formCaption: formDef.caption}
        ExtensionsUtil.deleteRecord(record, gotoRecsList);
      }
    };

    function gotoRecsList() {
      if (typeof viewOpts.goBackFn == 'function') {
        viewOpts.goBackFn();
        return;
      }

      reloadRecs().then(
        function() {
          LocationChangeListener.allowChange();
          var params = {formId: $stateParams.formId, formCtxtId: $stateParams.formCtxId, recordId: null}
          $state.go($scope.extnState + 'list', params);
        }
      );
    }

    function reloadRecs() {
      records.length = 0;
      return $scope.object.getRecords().then(
        function(dbRecs) {
          Array.prototype.push.apply(records, dbRecs);
          ExtensionsUtil.linkFormRecords(forms, records);
          return dbRecs;
        }
      );
    }
  });

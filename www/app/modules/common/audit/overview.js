angular.module('os.common.audit')
  .directive('osAuditOverview', function(Audit) {

    function linker(scope, element, attrs) {
      var objectsList = scope.objectsList;
      if (!!scope.objectName && !!scope.objectId) {
        objectsList = [{objectName: scope.objectName, objectId: scope.objectId}];
      }

      if (!angular.isArray(objectsList)) {
        return;
      }

      Audit.getInfo(objectsList).then(
        function(auditInfoList) {
          var auditInfo = {};
          angular.forEach(auditInfoList,
            function(audit, idx) {
              if (idx == 0) {
                auditInfo = audit;
              } else {
                if (audit.createdOn < auditInfo.createdOn) {
                  auditInfo.createdBy = audit.createdBy;
                  auditInfo.createdOn = audit.createdOn;
                }

                if (audit.lastUpdatedOn < auditInfo.lastUpdatedOn) {
                  auditInfo.lastUpdatedBy = audit.lastUpdatedBy;
                  auditInfo.lastUpdatedOn = audit.lastUpdatedOn;
                }
              }
            }
          );

          scope.audit = auditInfo;
        }
      );
    }

    return {
      restrict: 'E',
      scope: {
        objectName: '=',
        objectId: '=',
        objectsList: '='
      },
      replace: true,
      templateUrl: 'modules/common/audit/overview.html',
      link: linker
    }
  });

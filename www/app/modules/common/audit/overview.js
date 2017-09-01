angular.module('os.common.audit')
  .directive('osAuditOverview', function(Audit) {

    function linker(scope, element, attrs) {
      Audit.getInfo(scope.objectName, scope.objectId).then(
        function(audit) {
          scope.audit = audit;
        }
      );
    }

    return {
      restrict: 'E',
      scope: {
        objectName: '=',
        objectId: '='
      },
      replace: true,
      templateUrl: 'modules/common/audit/overview.html',
      link: linker
    }
  });

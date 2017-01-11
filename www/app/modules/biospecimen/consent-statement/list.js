angular.module('os.biospecimen.consentstatement')
  .controller('ConsentStatementListCtrl', function($scope, $state, ConsentStatement, Util) {

    function init() {
      $scope.ctx = {
        statements: [],
        filterOpts: {}
      };

      loadStmts();
      Util.filter($scope, 'ctx.filterOpts', loadStmts);
    }

    function loadStmts(filterOpts) {
      ConsentStatement.query(filterOpts).then(
        function(statements) {
          $scope.ctx.statements = statements;
        }
      );
    }

    $scope.showStatementEdit = function(stmt) {
      $state.go('consent-statement-addedit', {stmtId: stmt.id});
    };

    init();
  });


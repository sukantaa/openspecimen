
angular.module('os.biospecimen.consentstatement', ['os.biospecimen.models'])
  .config(function($stateProvider) {
    $stateProvider
      .state('consent-statement-list', {
        url: '/consent-statements',
        templateUrl: 'modules/biospecimen/consent-statement/list.html',
        controller: 'ConsentStatementListCtrl',
        parent: 'signed-in'
      })
      .state('consent-statement-addedit', {
        url: '/consent-statement-addedit/:stmtId',
        templateUrl: 'modules/biospecimen/consent-statement/addedit.html',
        resolve: {
          consentStatement: function($stateParams, ConsentStatement) {
            if ($stateParams.stmtId) {
              return ConsentStatement.getById($stateParams.stmtId);
            }

            return new ConsentStatement();
          }
        },
        controller: 'ConsentStatementAddEditCtrl',
        parent: 'signed-in'
      });
  });

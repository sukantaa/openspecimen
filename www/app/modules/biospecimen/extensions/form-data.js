
angular.module('os.biospecimen.extensions')
  .directive('osFormData', function(ApiUrls) {
    return {
      restrict: 'E',

      templateUrl: 'modules/biospecimen/extensions/form-data.html',

      scope: {
        data: '='
      },

      link: function(scope, element, attrs) {
        scope.filesUrl = ApiUrls.getBaseUrl() + 'form-files';
      }
    };
  });

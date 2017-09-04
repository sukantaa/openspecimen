angular.module('os.common.audit')
  .factory('Audit', function($http, osModel) {
    var Audit = osModel('audit');

    Audit.getInfo = function(objectsList) {
      return $http.post(Audit.url(), objectsList).then(
        function(resp) {
          return resp.data;
        }
      );
    }

    return Audit;
  });

angular.module('os.common.audit')
  .factory('Audit', function($http, osModel) {
    var Audit = osModel('audit');

    Audit.getInfo = function(objectName, objectId) {
      var params = {objectName: objectName, objectId: objectId};
      return Audit.query(params, Audit.noTransform);
    }

    return Audit;
  });

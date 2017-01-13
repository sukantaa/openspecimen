angular.module('os.biospecimen.models.specimenkit', ['os.common.models'])
  .factory('SpecimenKit', function(osModel, $http) {

    var SpecimenKit = osModel('specimen-kits');

    SpecimenKit.list = function(opts) {
      var defOpts = {includeStats: true};
      return SpecimenKit.query(angular.extend(defOpts, opts || {}));
    }

    return SpecimenKit;
});

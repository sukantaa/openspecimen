
angular.module('os.common')
  .factory('UrlResolver', function($state) {
    var urlStateMap = {};

    function regUrlState(urlKey, stateName, idName) {
      urlStateMap[urlKey] = {stateName: stateName, idName: idName};
    }

    function getUrl(urlKey, idValue) {
      var urlState = urlStateMap[urlKey];
      if (!urlState) {
        return null;
      }

      var params = {};
      params[urlState.idName || 'id'] = idValue;
      return $state.href(urlState.stateName, params);
    }

    return {
      regUrlState: regUrlState,

      getUrl: getUrl
    };
  }
);

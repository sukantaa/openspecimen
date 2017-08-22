angular.module('openspecimen')
  .directive('osKeyValues', function() {
    return {
      restrict: 'C',

      link: function(scope, element, attrs) {
        scope.$watch(
          function() {
            var result = '';
            angular.forEach(element.find('li.item .value'),
              function(el, idx) {
                result += idx + el.scrollWidth;
              }
            );
            return result;
          },
          function() {
            angular.forEach(element.find('li.item .value'),
              function(el) {
                if (el.offsetWidth < el.scrollWidth) {
                  el.setAttribute('title', el.textContent.trim());
                }
              }
            );
          }
        );
      }
    };
  });

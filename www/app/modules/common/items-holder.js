angular.module('openspecimen')
  .factory('ItemsHolder', function() {
     var itemsMap = {};

     return {
       getItems: function(type) {
         return itemsMap[type] || [];
       },

       setItems: function(type, items) {
         itemsMap[type] = items;
       }
     }
  });

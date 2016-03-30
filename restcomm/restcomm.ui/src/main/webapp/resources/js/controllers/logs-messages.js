'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsMessagesCtrl', function ($scope, $resource, $timeout, $modal, Identity, RCommLogsMessages) {

  $scope.Math = window.Math;

  $scope.sid = Identity.getAccountSid();

  // pagination support ----------------------------------------------------------------------------------------------

  $scope.currentPage = 1; //current page
  $scope.maxSize = 5; //pagination max size
  $scope.entryLimit = 10; //max rows for data table
  $scope.reverse = false;
  $scope.predicate = "date_created";

  $scope.setEntryLimit = function(limit) {
    $scope.entryLimit = limit;
    $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
  };

  $scope.setPage = function(pageNo) {
    $scope.currentPage = pageNo;
  };

  $scope.filter = function() {
    $timeout(function() { //wait for 'filtered' to be changed
      /* change pagination with $scope.filtered */
      $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    }, 10);
  };

  // Modal : Message Details
  $scope.showMessageDetailsModal = function (message) {
    $modal.open({
      controller: 'LogsMessagesDetailsCtrl',
      scope: $scope,
      templateUrl: 'modules/modals/modal-logs-messages.html',
      resolve: {
        messageSid: function() {
          return message.sid;
        }
      }
    });
  };

  // initialize with a query
  $scope.messagesLogsList = RCommLogsMessages.query({accountSid: $scope.sid}, function() {
    $scope.noOfPages = Math.ceil($scope.messagesLogsList.length / $scope.entryLimit);
  });


$scope.sort = function(item) {
        if ($scope.predicate == 'date_created') {
            return new Date(item.date_created);
        }
           return  item[$scope.predicate];
    };

$scope.sortBy = function(field) {
        if ($scope.predicate != field) {
            $scope.predicate = field;
            $scope.reverse = false;
        } else {
            $scope.reverse = !$scope.reverse;
        }
    };


});

rcMod.controller('LogsMessagesDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, Identity, RCommLogsMessages, messageSid) {
  $scope.sid = Identity.getAccountSid();
  $scope.messageSid = $routeParams.messageSid || messageSid;

  $scope.closeMessageDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.messageDetails = RCommLogsMessages.view({accountSid: $scope.sid, smsMessageSid: $scope.messageSid});
});
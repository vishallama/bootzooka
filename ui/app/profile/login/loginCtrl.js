"use strict";

angular.module('smlBootzooka.profile')
    .controller('LoginCtrl', function LoginCtrl($scope, UserSessionService, $location, $state, $stateParams, NotificationsService) {

        var self = this;

        $scope.user = {};
        $scope.user.login = '';
        $scope.user.password = '';
        $scope.user.rememberMe = false;

        $scope.login = function () {
            // set dirty to show error messages on empty fields when submit is clicked
            $scope.loginForm.login.$dirty = true;
            $scope.loginForm.password.$dirty = true;

            if ($scope.loginForm.$invalid === false) {
                UserSessionService.login($scope.user).then(self.loginOk, self.loginFailed);
            }
        };

        this.loginOk = function () {
            var optionalRedirect = UserSessionService.loadTarget();
            if (optionalRedirect) {
                $state.go(optionalRedirect.targetState, optionalRedirect.targetParams)
            } else {
                $location.path("/main");
            }
        };

        this.loginFailed = function () {
            NotificationsService.showError("Invalid login and/or password.");
        };
    });
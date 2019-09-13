angular
    .module('ipInformationApp', [])
    .controller('ipController', ['$scope', '$http',
        function ($scope, $http) {

            /** CONSTANTS **/
            $scope.ipPattern = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

            /** MODEL **/
            $scope.ip = {
                value: "5.6.7.8",
            };
            $scope.error = '';
            $scope.buttonLabel = 'Send';

            $scope.statistics = {};

            /** SCOPE FUNCTIONS **/
            $scope.formatLanguages = function(languages) {
                return languages
                    .map(language => language.name + " (" + language.isoCode + ")")
                    .join(", ");
            };

            $scope.send = function () {
                if (document.querySelector('form:invalid'))
                    return;

                $scope.buttonLabel = 'Loading...';
                $scope.error = "";

                $http({
                    method: "GET",
                    url: "http://localhost:8080/ip-tracer",
                    params: {ip: $scope.ip.value}

                }).then((response) => {     // Success
                    $scope.ip.data = response.data;

                }, (response) => {          // Error
                    $scope.error = "Unable to fetch IP information";

                }).finally(() => {
                    $scope.buttonLabel = 'Send';
                });
            };

            /** INTERNAL FUNCTIONS **/

        }]);
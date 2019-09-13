angular
    .module('ipInformationApp', [])
    .controller('ipController', ['$scope', '$http',
        function ($scope, $http) {

            /** MODEL **/
            $scope.ip = {
                value: "5.6.7.8",
            };
            $scope.error = '';
            $scope.buttonLabel = 'Send';

            $scope.statistics = {}

            /** SCOPE FUNCTIONS **/
            $scope.formatLanguages = function(languages) {
                return languages
                    .map(language => language.name + " (" + language.isoCode + ")")
                    .join(", ");
            };

            $scope.send = function () {
                $scope.buttonLabel = 'Loading...';
                $http({
                    method: "GET",
                    url: "http://localhost:8080/ip-tracer",
                    params: {ip: $scope.ip.value}

                }).then((response) => {     // Success
                    $scope.ip.data = response.data

                }, (response) => {          // Error
                    $scope.error = response.statusText;

                }).finally(() => {
                    $scope.buttonLabel = 'Send';
                });
            };

            /** INTERNAL FUNCTIONS **/

        }]);
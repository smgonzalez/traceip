angular
    .module('ipApp', [])
    .controller('ipController', ['$scope', '$http',
        function ($scope, $http) {

            $scope.ip = {
                value: "5.6.7.8",
                data: {}
            };
            $scope.error = '';

            $scope.send = function () {
                $http({
                    method: "GET",
                    url: "http://localhost:8080/ip-tracer",
                    params: {ip: $scope.ip.value}
                }).then((response) => {     // Success
                    console.log(response.data);
                    $scope.ip.data = response.data
                }, (response) => {          // Error
                    $scope.error = response.statusText;
                });
            };

        }]);
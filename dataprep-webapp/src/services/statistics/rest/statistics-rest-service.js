(function () {
    'use strict';

    /**
     * @ngdoc service
     * @name data-prep.services.statistics.service:StatisticsRestService
     * @description Statistics REST service.
     */
    function StatisticsRestService($http, RestURLs) {

        /**
         * @ngdoc method
         * @name getAggregations
         * @methodOf data-prep.services.statistics.service:StatisticsRestService
         * @param {string} stringifiedColumn The column metadata
         * @description Fetch the aggregation on a column of the dataset
         * @returns {HttpPromise} The POST promise
         */
        this.getAggregations = function(stringifiedColumn) {

            var responsePromise =  $http.post(RestURLs.datasetUrl + '/aggregation/column', stringifiedColumn)
                .then(function(response) {
                    return response;
            }, function() {
                    var mockResponse ={};
                    mockResponse.data = [
                        {
                            'data': 'Lansing',
                            'occurrences': 15
                        },
                        {
                            'data': 'Helena',
                            'occurrences': 5
                        },
                        {
                            'data': 'Baton Rouge',
                            'occurrences': 64
                        },
                        {
                            'data': 'Annapolis',
                            'occurrences': 4
                        },
                        {
                            'data': 'Pierre',
                            'occurrences': 104
                        },
                        {
                            'data': 'Nashville',
                            'occurrences': 4
                        },
                        {
                            'data': 'Salt Lake City',
                            'occurrences': 3
                        },
                        {
                            'data': '',
                            'occurrences': 3
                        },
                        {
                            'data': 'Concord',
                            'occurrences': 0
                        },
                        {
                            'data': 'Hartford',
                            'occurrences': 3
                        },
                        {
                            'data': 'Boston',
                            'occurrences': 3
                        },
                        {
                            'data': 'Carson City',
                            'occurrences': 5
                        },
                        {
                            'data': 'Topeka',
                            'occurrences': 75
                        },
                        {
                            'data': 'Montgomery',
                            'occurrences': 68
                        },
                        {
                            'data': 'Richmond',
                            'occurrences': 2
                        }
                    ];

                    return mockResponse;
            });

            return responsePromise;
        };

    }

    angular.module('data-prep.services.statistics')
        .service('StatisticsRestService', StatisticsRestService);
})();
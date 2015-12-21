describe('ColumnProfile controller', function () {
    'use strict';

    var createController, scope;

    var stateMock;
    var removeFilterFn = function () {
    };

    beforeEach(module('data-prep.column-profile', function ($provide) {
        stateMock = {playground: {grid: {}}};
        $provide.constant('state', stateMock);
    }));

    beforeEach(inject(function ($rootScope, $controller) {
        scope = $rootScope.$new();

        createController = function () {
            return $controller('ColumnProfileCtrl', {
                $scope: scope
            });
        };
    }));

    describe('filter', function () {
        beforeEach(inject(function ($q, FilterService, StatisticsService, PlaygroundService) {
            spyOn(FilterService, 'addFilterAndDigest').and.returnValue();
            spyOn(StatisticsService, 'getRangeFilterRemoveFn').and.returnValue(removeFilterFn);
            spyOn(PlaygroundService, 'updateStatistics').and.returnValue($q.when());
        }));

        it('should add a new "exact" filter', inject(function (FilterService) {
            //given
            var ctrl = createController();
            var obj = {'data': 'Ulysse', 'occurrences': 5};

            stateMock.playground.grid.selectedColumn = {
                id: '0001',
                name: 'firstname'
            };

            //when
            ctrl.addBarchartFilter(obj);

            //then
            expect(FilterService.addFilterAndDigest).toHaveBeenCalledWith('exact', '0001', 'firstname', {
                phrase: 'Ulysse',
                caseSensitive: true
            });
        }));

        it('should add a new "range" filter from StatisticsService', inject(function (StatisticsService, FilterService) {
            //given
            var ctrl = createController();
            var interval = {
                min: 5,
                max: 15
            };

            stateMock.playground.grid.selectedColumn = {
                id: '0001',
                name: 'firstname'
            };

            //when
            ctrl.addRangeFilter(interval);

            //then
            expect(StatisticsService.getRangeFilterRemoveFn).toHaveBeenCalled();
            expect(FilterService.addFilterAndDigest).toHaveBeenCalledWith('inside_range', '0001', 'firstname', {interval: [5, 15]}, removeFilterFn);
        }));

        it('should NOT add a "range" filter on date columns', inject(function (StatisticsService, FilterService) {
            //given
            var ctrl = createController();
            var interval = ['1 jan 2015', '1 feb 2015'];

            stateMock.playground.grid.selectedColumn = {
                id: '0001',
                name: 'birth',
                type: 'date'
            };

            //when
            ctrl.addRangeFilter(interval);

            //then
            expect(FilterService.addFilterAndDigest).not.toHaveBeenCalled();
        }));

        it('should add a new "empty_records" filter from exact_filter on barchart click callback', inject(function (StatisticsService, FilterService) {
            //given
            var ctrl = createController();
            var obj = {'data': '', 'occurrences': 5};

            stateMock.playground.grid.selectedColumn = {
                id: '0001',
                name: 'firstname'
            };

            //when
            ctrl.addBarchartFilter(obj);

            //then
            expect(FilterService.addFilterAndDigest).toHaveBeenCalledWith('empty_records', '0001', 'firstname');
        }));
    });

    describe('external bindings', function () {
        beforeEach(inject(function ($q, PlaygroundService) {
            spyOn(PlaygroundService, 'updateStatistics').and.returnValue($q.when());
        }));

        it('should bind histogram getter to StatisticsService.histogram', inject(function (StatisticsService) {
            //given
            var data = {};
            var ctrl = createController();

            //when
            StatisticsService.histogram = data;

            //then
            expect(ctrl.histogram).toBe(data);
        }));

        it('should bind filteredHistogram getter to StatisticsService.filteredHistogram', inject(function (StatisticsService) {
            //given
            var data = {};
            var ctrl = createController();

            //when
            StatisticsService.filteredHistogram = data;

            //then
            expect(ctrl.filteredHistogram).toBe(data);
        }));

        it('should bind aggregationColumns getter to StatisticsService.getAggregationColumns()', inject(function (StatisticsService) {
            //given
            var ctrl = createController();

            var numericColumns = [{id: '0001'}, {id: '0002'}];
            spyOn(StatisticsService, 'getAggregationColumns').and.returnValue(numericColumns);

            //then
            expect(ctrl.aggregationColumns).toBe(numericColumns);
        }));
    });

    describe('aggregation', function () {
        beforeEach(inject(function ($q, PlaygroundService) {
            spyOn(PlaygroundService, 'updateStatistics').and.returnValue($q.when());
        }));

        it('should get the current aggregation name', inject(function (StatisticsService) {
            //given
            var ctrl = createController();
            StatisticsService.histogram = {
                aggregation: 'MAX'
            };

            //when
            var aggregation = ctrl.getCurrentAggregation();

            //then
            expect(aggregation).toBe('MAX');
        }));

        it('should get the default aggregation name when there is no histogram', inject(function (StatisticsService) {
            //given
            var ctrl = createController();
            StatisticsService.histogram = null;

            //when
            var aggregation = ctrl.getCurrentAggregation();

            //then
            expect(aggregation).toBe('LINE_COUNT');
        }));

        it('should get the default aggregation name when histogram is not an aggregation', inject(function (StatisticsService) {
            //given
            var ctrl = createController();
            StatisticsService.histogram = {data: []};

            //when
            var aggregation = ctrl.getCurrentAggregation();

            //then
            expect(aggregation).toBe('LINE_COUNT');
        }));

        it('should change aggregation chart', inject(function (StatisticsService) {
            //given
            spyOn(StatisticsService, 'processAggregation').and.returnValue();
            var ctrl = createController();

            var column = {id: '0001'};
            var aggregation = {name: 'MAX'};

            //when
            ctrl.changeAggregation(column, aggregation);

            //then
            expect(StatisticsService.processAggregation).toHaveBeenCalledWith(column, aggregation);
        }));

        it('should do nothing if the current histogram is already the wanted aggregation', inject(function (StatisticsService) {
            //given
            var column = {id: '0001'};
            var aggregation = {name: 'MAX'};

            spyOn(StatisticsService, 'processAggregation').and.returnValue();
            StatisticsService.histogram = {
                aggregation: aggregation,
                aggregationColumn: column,
                data: [{field: 'toto', value: 2}]
            };

            var ctrl = createController();

            //when
            ctrl.changeAggregation(column, aggregation);

            //then
            expect(StatisticsService.processAggregation).not.toHaveBeenCalled();
        }));
    });

    describe('statistics', function () {
        it('should not update columns statistics when there are statistics charts to display', inject(function ($q, PlaygroundService, StatisticsService) {
            //given
            spyOn(PlaygroundService, 'updateStatistics').and.returnValue($q.when());
            StatisticsService.histogram = {data: [{field: 'toto', value: 2}]};

            //when
            createController();

            //then
            expect(PlaygroundService.updateStatistics).not.toHaveBeenCalled();
        }));

        it('should update statistics when there are no histogram yet', inject(function ($q, PlaygroundService, StatisticsService) {
            //given
            spyOn(PlaygroundService, 'updateStatistics').and.returnValue($q.when());
            StatisticsService.histogram = null;

            //when
            createController();

            //then
            expect(PlaygroundService.updateStatistics).toHaveBeenCalled();
        }));

        it('should retry statistics update when previous fetch has been rejected (stats not computed yet) with a delay of 1500ms', inject(function ($q, $timeout, PlaygroundService, StatisticsService) {
            //given
            var retry = 0;
            spyOn(PlaygroundService, 'updateStatistics').and.callFake(function () {
                if (retry === 0) {
                    retry++;
                    return $q.reject();
                }
                else {
                    return $q.when();
                }
            });
            StatisticsService.histogram = null;

            //when
            createController();
            scope.$digest();
            expect(PlaygroundService.updateStatistics.calls.count()).toBe(1);
            $timeout.flush(1500);

            //then
            expect(PlaygroundService.updateStatistics.calls.count()).toBe(2);
        }));
    });
});
describe('Dataset list controller', function () {
    'use strict';

    var createController, scope;
    var datasets = [
        {id: 'ec4834d9bc2af8', name: 'Customers (50 lines)'},
        {id: 'ab45f893d8e923', name: 'Us states'},
        {id: 'cf98d83dcb9437', name: 'Customers (1K lines)'}
    ];
    var refreshedDatasets = [
        {id: 'ec4834d9bc2af8', name: 'Customers (50 lines)'},
        {id: 'ab45f893d8e923', name: 'Us states'}
    ];
    var preparations = [
        {id: 'z68x48e61w23d8', name: 'clean up names', datasetid : datasets[0].id},     // default preparation for datasets[0]
        {id: 'd68d789s8e354e', name: 'fill missing dates', datasetid : datasets[1].id},
        {id: '8w848ds9q6823w', name: 'format cyties names', datasetid : datasets[1].id}
    ];

    beforeEach(module('data-prep.dataset-list'));

    beforeEach(inject(function ($rootScope, $controller, $q, DatasetListService, PreparationListService, PlaygroundService, MessageService) {
        var datasetsValues = [datasets, refreshedDatasets];
        scope = $rootScope.$new();

        createController = function () {
            var ctrl = $controller('DatasetListCtrl', {
                $scope: scope
            });
            return ctrl;
        };

        spyOn(DatasetListService, 'refreshDatasets').and.callFake(function () {
            DatasetListService.datasets = datasetsValues.shift();
            return $q.when(DatasetListService.datasets);
        });
        spyOn(PreparationListService, 'getPreparationsPromise').and.returnValue($q.when(true));
        spyOn(PreparationListService, 'getDatasetPreparations').and.callFake(function(dataset) {
            var getPreparationsForDataset =
                _.filter(preparations, function(preparation) {
                    return preparation.datasetid === dataset.id;
                });
            return getPreparationsForDataset;
        });

        spyOn(PlaygroundService, 'initPlayground').and.returnValue($q.when(true));
        spyOn(PlaygroundService, 'show').and.callThrough();
        spyOn(MessageService, 'error').and.returnValue(null);
    }));

    afterEach(inject(function($stateParams) {
        $stateParams.datasetid = null;
    }));

    it('should get dataset on creation', inject(function (DatasetListService, PreparationListService) {
        //when
        var ctrl = createController();
        scope.$digest();

        //then
        expect(DatasetListService.refreshDatasets).toHaveBeenCalled();
        expect(PreparationListService.getDatasetPreparations).toHaveBeenCalled();
        expect(ctrl.datasets).toBe(datasets);
    }));

    it('should init playground with the provided datasetId from url', inject(function ($stateParams, PlaygroundService) {
        //given
        $stateParams.datasetid = 'ab45f893d8e923';

        //when
        createController();
        scope.$digest();

        //then
        expect(PlaygroundService.initPlayground).toHaveBeenCalledWith(datasets[1]);
        expect(PlaygroundService.show).toHaveBeenCalled();
    }));

    it('should set the default preparation for each dataset', inject(function (DatasetListService, PreparationListService) {
        //when
        var ctrl = createController();
        scope.$digest();

        //then
        expect(DatasetListService.refreshDatasets).toHaveBeenCalled();
        expect(PreparationListService.getDatasetPreparations.calls.count()).toEqual(datasets.length);
        expect(ctrl.datasets[0].defaultPreparationId).toBe(preparations[0].id);
        expect(ctrl.datasets[1].defaultPreparationId).not.toBeDefined();
        expect(ctrl.datasets[2].defaultPreparationId).not.toBeDefined();
    }));

    it('should show error message when dataset id is not in users dataset', inject(function ($stateParams, PlaygroundService, MessageService) {
        //given
        $stateParams.datasetid = 'azerty';

        //when
        createController();
        scope.$digest();

        //then
        expect(PlaygroundService.initPlayground).not.toHaveBeenCalled();
        expect(PlaygroundService.show).not.toHaveBeenCalled();
        expect(MessageService.error).toHaveBeenCalledWith('PLAYGROUND_FILE_NOT_FOUND_TITLE', 'PLAYGROUND_FILE_NOT_FOUND', {type: 'dataset'});
    }));

    describe('already created', function () {
        var ctrl;

        beforeEach(inject(function ($rootScope, $q, MessageService, DatasetService) {
            ctrl = createController();
            scope.$digest();

            spyOn(DatasetService, 'deleteDataset').and.returnValue($q.when(true));
            spyOn(MessageService, 'success').and.callThrough();
        }));

        it('should delete dataset, show toast and refresh dataset list', inject(function ($q, MessageService, DatasetService, PreparationListService, DatasetListService, TalendConfirmService) {
            //given
            var dataset = datasets[0];
            spyOn(TalendConfirmService, 'confirm').and.returnValue($q.when(true));

            //when
            ctrl.delete(dataset);
            scope.$digest();

            //then
            expect(TalendConfirmService.confirm).toHaveBeenCalledWith({disableEnter: true}, [ 'DELETE_PERMANENTLY', 'NO_UNDONE_CONFIRM' ], {type: 'dataset', name: 'Customers (50 lines)' });
            expect(DatasetService.deleteDataset).toHaveBeenCalledWith(dataset);
            expect(MessageService.success).toHaveBeenCalledWith('REMOVE_SUCCESS_TITLE', 'REMOVE_SUCCESS', {type: 'dataset', name: 'Customers (50 lines)'});
            expect(DatasetListService.refreshDatasets).toHaveBeenCalled();
            expect(PreparationListService.getDatasetPreparations).toHaveBeenCalled();
        }));

        it('should init and show playground', inject(function ($rootScope, PlaygroundService) {
            //given
            var dataset = {name: 'Customers (50 lines)', id: 'aA2bc348e933bc2'};

            //when
            ctrl.open(dataset);
            $rootScope.$apply();

            //then
            expect(PlaygroundService.initPlayground).toHaveBeenCalledWith(dataset);
            expect(PlaygroundService.show).toHaveBeenCalled();
        }));

        it('should bind datasets getter to DatasetListService.datasets', inject(function (DatasetListService) {
            //given
            expect(ctrl.datasets).toBe(datasets);

            //when
            DatasetListService.datasets = refreshedDatasets;

            //then
            expect(ctrl.datasets).toBe(refreshedDatasets);
        }));

        it('should bind datasets setter to DatasetListService.datasets', inject(function (DatasetListService) {
            //given
            expect(DatasetListService.datasets).toBe(datasets);

            //when
            ctrl.datasets = refreshedDatasets;

            //then
            expect(DatasetListService.datasets).toBe(refreshedDatasets);
        }));
    });
});

/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Recipe Bullet controller', function () {
    'use strict';

    var createController;
    var scope;
    var step;

    beforeEach(angular.mock.module('data-prep.recipe-bullet'));

    beforeEach(inject(function ($rootScope, $controller) {
        scope = $rootScope.$new();
        step = {
            transformation: { stepId: '138ea798bc56', column:{ id:'0002' } }
        };

        createController = function () {
            var ctrlFn = $controller('RecipeBulletCtrl', {
                $scope: scope
            }, true);
            ctrlFn.instance.step = step;
            return ctrlFn();
        };
    }));

    it('should init step index', inject(function (StepUtilsService) {
        //given
        spyOn(StepUtilsService, 'getStepIndex').and.returnValue(3);

        //when
        var ctrl = createController();

        //then
        expect(ctrl.stepIndex).toBe(3);
    }));

    it('should return true when step is the first step', inject(function (StepUtilsService) {
        //given
        spyOn(StepUtilsService, 'isFirstStep').and.returnValue(true);
        var ctrl = createController();

        //when
        var isFirst = ctrl.isStartChain();

        //then
        expect(isFirst).toBe(true);
    }));

    it('should return false when step is NOT the first step', inject(function (StepUtilsService) {
        //given
        spyOn(StepUtilsService, 'isFirstStep').and.returnValue(false);
        var ctrl = createController();

        //when
        var isFirst = ctrl.isStartChain();

        //then
        expect(isFirst).toBe(false);
    }));

    it('should return true when step is the last step', inject(function (StepUtilsService) {
        //given
        spyOn(StepUtilsService, 'isLastStep').and.returnValue(true);
        var ctrl = createController();

        //when
        var isLast = ctrl.isEndChain();

        //then
        expect(isLast).toBe(true);
    }));

    it('should return false when step is NOT the last step', inject(function (StepUtilsService) {
        //given
        spyOn(StepUtilsService, 'isLastStep').and.returnValue(false);
        var ctrl = createController();

        //when
        var isLast = ctrl.isEndChain();

        //then
        expect(isLast).toBe(false);
    }));

    it('should call hover start action', inject(function (RecipeService, RecipeBulletService) {
        //given
        spyOn(RecipeBulletService, 'stepHoverStart').and.returnValue();
        var ctrl = createController();

        //when
        ctrl.stepHoverStart();

        //then
        expect(RecipeBulletService.stepHoverStart).toHaveBeenCalledWith(step);
    }));

    it('should call hover end action', inject(function (RecipeBulletService) {
        //given
        spyOn(RecipeBulletService, 'stepHoverEnd').and.returnValue();
        var ctrl = createController();

        //when
        ctrl.stepHoverEnd();

        //then
        expect(RecipeBulletService.stepHoverEnd).toHaveBeenCalledWith(step);
    }));

    it('should call toggle step action', inject((PlaygroundService) => {
        //given
        spyOn(PlaygroundService, 'toggleStep').and.returnValue();
        const ctrl = createController();

        //when
        ctrl.toggleStep();

        //then
        expect(PlaygroundService.toggleStep).toHaveBeenCalledWith(step);
    }));

    describe('active step', function () {
        var stepIndex = 3;
        var activeStepThreshold = 5;
        var allSvgs = [2, 5, 8, 58, 4, 212, 87, 52];

        beforeEach(inject(function (StepUtilsService) {
            step.inactive = false;

            spyOn(StepUtilsService, 'getStepIndex').and.returnValue(stepIndex);
            spyOn(StepUtilsService, 'getActiveThresholdStepIndex').and.returnValue(activeStepThreshold);
        }));

        it('should get all steps after the current when step is active', function () {
            //given
            var ctrl = createController();

            //when
            var bullets = ctrl.getBulletsToChange(allSvgs);

            //then
            expect(bullets).toEqual(allSvgs.slice(stepIndex));
        });
    });

    describe('inactive step', function () {
        var stepIndex = 5;
        var activeStepThreshold = 2;
        var allSvgs = [2, 5, 8, 58, 4, 212, 87, 52];

        beforeEach(inject(function (StepUtilsService) {
            step.inactive = true;

            spyOn(StepUtilsService, 'getStepIndex').and.returnValue(stepIndex);
            spyOn(StepUtilsService, 'getActiveThresholdStepIndex').and.returnValue(activeStepThreshold);
        }));

        it('should get all steps after the current when step is active', function () {
            //given
            var ctrl = createController();

            //when
            var bullets = ctrl.getBulletsToChange(allSvgs);

            //then
            expect(bullets).toEqual(allSvgs.slice(3, 6));
        });
    });
});

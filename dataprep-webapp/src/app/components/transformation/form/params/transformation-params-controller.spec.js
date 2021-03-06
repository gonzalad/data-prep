/*  ============================================================================

  Copyright (C) 2006-2018 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Transform params controller', function () {
    'use strict';

    var createController;
    var scope;

    beforeEach(angular.mock.module('data-prep.transformation-form'));

    beforeEach(inject(function ($rootScope, $controller) {
        scope = $rootScope.$new();

        createController = function () {
            return $controller('TransformParamsCtrl', {
                $scope: scope,
            });
        };
    }));

    it('should get the correct parameter type', function () {
        // given / when
        var ctrl = createController();

        //then
        expect(ctrl.getParameterType({ type: 'numeric' })).toEqual('simple');
        expect(ctrl.getParameterType({ type: 'integer' })).toEqual('simple');
        expect(ctrl.getParameterType({ type: 'double' })).toEqual('simple');
        expect(ctrl.getParameterType({ type: 'float' })).toEqual('simple');
        expect(ctrl.getParameterType({ type: 'string' })).toEqual('simple');
        expect(ctrl.getParameterType({ type: 'select' })).toEqual('select');
        expect(ctrl.getParameterType({ type: 'cluster' })).toEqual('cluster');
        expect(ctrl.getParameterType({ type: 'date' })).toEqual('date');
        expect(ctrl.getParameterType({ type: 'column' })).toEqual('column');
        expect(ctrl.getParameterType({ type: 'regex' })).toEqual('regex');
        expect(ctrl.getParameterType({ type: 'hidden' })).toEqual('hidden');
        expect(ctrl.getParameterType({ type: 'toto' })).toEqual('simple');
    });
});

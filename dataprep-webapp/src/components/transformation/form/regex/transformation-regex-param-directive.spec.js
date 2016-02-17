describe('Transformation regex params directive', function () {
    'use strict';
    var scope, createElement;

    beforeEach(module('data-prep.transformation-form'));
    beforeEach(module('htmlTemplates'));

    beforeEach(module('pascalprecht.translate', function ($translateProvider) {
        $translateProvider.translations('en', {
            'COLON': ': '
        });
        $translateProvider.preferredLanguage('en');
    }));


    beforeEach(inject(function($rootScope, $compile) {
        scope = $rootScope.$new();

        createElement = function() {
            var element = angular.element('<transform-regex-param parameter="parameter"></transform-regex-param>');
            $compile(element)(scope);
            scope.$digest();
            return element;
        };
    }));

    afterEach(function() {
        scope.$destroy();
    });

    it('should render an action with a parameter', function() {
        //given
        scope.parameter =   {
                'name': 'param1',
                'label': 'Param 1',
                'type': 'regex',
                'default': ''
            };

        //when
        var element = createElement();

        //then
        expect(element.find('.param-name').text().trim()).toBe('Param 1:');
        expect(element.find('.param-input').find('talend-editable-regex').length).toBe(1);
    });
});
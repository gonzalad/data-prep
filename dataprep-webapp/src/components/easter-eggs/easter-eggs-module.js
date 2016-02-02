import EasterEggsCtrl from './easter-eggs-controller';
import EasterEggs from './easter-eggs-directive';
import StarWars from './star-wars/star-wars-directive';

(() => {
    'use strict';

    /**
     * @ngdoc object
     * @name data-prep.easter-eggs
     * @description This module contains data prep easter eggs
     * @requires 'data-prep.services.easter-eggs'
     * @requires 'data-prep.services.state'
     * @requires 'data-prep.services.utils'
     */
    angular.module('data-prep.easter-eggs',
        [
            'data-prep.services.state',
            'data-prep.services.easter-eggs',
            'data-prep.services.utils'
        ])
        .controller('EasterEggsCtrl', EasterEggsCtrl)
        .directive('starWars', StarWars)
        .directive('easterEggs', EasterEggs);
})();
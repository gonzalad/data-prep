/**
 * @ngdoc controller
 * @name data-prep.transformation-form.controller:TransformDateParamCtrl
 * @description Transformation date parameter controller.
 */
export default function TransformDateParamCtrl() {
    var vm = this;

    /**
     * @ngdoc method
     * @name initParamValues
     * @methodOf data-prep.transformation-form.controller:TransformDateParamCtrl
     * @description [PRIVATE] Init date parameter values to default
     */
    var initParamValue = function () {
        var param = vm.parameter;
        if (!param.value) {
            param.value = param.default;
        }
    };

    initParamValue();
}
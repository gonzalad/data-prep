/**
 * @ngdoc directive
 * @name data-prep.validation.directive:IsDateTimeValidation
 * @description This directive perform a datetime validation on input value modification. you can use format attribute
 * to set a datetime pattern to use otherwise a default is used DD/MM/YYYY hh:mm:ss
 * @restrict E
 * @usage <input ... is-datetime />
 */
export default function IsDateTimeValidation() {
    return {
        require: 'ngModel',
        link: function (scope, elm, attributes, ctrl) {
            ctrl.$validators.isDateTimeValidation = function (modelValue) {
                if (ctrl.$isEmpty(modelValue)) {
                    return false;
                }
                var format = attributes.format ? attributes.format : 'DD/MM/YYYY hh:mm:ss';
                return moment(modelValue, format, true).isValid();
            };
        }
    };
}
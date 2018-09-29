"use strict";

var ENUM_ResponseState = {
    USERINFO_INVALID: 0, /* username or password is error */
    LOGIN_WITHOUT_U2F: 1, /* userinfo passed and no reginfo */
    START_SIGN: 2, /*
					 * userinfo passed and return chanllage, wait to get sign
					 * from u2f token
					 */
    START_REGISTER: 3, /*
					 * return chanllage and wait to get register info from u2f
					 * token
					 */
    FINISH_SIGN: 4,
    FINISH_REGISTER: 5,
    REMOVE_CHALLENGE: 6,
    USER_EXISTED: 7,
    USER_ADDED: 8,
    USER_DELED: 9,
    DEL_REGISTRATION: 10,
    PASSWORD_MODIFIED: 11,
    SERVER_ERROR: 99
}

var U2FErrorDesc = [
    'OK',
    'OTHER_ERROR',
    'BAD_REQUEST',
    'CONFIGURATION_UNSUPPORTED',
    'DEVICE_INELIGIBLE',
    'TIMEOUT'
];

function trace(text, obj) {
    console.log((performance.now() / 1000).toFixed(3) + ": " + text, obj);
}

function ajaxError(xhr, state) {
    alert("ajax invoke error:" + state);
    closeWindow();
}

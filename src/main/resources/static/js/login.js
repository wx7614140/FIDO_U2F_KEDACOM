"use strict";

/* 页面初始化 */
function init() {
    $('form input[type="button"]').click(login);
}

/* 向服务器发送登录请求，如果该用户名已经绑定U2F设备，发回challenge值 */
function login() {
    var username = $('#username').val();
    var password = $('#password').val();
    $.ajax({
        type: "POST",
        url: "/login",
        contentType: "application/json;charset=utf-8",
        headers: {
            // Accept: "application/json;charset=utf-8",
            // contentType: "application/json;charset=utf-8"
            // Content-Type: "text/plain;charset=utf-8"
        },
        data: JSON.stringify(
            {
                username: username,
                password: password
            }),
        // dataType: "json",
        success: loginResult,
        error: ajaxError
    });
}

/* 处理登录结果 */
function loginResult(data, state, xhr) {
    trace("loginresult:" + JSON.stringify(data));
    switch (data.responseState) {
        case ENUM_ResponseState.USERINFO_INVALID:
            handle_USERINFO_INVALID(data)
            break;
        case ENUM_ResponseState.LOGIN_WITHOUT_U2F:
            handle_LOGIN_WITHOUT_U2F(data)
            break;
        case ENUM_ResponseState.START_SIGN:
            handle_WAIT_SIGN(data)
            break;
        case ENUM_ResponseState.START_SIGN:
            alert("服务器端处理错误！");
            break;
        default:
            alert("返回数据格式错误:" + JSON.stringify(data));
            break;
    }
}

/*用户名或密码不正确的情况*/
function handle_USERINFO_INVALID(data) {
    $('#loginerror').text("登录失败，请确认用户名或密码正确");
}

/*不使用U2F设备登录的情况*/
function handle_LOGIN_WITHOUT_U2F(data) {
    //其实这里使用返回的数据做重定向最好
    $(location).attr('href', '/mainContent.html');
}

/*绑定了U2F设备等待设备签名*/
function handle_WAIT_SIGN(data) {
    const signReqData = JSON.parse(data.responseData);
    trace("request sign 返回对象：", signReqData);
    const request = signReqData.request
    const requestId = request.requestId
    const publicKeyCredentialRequestOptions = request.publicKeyCredentialRequestOptions
    const username = request.username
    executeAuthenticateRequest(publicKeyCredentialRequestOptions)
        .then( response => {
                return webauthn.responseToObject(response)
            })
        .then(response => {
            console.log(response)
            $('#promptModal').dialog({
                modal: true,
                open: function (event, ui) {
                    $(".ui-dialog-titlebar-close", $(this).parent()).hide();
                }
            });
            const data = {
                username: username,
                requestId: requestId,
                credential: response
            }
            return data;
        }).then(data =>{
            trace("U2F sign返回值", data);
            $('#promptModal').dialog("close");
            if (data.errorCode) {
                $('#loginerror').text("鉴权失败，U2F设备返回错误码：[" + data.errorCode + "]" + U2FErrorDesc[data.errorCode])
            }
            //向服务器发送签名验证请求
            $.ajax({
                type: "POST",
                url: "/checkSign",
                contentType: "application/json;charset=utf-8",
                headers: {
                    Accept: "application/json;charset=utf-8"
                },
                data: JSON.stringify(data),
                dataType: "json",
                success: handleCheckSignRsp,
                error: ajaxError
            });
        })
}
function executeAuthenticateRequest(publicKeyCredentialRequestOptions) {
    console.log('executeAuthenticateRequest', publicKeyCredentialRequestOptions);
    return webauthn.getAssertion(publicKeyCredentialRequestOptions);
}
//接收U2F设备的签名操作，并发往服务器进行验证
function checkSign(appId, challenge, signRequests) {
    u2f.sign(appId, challenge, signRequests, function (data) {
        trace("U2F sign返回值", data);
        $('#promptModal').dialog("close");
        if (data.errorCode) {
            $('#loginerror').text("鉴权失败，U2F设备返回错误码：[" + data.errorCode + "]" + U2FErrorDesc[data.errorCode])
        }
        //向服务器发送签名验证请求
        $.ajax({
            type: "POST",
            url: "/checkSign",
            contentType: "application/json;charset=utf-8",
            headers: {
                Accept: "application/json;charset=utf-8"
            },
            data: JSON.stringify({
                errorCode: data.errorCode ? data.errorCode : 0,
                username: $('#username').val(),
                challenge: challenge,
                tokenResponse: JSON.stringify(data)
            }),
            dataType: "json",
            success: handleCheckSignRsp,
            error: ajaxError
        });
    });
}

//对服务器端签名验证的结果进行处理
function handleCheckSignRsp(data, state) {
    if (data.responseState == ENUM_ResponseState.FINISH_SIGN) {
        trace("server sign 返回值", data);
        handle_LOGIN_WITHOUT_U2F();
    } else {
        if (data.responseState == ENUM_ResponseState.SERVER_ERROR) {
            $('#loginerror').text("服务器端验签失败！");
        }
    }
}

/* 窗口关闭事件 */
function closeWindow() {
    trace('close window!');
    var userAgent = navigator.userAgent;

    if (userAgent.indexOf("Firefox") != -1 || userAgent.indexOf("Chrome") != -1) {
        window.location.href = "about:blank";
        document.title = "about:blank";
    } else {
        window.opener = null;
        window.open('', '_self');
        window.close();
    }
}

$(window).load(init);
$(window).unload(closeWindow);

"use strict";

/*全局变量定义*/
var userRegInfoList;  //用户注册信息
var currentUi = null;

/* 页面元素定义 */
var imgExit; // 用户退出按钮

/*------------页面初始化-----------------------------*/

$(window).load(init);
$(window).unload(closeWindow);

//页面初始化
function init() {
    // 获取页面元素变量
    imgExit = document.getElementById("exit");
    $('#mgmTabs').hide();

    // 初始有效性检验
    getLoginName(); // 从服务端获取挡墙登录用户名
}

// 窗口关闭事件
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

/* 检查此页面携带的sessionId是否合法 */
function getLoginName() {
    $.ajax({
        type: "POST",
        url: "/getLoginName",
        contentType: "application/json;charset=utf-8",
        success: getLoginNameResult,
        error: ajaxError
    });
}

// checkSession操作的response处理
function getLoginNameResult(data, state) {
    if ((null != data) && ("" !== data)) {
        updatePageStatus("登录用户:" + data + " ");
        initPage();
    } else {
        validPageFail();
    }
}

// 修改EXIT按钮的提示信息
function updatePageStatus(info) {
    var textnodeId = document.createTextNode(info);
    imgExit.parentNode.insertBefore(textnodeId, imgExit);
}

// 请求页面失败
function validPageFail() {
    alert("用户尚未登录，请先登录！");
    redirectLogin();
}

//根据当前用户初始化页面控件
function initPage() {
    //消除右键菜单
    $("html").on('contextmenu', function () {
        return false;
    }).click(function () {
        $("#userCtxMenu").hide();
    });

    // 添加元素事件
    $("#exit").click(redirectLogin);
    $("#btnModifyPwd").click(modifyPwd);
    $('#userCtxMenu a[menu="menuAddUser"]').on('click', onMenuAddUser);
    $('#userCtxMenu a[menu="menuDelUser"]').on('click', onMenuDelUser);
    $("input[name='btnRegU2f']").click(onBtnRegU2f);
    $("input[name='btnUnregU2f']").click(onBtnUnregU2f);
    $("#keyhandleList").change(onKeyhandleListChange);

    //从服务端载入数据
    loadUserInfoFromSvr();
}

//从服务器获取用户数据
function loadUserInfoFromSvr() {
    $.ajax({
        type: "POST",
        url: "/loadUserInfo",
        contentType: "application/json;charset=utf-8",
        headers: {
            Accept: "application/json;charset=utf-8"
        },
        dataType: "json",
        success: loadUserInfoResult,
        error: ajaxError
    });
}

//在sidebar的用户列表中添加用户
function addSidebarUser(username) {
    $("sidebar ul").append("<hr/>");
    $("sidebar ul").append("<li value='" + username +
        "' style='cursor:pointer' onclick='onUserClick(this)'>" +
        username + "</li>");
    //增加上下文菜单
    $("sidebar ul li[value='" + username + "']").on('contextmenu', function (e) {
        var popupmenu = $("#userCtxMenu");
        var l = ($(document).width() - e.clientX) < popupmenu.width() ? (e.clientX - popupmenu.width()) : e.clientX;
        var t = ($(document).height() - e.clientY) < popupmenu.height() ? (e.clientY - popupmenu.height()) : e.clientY;
        popupmenu.css({left: l, top: t}).show();
        onUserClick(this);
        return false;
    });
}

//在sidebar的用户列表中删除用户
function delSidebarUser(username) {

    $("sidebar ul li[value='" + username + "']").prev().remove();
    $("sidebar ul li[value='" + username + "']").remove();
    $("sidebar ul li").click();

}

// loadUserInfo 操作的response处理,data是array类型
function loadUserInfoResult(data, state) {
    if (null == data) {
        return;
    }
    var userlen = data.length;
    userRegInfoList = {};

    //将从服务器获取的数据载入到控件中
    for (var i = 0; i < userlen; i++) { //初始化控件

        var username = data[i].userinfo.username;
        userRegInfoList[username] = {};
        userRegInfoList[username]['userinfo'] = data[i].userinfo;
        userRegInfoList[username]['userreginfo'] = data[i].userreginfo;
        addSidebarUser(username);
        $("sidebar ul li")[0].click();
    }

    //渲染mgmTabs和svrSideBar
    $('#mgmTabs').tabs({
        classes: {'ui-tabs': "highlight"},
        heightStyle: "fill"
    });
    $('#mgmTabs').show();
}

//点击“退出”按钮
function redirectLogin() {
    var form = document.createElement("form");
    form.action = "/logout";
    form.method = "post";
    // send post request
    document.body.appendChild(form);
    form.submit();

    document.body.removeChild(form);
}

/* ------------------窗口控件事件处理-------------------------*/

//sidebar上的列表项点击,选择一个用户，获取该用户的数据
function onUserClick(liEle) {

    $(liEle).addClass("selected").siblings().removeClass("selected");
    var username = liEle.getAttribute("value");
    if ((null != currentUi) && (currentUi.username == username)) {
        //如果在当前用户上重复点击
        return;
    }

    trace("select current user:" + liEle.getAttribute("value"));
    currentUi = userRegInfoList[username].userinfo;

    //"用户操作"TAB页
    clearRegisterResult();
    $("input[name='userName']").val(username);
    $("input[name='passWord']").val(userRegInfoList[username].userinfo.password);
    $("input[name='confirmPassWord']").val(userRegInfoList[username].userinfo.password);

    //“绑定数据”TAB页
    clearBindData();
    var reginfo = userRegInfoList[username]['userreginfo'];
    for (var keyhandle in reginfo) {
        $("#keyhandleList").append("<option value=\"" + keyhandle + "\">" + keyhandle + "</option>");
    }
    $("#keyhandleList").change();
}

//keyhandle列表被选择改变
function onKeyhandleListChange(el) {
    var currKeyhandle = this.value;
    var username = currentUi.username;
    if ((currKeyhandle) && (currKeyhandle != "")) {
        var jsonbinddata = userRegInfoList[username]['userreginfo'][currKeyhandle];
        var binddata = JSON.parse(jsonbinddata);
        $("input[name='regPublicKey']").val(binddata.publicKey);
        $("input[name='regKeyHandle']").val(binddata.keyHandle);
        $("input[name='regCounter']").val(binddata.counter);
        $("input[name='regCompromised']").val(binddata.compromised);
    } else {
        clearBindData();
    }
}

//右键菜单“增加用户”
function onMenuAddUser() {
    $('#dialog-adduser').dialog({
        modal: true,
        height: 300,
        width: 350,
        buttons: {
            "增加": function () {
                var username = $("#username_input").val().trim();
                var password = $("#password_input").val().trim();
                var cpassword = $("#cpassword_input").val().trim();
                if ((username == "") || (password == "") || (cpassword == "")) {
                    alert("所有字段都不可为空！");
                    return false;
                }
                if ((password != "") && (cpassword != "") && (cpassword != password)) {
                    alert("密码与确认密码不一致！");
                    return false;
                }
                addUser(username, password);
                $(this).dialog("close");
            }
        },
        close: function () { //对话框关闭时将输入栏清空
            $("#username_input").val("");
            $("#password_input").val("");
            $("#cpassword_input").val("");
        }
    })
}

//向服务器端添加用户
function addUser(uname, pwd) {
    $.ajax({
        type: "POST",
        url: "/addUser",
        contentType: "application/json;charset=utf-8",
        headers: {
            Accept: "application/json;charset=utf-8"
        },
        data: JSON.stringify({
            username: uname,
            password: pwd
        }),
        dataType: "json",
        success: handleAddUserRsp(uname, pwd),
        error: ajaxError
    });
}

//处理服务器返回的添加用户回应
function handleAddUserRsp(uname, pwd) {
    return function (data, state) {
        if (data.responseState == ENUM_ResponseState.USER_ADDED) {
            //修改全局变量
            userRegInfoList[uname] = {};
            userRegInfoList[uname]['userinfo'] = {
                username: uname,
                password: pwd
            };
            userRegInfoList[uname]['userreginfo'] = {};//新建用户还未绑定U2F
            //修改界面
            addSidebarUser(uname);
            trace("[success]add user:" + uname);
        } else {
            if (data.responseState == ENUM_ResponseState.USER_EXISTED) {
                alert("新增用户失败：用户已存在！");
            } else {
                alert("新增用户失败：服务器处理错误！");
            }
        }
    }
}

//右键菜单“删除用户”
function onMenuDelUser() {
    var username = currentUi.username;
    if ("admin" == username) {
        alert("admin用户不可删除！");
    } else {
        var answer = confirm("您确定要删除用户" + username + "吗？");
        if (answer) {
            $.ajax({
                type: "POST",
                url: "/delUser",
                contentType: "application/json;charset=utf-8",
                headers: {
                    Accept: "application/json;charset=utf-8"
                },
                data: JSON.stringify(currentUi),
                dataType: "json",
                success: handleDelUserRsp(username),
                error: ajaxError
            });
        }
    }
}

//处理服务器返回的删除用户回应
function handleDelUserRsp(username) {
    return function (data, state) {
        if (data.responseState == ENUM_ResponseState.USER_DELED) {
            //全局变量中删除
            delete userRegInfoList[username];
            //界面中删除
            delSidebarUser(username);
            trace("[success]del user:" + username);
        } else {
            if (data.responseState == ENUM_ResponseState.SERVER_ERROR) {
                alert("服务器端删除用户" + username + "失败");
            }
        }
    }
}

//点击“绑定U2F设备”按钮
function onBtnRegU2f(e) {
    var username = $("input[name='userName']").val();
    if (null == currentUi) {
        alert("请先选择绑定用户！");
        return;
    }
    $.ajax({
        type: "POST",
        url: "/requestRegistration",
        contentType: "application/json;charset=utf-8",
        headers: {
            Accept: "application/json;charset=utf-8"
        },
        data: JSON.stringify(currentUi),
        dataType: "json",
        success: handleRequestRegistrationRsp,
        error: ajaxError
    });
}

// startRegistration操作的回应处理,调用U2F设备register接口
function handleRequestRegistrationRsp(data, state) {
    trace("RegisterRequests:", data);
    if (data.responseState == ENUM_ResponseState.START_REGISTER) {
        var responseData = JSON.parse(data.responseData);
        var appid = responseData.appId;
        var registeredKeys = /*[];*/responseData.registeredKeys;
        var registerRequests = responseData.registerRequests;//[{"version":"U2F_V2","challenge":"...","appId":"https://www.kedacom.com"}]
        $('#promptModal').dialog({
            modal: true,
            open: function (event, ui) {
                $(".ui-dialog-titlebar-close", $(this).parent()).hide();
            }
        });
        saveRegistration(appid, registeredKeys, registerRequests);
    } else {
        if (data.responseState == ENUM_ResponseState.SERVER_ERROR) {
            alert("服务端注册请求失败！");
        }
    }
}

// 将U2F设备的回应数据发送到服务器端
function saveRegistration(appid, registeredKeys, registerRequests) {
    u2f.register(appid, registerRequests, registeredKeys, function (data) {
        trace("U2F Register返回值", data);
        $('#promptModal').dialog("close");
        if (data.errorCode) {
            alert("注册失败，U2F设备返回错误码：[" + data.errorCode + "]" + U2FErrorDesc[data.errorCode])
        }
        $.ajax({
            type: "POST",
            url: "/saveRegistration",
            contentType: "application/json;charset=utf-8",
            headers: {
                Accept: "application/json;charset=utf-8"
            },
            data: JSON.stringify({
                errorCode: data.errorCode ? data.errorCode : 0,
                username: currentUi.username,
                challenge: registerRequests[0].challenge,
                tokenResponse: JSON.stringify(data)
            }),
            dataType: "json",
            success: handleSaveRegistrationRsp(registerRequests),
            error: ajaxError
        })
        ;
    });
}

//服务器端返回完成注册信息，显示在界面上
function handleSaveRegistrationRsp(rrs) {
    return function (data, state) {
        if (data.responseState == ENUM_ResponseState.FINISH_REGISTER) {
            trace("server Register返回值", data);
            var regdate = JSON.parse(data.responseData);
            showRegisterResult(rrs, regdate);

            //增加到全局变量中并渲染“绑定数据”TAB页
            clearBindData();
            userRegInfoList[currentUi.username]['userreginfo'][regdate.keyHandle] = data.responseData;
            var reginfo = userRegInfoList[currentUi.username]['userreginfo'];
            for (var keyhandle in reginfo) {
                $("#keyhandleList").append("<option value=\"" + keyhandle + "\">" + keyhandle + "</option>");
            }
            $("#keyhandleList").change();

        } else {
            if (data.responseState == ENUM_ResponseState.SERVER_ERROR) {
                alert("服务端注册操作失败！");
            }
        }
    }
}

//绑定的U2F_keyhandle解除绑定
function onBtnUnregU2f() {
    var keyhandle = $("#keyhandleList").val();
    if ((keyhandle) && (keyhandle != "")) {
    	var username = currentUi.username;
        if (confirm("您确定要解绑U2F设备[keyHandle=" + keyhandle + "]吗？")) {
            $.ajax({
                type: "POST",
                url: "/unRegistration",
                contentType: "application/json;charset=utf-8",
                headers: {
                    Accept: "application/json;charset=utf-8"
                },
                data: JSON.stringify({
                    username: username,
                    keyhandle: keyhandle
                }),
                dataType: "json",
                success: handleUnRegistrationRsp(username, keyhandle),
                error: ajaxError
            })
        }
    } else {
        alert("无选中的keyHandle!");
    }
}

//解除U2F设备绑定后回应处理
function handleUnRegistrationRsp(username, keyhandle) {
    return function (data, state) {
        if (data.responseState == ENUM_ResponseState.DEL_REGISTRATION) {
            //全局变量中删除
            delete userRegInfoList[username]['userreginfo'][keyhandle];
            //界面中删除
            $("#keyhandleList option[value=" + keyhandle + "]").remove();
            $("#keyhandleList").change();
            trace("[success]del Registration:" + username + "/" + keyhandle);
        } else {
            if (data.responseState == ENUM_ResponseState.SERVER_ERROR) {
                alert("服务器端解除绑定注册信息失败！");
            }
        }
    }
}

//修改当前用户的密码
function modifyPwd(){
    var curusername = $("input[name='userName']").val().trim();
    if(curusername==""){
        alert("请先选择用户！");
        return;
    }
    var pwd = $("input[name='passWord']").val().trim();
    var cpwd = $("input[name='confirmPassWord']").val().trim();
    if(!pwd || !cpwd || pwd!=cpwd || pwd==currentUi.password ){
        alert("请确保:密码输入非空,密码栏和确认密码栏一致,新密码与旧密码不同！");
        return;
    }
    $.ajax({
        type: "POST",
        url: "/modifyPassword",
        contentType: "application/json;charset=utf-8",
        headers: {
            Accept: "application/json;charset=utf-8"
        },
        data: JSON.stringify({
            username: curusername,
            password: pwd
        }),
        dataType: "json",
        success: handleModifyPasswordRsp,
        error: ajaxError
    })
}

//处理修改密码后的回应
function handleModifyPasswordRsp(data,state){
    if (data.responseState == ENUM_ResponseState.PASSWORD_MODIFIED) {
        //全局变量中修改
        userRegInfoList[currentUi.username]['userinfo'].password = data.responseData;
        trace("[success]ModifyPassword:" + currentUi.username);
        alert("服务器端修改用户密码成功！");
    } else {
        if (data.responseState == ENUM_ResponseState.SERVER_ERROR) {
            alert("服务器端修改用户密码失败！");
        }
    }

}

// 显示本次注册操作结果
function showRegisterResult(registerRequests, responseData) {
    $("input[name='challenge']").val(registerRequests[0].challenge);
    $("input[name='version']").val(registerRequests[0].version);
    $("input[name='appId']").val(registerRequests[0].appId);
    $("input[name='keyHandle']").val(responseData.keyHandle);
    $("input[name='publicKey']").val(responseData.publicKey);

}

// 清除注册操作结果
function clearRegisterResult() {
    $("input[name='challenge']").val('');
    $("input[name='version']").val('');
    $("input[name='appId']").val('');
    $("input[name='keyHandle']").val('');
    $("input[name='publicKey']").val('');
}

//清除界面上的U2F绑定数据
function clearBindData() {
    $("#keyhandleList").empty();
    $("input[name='regKeyHandle']").val('');
    $("input[name='regPublicKey']").val('');
    $("input[name='regCounter']").val('');
    $("input[name='regCompromised']").val('');
}











//屏幕信息
var screen;
let qr;
let ck;
let pageStatus;
let authCodeCountDown;
let canClickLogin;
let canSendAuth;
let sessionTimeOut;
let totalChromeCount;
let availChromeCount;
let webSessionCount;
let qqSessionCount;
let reg = new RegExp(/^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/);
let reg2 = new RegExp(/^\d{6}$/);
//临时变量，控制ajax顺序
let sendingAuthCode = false;
let cracking = false;
let phone;
let remark;
var serverHost = window.location.host;
var ws;
var screenTimer;
var timeoutTimer;
// Example starter JavaScript for disabling form submissions if there are invalid fields
(function () {
    'use strict';
    window.addEventListener('load', function () {
        // Fetch all the forms we want to apply custom Bootstrap validation styles to
        var forms = document.getElementsByClassName('needs-validation');
        // Loop over them and prevent submission
        var validation = Array.prototype.filter.call(forms, function (form) {
            form.addEventListener('submit', function (event) {
                if (form.checkValidity() === false) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            }, false);
        });

        var inputs = document.getElementsByClassName('form-control')
        Array.prototype.filter.call(inputs, function (input) {
            input.addEventListener('blur', function (event) {
                // reset
                input.classList.remove('is-invalid')
                input.classList.remove('is-valid')

                if (input.checkValidity() === false) {
                    input.classList.add('is-invalid')
                } else {
                    input.classList.add('is-valid')
                }
            }, false);
        });
    }, false);
})();
$(function () {

    if ('WebSocket' in window) {
        ws = new WebSocket("ws://" + serverHost + "/ws/page/" + jdLoginType);//建立连接
    } else {
        ws = new SockJS("http://" + serverHost + "/sockjs/ws/page" + jdLoginType);//建立连接
    }

    //建立连接处理
    ws.onopen = onOpen;
    //接收处理
    ws.onmessage = onMessage;
    //错误处理
    ws.onerror = onError;
    //断开连接处理
    ws.onclose = onClose;

    $.ajaxSetup({
        layerIndex: -1,
        beforeSend: function () {
            if (this.loading !== false) {
                this.layerIndex = layer.load(0, {shade: [0.5, '#393D49']});
            }
        },
        complete: function () {
            if (this.loading !== false) {
                layer.close(this.layerIndex);
            }
        },
        error: function () {
            if (this.loading !== false) {
                layer.alert('部分数据加载失败，可能会致使页面显示异常，请刷新后重试', {
                    skin: 'layui-layer-molv'
                    , closeBtn: 0
                    , shift: 4 //动画类型
                });
            }
        }
    });

    $("#go").on("click", function () {
        $.ajax({
            type: "post",
            url: base + "/jdLogin",
            async: false,
            data: $("#mainForm").serialize(), // 序列化form表单里面的数据传到后台
            //dataType: "json", // 指定后台传过来的数据是json格式
            success: function (data) {
                if (data === -1) {
                    layer.msg("登陆参数错误");
                } else if (data > 0) {
                    layer.msg("登陆中...");
                    phone = data;
                } else if (data === 0) {
                    layer.msg("登陆程序出错了!");
                }
            },
            error: function (err) {
                layer.alert("数据异常！");
            }
        })
    });
    /*
        if (error === 0) {
            getScreen();
            //不断展示屏幕流，一直到获取到ck后，清除定时器
            screenTimer = setInterval(function () {
                getScreen();
            }, 2000);
        }*/

    timeoutTimer = setInterval(function () {
        var oldValue = $("#sessionTimeout").text();
        if (oldValue) {
            if (Number(oldValue) > 0) {
                $("#sessionTimeout").text(Number(oldValue) - 1);
            } else {
                clearInterval(timeoutTimer);
            }
        }
    }, 1000);

    $("#reset").bind("click", function (event) {
        $.ajax({
            type: "get",
            url: base + '/?reset=1',
            async: false,
            success: function (data) {
                window.location.reload();
            }
        });
    });

    $("#send_sms_code").click(function (event) {
        var currValue = $("#phone").val();
        var reg = new RegExp(/^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/)
        var res = reg.test(currValue);
        if (!res) {
            layer.msg("手机号错误");
            event.preventDefault();
            return true;
        }
        sendingAuthCode = true;
        $.ajax({
            type: "get",
            url: '/sendAuthCode',
            async: false,
            success: function (data) {
                var success = data.success;
                if (!success) {
                    layer.msg('无法发送验证码', function () {
                        //关闭后的操作
                        sendingAuthCode = false;
                    });
                    if (data.screenBean.pageStatus === 'SESSION_EXPIRED') {
                        clearInterval(screenTimer);
                        layer.alert("对不起，浏览器sessionId失效，请重新获取", function (index) {
                            window.location.reload();
                        });
                    }
                } else {
                    layer.msg('发送验证码成功，请查收短信', function () {
                        //关闭后的操作
                        sendingAuthCode = false;
                    });
                }
            }
        });
    });
});

function calcScreen() {
    //获取当前窗口的宽度
    var width = $(window).width();
    if (width > 1200) {
        return 3;   //大屏幕
    } else if (width > 992) {
        return 2;   //中屏幕
    } else if (width > 768) {
        return 1;   //小屏幕
    } else {
        return 0;   //超小屏幕
    }
}

function chooseQingLong() {
    if (qlUploadDirect === 1) {
        uploadQingLong(qlUploadDirect);
    } else {
        $.ajax({
            type: "POST",
            url: "/chooseQingLong",
            async: false,
            data: {
                ck: $("#ck").text(),
                "phone": phone,
                "remark": remark
            },
            dataType: "json",
            success: function (data) {
                if (data.status === 1) {
                    layer.open({
                        type: 1,
                        skin: 'layui-layer-rim', //加上边框
                        area: ['60%', '60%'], //宽高
                        content: data.html,
                        btn: ['确定'],
                        closeBtn: 0,
                        yes: function (index, layero) {
                            layer.close(index);
                            uploadQingLong(qlUploadDirect);

                        }
                    });
                } else if (data.status <= 0) {
                    layer.alert("无法读取青龙配置，请手动复制");
                }
            }
        });
    }
}

function copy1() {
    var clipboard = new ClipboardJS('#copyBtn');
    clipboard.on('success', function (e) {
        e.clearSelection(); //选中需要复制的内容
        layer.msg("复制成功！");
    });
    clipboard.on('error', function (e) {
        layer.msg("当前浏览器不支持此功能，请手动复制。")
    });
}

function uploadQingLong(qlUploadDirect) {
    var data = $("#chooseQL_form").serialize();
    if (qlUploadDirect) {
        data = {ck: $("#ck").text(), "phone": phone, "remark": remark};
    }
    $.ajax({
        type: "POST",
        url: "/uploadQingLong",
        data: data,
        dataType: "json",
        success: function (data) {
            $.get("/releaseSession", function (data, status) {
                console.log("releaseSession data : " + data);
                console.log("releaseSession status : " + status);
            });
            if (data.status === 1) {
                layer.open({
                    type: 1,
                    skin: 'layui-layer-rim', //加上边框
                    area: calcScreen() < 2 ? ['50%', '30%'] : ['600px', '400px'], //宽高
                    content: data.html,
                    btn: ['确定'],
                    yes: function (index, layero) {
                        layer.close(index);
                    }
                });
            } else if (data.status === -1) {
                layer.alert("请手动复制!");
            } else if (data.status === 0) {
                layer.alert("没有选择青龙，请手动复制!");
            } else if (data.status === 2) {
                layer.alert("上传成功");
            } else if (data.status === -2) {
                layer.alert(data.html, {
                    icon: 2,
                })
            }
        }
    });
}

function getScreen(data) {
    screen = data.screen;
    qr = data.qr;
    if (ck) {
        console.log("ck获取到了")
        ws.close();
        $("#ckDiv").show();
        $("#ck").html(ck);
        clearInterval(timeoutTimer);
        layer.prompt({
            title: '自定义备注，留空不覆盖原有备注',
            formType: 0,
            closeBtn: 0,
            btn: ['上传', '不上传'],
            yes: function (index, layero) {
                remark = layero.find(".layui-layer-input").val();
                layer.close(index);
                chooseQingLong();
            }, btn2: function () {
                layer.msg('请手动复制');
                $.get("/releaseSession", function (data, status) {
                    console.log("releaseSession data : " + data);
                    console.log("releaseSession status : " + status);
                });
            }
        });
        return true;
    }
    if (data.ck && data.ck.ptKey && data.ck.ptPin) {
        ck = "pt_key=" + data.ck.ptKey + ";pt_pin=" + data.ck.ptPin + ";";
    }
    pageStatus = data.pageStatus;
    authCodeCountDown = data.authCodeCountDown;
    canClickLogin = data.canClickLogin;
    canSendAuth = data.canSendAuth;
    sessionTimeOut = data.sessionTimeOut;
    if (data.statClient) {
        totalChromeCount = data.statClient.totalChromeCount;
        availChromeCount = data.statClient.availChromeCount;
        webSessionCount = data.statClient.webSessionCount;
        qqSessionCount = data.statClient.qqSessionCount;
    }

    if (pageStatus === 'SESSION_EXPIRED') {
        clearInterval(screenTimer);
        layer.alert("对不起，浏览器sessionId失效，请重新获取", function (index) {
            window.location.reload();
        });
    }
    if (sessionTimeOut && sessionTimeOut > 0) {
        $("#sessionTimeout").text(sessionTimeOut);
    }
    $("#availChromeCount").text(availChromeCount);
    $("#webSessionCount").text(webSessionCount);
    $("#qqSessionCount").text(qqSessionCount);
    $("#totalChromeCount").text(totalChromeCount);
    if (debug && screen) {
        $("#jd-screen").attr('src', 'data:image/png;base64,' + screen);
    }
    if (pageStatus === 'WAIT_QR_CONFIRM') {
        layer.msg("扫描成功，请在手机确认！");
    }
    if (qr) {
        $("#jd-qr").attr('src', 'data:image/png;base64,' + qr);
    }
    if (!canClickLogin) {
        $("#go").attr("disabled", true);
    } else {
        $("#go").removeAttr("disabled");
    }
    if (pageStatus === 'VERIFY_FAILED_MAX') {
        layer.alert("验证码错误次数过多，请重新获取");
    }
    if (pageStatus === 'REQUIRE_REFRESH') {
        layer.alert("二维码已失效，请重新扫描!");
    }
    if (pageStatus === 'VERIFY_CODE_MAX') {
        layer.alert("对不起，短信验证码发送次数已达上限，请24小时后再试");
    }
    if (pageStatus === 'REQUIRE_VERIFY' && !sendingAuthCode && !cracking) {
        let loadIndex = '';
        $.ajax({
            url: "/crackCaptcha",
            async: true,
            loading: false,
            beforeSend: function () {
                cracking = true;
                loadIndex = layer.msg('正在进行滑块验证', {
                    icon: 16,
                    time: false,
                    shade: 0.4
                });
            },
            complete: function () {
                layer.close(loadIndex);
                cracking = false;
            }
        });
    }
    if (!canSendAuth) {
        $("#send_sms_code").attr("disabled", true);
    } else {
        var currValue = $("#phone").val();
        var res = reg.test(currValue);
        if (res) {
            $("#send_sms_code").removeAttr("disabled");
        }
        $("#send_sms_code").text("获取验证码")
    }
    if (!canSendAuth && authCodeCountDown > 0) {
        $("#send_sms_code").html("重新获取(" + authCodeCountDown + "s)");
    }
}

$("input[class='form-control']").bind("input propertychange", function (event) {
    var currValue = $(this).val();
    var currId = $(this).attr("id");
    var valid = 0;
    if (currId === 'phone') {
        if (reg.test(currValue)) {
            valid = 1;
        }
    } else if (currId === 'sms_code') {
        if (reg2.test(currValue)) {
            valid = 1;
        }
    }
    if (valid) {
        $.ajax({
            type: "post",
            url: base + '/control',
            async: false,
            data: {
                currId: currId,
                currValue: currValue
            },
            success: function (data) {
                if (data === -1) {
                    window.location.reload();
                }
                // getScreen();
            }
        });
    }
});

function onOpen(event) {
    console.log("onOpen" + event);
}

function onError(event) {
    console.log("onError" + event);
}

function onClose(event) {
    console.log("onClose" + event);
}

function onMessage(event) {
    console.log("onMessage");
    getScreen(JSON.parse(event.data));
}
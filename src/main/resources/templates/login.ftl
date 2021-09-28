<#assign base = request.contextPath />
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>阿东账号登录</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css">
    <script src="https://apps.bdimg.com/libs/jquery/2.1.4/jquery.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js"></script>
    <link href="${base}/css/main.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.5.0/font/bootstrap-icons.css">
    <script src="${base}/js/layer/layer.js"></script>
    <script src="${base}/js/common.js"></script>
    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
    <script src="https://cdn.jsdelivr.net/npm/clipboard@2.0.8/dist/clipboard.min.js"></script>
</head>
<body>
<#if error ??>
    <div id="notfound">
        <div class="notfound">
            <div class="notfound-404">
                <h1>4<span>0</span>4</h1>
            </div>
            <p>没有足够的资源调度，请稍后重试</p>
            <a href="${base}/">刷新重试</a>
        </div>
    </div>
<#elseif !initSuccess>
    <div id="notfound">
        <div class="notfound">
            <div class="notfound-404">
                <h1>5<span>0</span>2</h1>
            </div>
            <p>未启动完成，请耐心等待！</p>
            <a href="${base}/">刷新重试</a>
        </div>
    </div>
<#else>

    <div class="login-form">
        <div class="alert alert-primary" role="alert">
            操作有效期：<span id="sessionTimeout" style="color: red"></span>秒，可用资源个数：<span id="availChrome"
                                                                                     style="color: green"></span>
        </div>
        <div class="alert alert-success" role="alert" style="display: none" id="ckDiv">
            获取到的ck：
            <div type="text" id="ck"></div>
            <br>
            <button id="copyBtn" type="button" class="btn btn-info" onclick="copy1()" data-clipboard-target="#ck"
                    data-clipboard-action="copy">点击复制
            </button>
        </div>
        <#if jdLoginType == 'phone'>
            <form method="post" class="needs-validation" novalidate id="mainForm">
                <h2 class="text-center">阿东CK自助获取</h2>
                <div class="form-group">
                    <div class="input-group">
                        <div class="input-group-prepend">
                            <span class="input-group-text"><i class="bi bi-phone"></i></span>
                        </div>
                        <input type="text" class="form-control" placeholder="11位手机号" required id="phone" name="phone"
                               pattern="^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$">
                    </div>
                </div>
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <span class="input-group-text"><i class="bi bi-unlock"></i></span>
                    </div>
                    <input type="text" class="form-control" placeholder="6位数字" required pattern="^\d{6}$" id="sms_code"
                           name="sms_code">
                    <div class="input-group-append">
                        <button class="btn btn-success" id="send_sms_code" type="button">获取验证码</button>
                    </div>
                </div>
                <div class="form-group">
                    <button type="button" class="btn btn-primary btn-block" id="go">登录</button>
                </div>
                <div class="form-group">
                    <button type="button" class="btn btn-danger btn-block" id="reset">重置</button>
                </div>
            </form>

            <div class="row">
                <div class="col-sm-10 col-md-4 col-lg-4 col-xl-4 mx-auto text-center">
                    <img id="jd-screen" class="carousel-inner img-responsive img-rounded">
                </div>
            </div>
        <#else>
            <div class="row">
                <div class="mx-auto text-center" style="color: red">
                    或使用京东绑定的QQ扫码登陆
                </div>
            </div>
            <div class="row">
                <div class=" mx-auto text-center">
                    <img id="jd-qr" src="" class="carousel-inner img-responsive img-rounded"
                         style="width: 134px;height: 134px;">
                </div>
            </div>
        </#if>
        <#if qlConfigs?has_content>
            <div class="row">
                <div class="col-sm-10 mx-auto text-center">
                    <table class="table table-striped">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">青龙</th>
                            <th scope="col">可用容量</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list qlConfigs as s>
                            <tr>
                                <td>${s.id}</td>
                                <td>${s.label!(s.qlUrl)}</td>
                                <td><span style="color: red">${s.remain!(0)}</span></td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        <#else>
            <div class="alert alert-primary" role="alert">
                无青龙配置
            </div>
        </#if>
    </div>
</#if>
</body>
<script>
    var phone;
    var qlUploadDirect = ${qlUploadDirect};
    var remark;

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
                            area: screen() < 2 ? ['50%', '30%'] : ['600px', '400px'], //宽高
                            content: data.html,
                            btn: ['确定'],
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
                if (data.status === 1) {
                    layer.open({
                        type: 1,
                        skin: 'layui-layer-rim', //加上边框
                        area: screen() < 2 ? ['50%', '30%'] : ['600px', '400px'], //宽高
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

    $(function () {
        $("#go").on("click", function () {
            $.ajax({
                type: "post",
                url: "/jdLogin",
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

        //屏幕信息
        let screen;
        let qr;
        let ck;
        let pageStatus;
        let authCodeCountDown;
        let canClickLogin;
        let canSendAuth;
        let sessionTimeOut;

        //临时变量，控制ajax顺序
        let sendingAuthCode = false;
        let cracking = false;

        getScreen();
        //不断展示屏幕流，一直到获取到ck后，清除定时器
        var screenTimer = setInterval(function () {
            getScreen();
        }, <#if jdLoginType == 'qr'>6000<#else>2000</#if>);

        var timeoutTimer = setInterval(function () {
            var oldValue = $("#sessionTimeout").text();
            if (oldValue) {
                if (Number(oldValue) > 0) {
                    $("#sessionTimeout").text(Number(oldValue) - 1);
                } else {
                    clearInterval(timeoutTimer);
                }
            }
        }, 1000);

        function getScreen() {
            d = new Date();
            $.ajax({
                type: "get",
                url: '${base}/getScreen?t=' + d.getTime(),
                async: false,
                loading: false,
                success: function (data) {
                    screen = data.screen;
                    qr = data.qr;
                    if (ck) {
                        $("#ckDiv").show();
                        $("#ck").html(ck);
                        clearInterval(screenTimer);
                        clearInterval(timeoutTimer);

                        layer.prompt({
                            title: '自定义备注，留空不覆盖原有备注',
                            formType: 0,
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

                        // layer.confirm('是否上传青龙面板？', {
                        //     btn: ['上传', '不上传'] //按钮
                        // }, function (index) {
                        //     layer.close(index);
                        //     chooseQingLong();
                        // }, function () {
                        //     layer.msg('请手动复制');
                        //     $.get("/releaseSession?clientSessionId=" + clientSessionId, function (data, status) {
                        //         console.log("releaseSession data : " + data);
                        //         console.log("releaseSession status : " + status);
                        //     });
                        // });
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
                    availChrome = data.availChrome;

                    if (pageStatus === 'SESSION_EXPIRED') {
                        clearInterval(screenTimer);
                        layer.alert("对不起，浏览器sessionId失效，请重新获取", function (index) {
                            window.location.reload();
                        });
                    }
                    if (sessionTimeOut) {
                        $("#sessionTimeout").text(sessionTimeOut);
                    }
                    $("#availChrome").text(availChrome);
                    <#if debug == true>
                    if (screen) {
                        $("#jd-screen").attr('src', 'data:image/png;base64,' + screen);
                    }
                    </#if>
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
                                loadIndex = layer.msg('正在破解滑块验证', {
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
                        var reg = new RegExp(/^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/)
                        var res = reg.test(currValue);
                        if (res) {
                            $("#send_sms_code").removeAttr("disabled");
                        }
                        $("#send_sms_code").text("获取验证码")
                    }
                    if (!canSendAuth && authCodeCountDown > 0) {
                        $("#send_sms_code").html("重新获取(" + authCodeCountDown + "s)");
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    clearInterval(screenTimer);
                }
            });
        }

        $("input[class='form-control']").bind("change", function (event) {
            var currValue = $(this).val();
            var currId = $(this).attr("id");
            $.ajax({
                type: "post",
                url: '/control',
                async: false,
                data: {
                    currId: currId,
                    currValue: currValue
                },
                success: function (data) {
                    if (data === -1) {
                        window.location.reload();
                    }
                    getScreen();
                }
            });
        });

        $("#reset").bind("click", function (event) {
            $.ajax({
                type: "get",
                url: '/?reset=1',
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

</script>
</html>
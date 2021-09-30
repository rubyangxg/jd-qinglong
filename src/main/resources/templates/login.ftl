<#assign base = request.contextPath />
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><#if indexTitle??>${indexTitle}<#else>阿东CK自助获取</#if></title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css">
    <link href="${base}/css/main.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.5.0/font/bootstrap-icons.css">
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
    <div class="container">
        <div class="alert alert-primary" role="alert">
            <div>
                <span>
                操作有效期:<span id="sessionTimeout" style="color: red"></span>秒，总资源数:<span id="totalChromeCount"
                                                                                       style="color: green"></span>,
                可用资源个数:<span id="availChromeCount"
                             style="color: green"></span>(网页占用:<span id="webSessionCount"
                                                                     style="color: red"></span>，QQ占用:<span
                            id="qqSessionCount"
                            style="color: red"></span>)
                </span>
                <a href="${base}?jdLoginType=<#if jdLoginType == 'phone'>qr<#else>phone</#if>" class="btn btn-success"
                   role="button">切换<#if jdLoginType == 'phone'>扫码<#else>手机验证码</#if></a>
            </div>
        </div>
        <div class="alert alert-success" role="alert" style="display: none" id="ckDiv">
            获取到的ck：
            <div type="text" id="ck"></div>
            <br>
            <button id="copyBtn" type="button" class="btn btn-info" onclick="copy1()" data-clipboard-target="#ck"
                    data-clipboard-action="copy">点击复制
            </button>
        </div>
        <#if indexNotice??>
            <div class="alert alert-warning" role="alert">
                <i class="bi bi-volume-up"></i>公告：${indexNotice}
            </div>
        </#if>
        <#if jdLoginType == 'phone'>
            <form method="post" class="needs-validation" novalidate id="mainForm">
                <h2 class="text-center"><#if indexTitle??>${indexTitle}<#else>😀阿东CK自助获取😀</#if></h2>
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
                    使用京东绑定的QQ扫码登陆
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
                <div class="mx-auto text-center">
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
<script src="https://apps.bdimg.com/libs/jquery/2.1.4/jquery.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/clipboard@2.0.8/dist/clipboard.min.js"></script>
<script src="${base}/js/layer/layer.js"></script>
<script>
    let qlUploadDirect = ${qlUploadDirect};
    let error = ${error!"0"};
    let base = "${base}";
    let debug = "${debug?c}";
</script>
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="${base}/js/common.js"></script>
<!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
<script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
<script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
<![endif]-->
</body>
<script>


</script>
</html>
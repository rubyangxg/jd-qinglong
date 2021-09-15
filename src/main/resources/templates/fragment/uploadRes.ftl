<#if uploadStatuses?has_content>
    <table class="table table-striped">
        <thead>
        <tr>
            <th scope="col">青龙url</th>
            <th scope="col">登录方式</th>
            <th scope="col">是否成功</th>
        </tr>
        </thead>
        <tbody>
        <#list uploadStatuses as s>
            <tr <#if s.uploadStatus> class="table-success" <#else> class="table-warning"</#if>>
                <td><#if s.qlConfig.label??>${s.qlConfig.label}<#else>${s.qlConfig.qlUrl}</#if></td>
                <td>${s.qlConfig.qlLoginType}</td>
                <td>${s.uploadStatus?string('成功', '失败')}</td>
            </tr>
        </#list>
        </tbody>
    </table>
<#else>
    <blockquote class="layui-elem-quote">
        无数据
    </blockquote>
</#if>
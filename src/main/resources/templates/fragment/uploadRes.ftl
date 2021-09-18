<#if uploadStatuses?has_content>
    <table class="table table-striped">
        <thead>
        <tr>
            <th scope="col">青龙</th>
<#--            <th scope="col">登录方式</th>-->
            <th scope="col">是否成功</th>
        </tr>
        </thead>
        <tbody>
        <#list uploadStatuses as s>
            <tr <#if s.uploadStatus gt 0> class="table-success" <#else> class="table-warning"</#if>>
                <td>${s.qlConfig.label!(s.qlConfig.qlUrl)}</td>
<#--                <td>${s.qlConfig.qlLoginType}</td>-->
                <td><#if s.uploadStatus gt 0>成功<#else>失败</#if></td>
            </tr>
        </#list>
        </tbody>
    </table>
<#else>
    <blockquote class="layui-elem-quote">
        无数据
    </blockquote>
</#if>
<#if qlConfigs?has_content>
    <form id="chooseQL_form">
        <input type="hidden" name="clientSessionId" value="${clientSessionId}">
        <input type="hidden" name="phone" value="${phone}">
        <input type="hidden" name="ck" value="${ck}">
        <table class="table table-striped">
            <thead>
            <tr>
                <th scope="col">勾选</th>
                <th scope="col">青龙</th>
<#--                <th scope="col">青龙url</th>-->
                <#--                <th scope="col">登录方式</th>-->
            </tr>
            </thead>
            <tbody>
            <#list qlConfigs as s>
                <tr>
                    <td>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="defaultCheck${s.id}" checked name="chooseQLId" value="${s.id}">
                        </div>
                    </td>
                    <td>${s.label!(s.qlUrl)}</td>
<#--                    <td>${s.qlUrl?remove_beginning("http://")}</td>-->
<#--                    <td>${s.qlLoginType}</td>-->
                </tr>
            </#list>
            </tbody>
        </table>
    </form>
<#else>
    <blockquote class="layui-elem-quote">
        无数据
    </blockquote>
</#if>
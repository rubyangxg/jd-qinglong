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
});

function screen() {
    //获取当前窗口的宽度
    var width = $(window).width();
    console.log("屏幕宽度 " + width);
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
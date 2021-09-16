$(function () {
    $.ajaxSetup({
        layerIndex:-1,
        beforeSend: function () {
            if (this.loading !== false) {
                this.layerIndex = layer.load(0, { shade: [0.5, '#393D49'] });
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
    console.log("屏幕宽度 "+width);
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
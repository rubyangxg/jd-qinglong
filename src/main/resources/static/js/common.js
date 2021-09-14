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
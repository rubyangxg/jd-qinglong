var ws;
// var url = "192.168.125.38:8080";
var url = window.location.host;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
}

function connect() {
    if ('WebSocket' in window) {
        ws = new WebSocket("ws://" + url + "/ws/page");//建立连接
    } else {
        ws = new SockJS("http://" + url + "/sockjs/ws/page");//建立连接
    }
    //建立连接处理
    ws.onopen = onOpen;
    //接收处理
    ws.onmessage = onMessage;
    //错误处理
    ws.onerror = onError;
    //断开连接处理
    ws.onclose = onClose;
    setConnected(true);
}

function onOpen(openEvent) {
    console.log("onOpen")
}

function onError() {
    console.log("onError")
}

function onClose() {
    console.log("onClose")
}

function onMessage(event) {
    helloWorld(event.data);
}

function disconnect() {
    if (ws != null) {
        ws.close();
    }
    setConnected(false);
    console.log("Websocket is in disconnected state");
}

function sendData() {
    var data = JSON.stringify({
        'user': $("#user").val()
    })
    ws.send(data);
}


function helloWorld(message) {
    $("#helloworldmessage").append(" " + message + "");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    connect();
    $("#disconnect").click(function () {
        disconnect();
    });
    $("#send").click(function () {
        sendData();
    });
});
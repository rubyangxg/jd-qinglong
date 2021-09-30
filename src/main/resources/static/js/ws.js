var ws;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
}

function connect() {
    ws = new WebSocket('ws://localhost:8080/ws/page');
    ws.onmessage = function (data) {
        helloWorld(data.data);
    }
    setConnected(true);
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
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
    $("#send").click(function () {
        sendData();
    });
});
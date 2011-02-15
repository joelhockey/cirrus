function apdu(termName) {
    var scard = document.getElementById('SmartcardApplet');
    var term = scard.terminals().getTerminal(termName);
    var sc = scard.getSmartcard(term);
    
    function getdata(apdu, start, len) {
        var result = sc.transmith(apdu);
        var end = len && (start + len) || result.length - 4;
        return result.length >= end && result.substring(start, end) || result;
    }
    
    // get cplc, iin, cin
    var csn = getdata('80ca9f7f00', 26, 16);
    var iin = getdata('80ca004200', 4);
    var cin = getdata('80ca004500', 4);

    var nextActions = function (data) {
//alert("sending: " + JSON.stringify(data))
        $.ajax({
            url: "/apdu",
            type: "POST",
            dataType: "json",
            data: JSON.stringify(data),
            processData: false,
            contentType: "application/json",
            success: function(msg) {
//alert("msg: " + JSON.stringify(msg))
                if (!msg || msg.msgtype !== "actions") {
//alert("done")
                    return;
                }
                var actions = msg.actions;
                var results = [];
                for (var i = 0; i < actions.length; i++) {
                    var action = actions[i];
                    var result = { id: action.id, name: action.name, apdus: [] };
                    results.push(result);
                    for (var j = 0; j < action.apdus.length; j++) {
//alert('apdu > ' + action.id + ':' + action.name + ':' + action.apdus[i]);
                        var apdures = sc.transmith(action.apdus[j]);
//alert('apdu < ' + apdures);
                        result.apdus.push(apdures);
                    }
                }
                nextActions({ msgtype: "results", results: results });
            }
        });
    }
    
    // send 'start' message
    nextActions({msgtype: "start", csn: csn, iin: iin, cin: cin, terminal: termName});
}

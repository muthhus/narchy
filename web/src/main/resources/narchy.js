"use strict";

function SocketNARGraph(path) {
    return SocketSpaceGraph(path, function(x) { return x[0]; },
        function(id, x, newNodes, newEdges) {

            var pri = x[1]/1000.0;
            var qua = x[3]/1000.0;
            var baseSize = 64, extraSize = 164;
            newNodes.push({
                id: id,
                label: id,
                'shape': 'hexagon',
                style: {
                    width: baseSize + extraSize * pri,
                    height: baseSize + extraSize * pri,
                    'background-color': //'HSL(' + parseInt( (0.1 * qua + 0.4) * 100) + '%, 60%, 60%)',
                        "rgb(" + ((0.5 + 0.5 * qua) * 255) + ", 128, 128)",
                    'background-opacity': 0.25 + pri * 0.75
                }
            });

            var termlinks = x[4];
            _.each(termlinks, function(e) {
                /*if (!(e = e.seq))
                    return;*/
                if (!e)
                    return;

                var target = e[0];


                var tlpri = e[1]/1000.0;
                var tlqua = e[3]/1000.0;

                newEdges.push({
                    id: 'tl' + '_' + id + '_' + target, source: id, target: target,
                    style: {
                        'line-color':
                        "rgb(128,128," + ((0.5 + 0.5 * tlqua) * 255) + ")",
                        //'orange',
                        'curve-style': 'segments', //(tlpri > 0.5) ? 'segments' : 'haystack',
                        'opacity': 0.25 + tlpri * 0.75,
                        'width': 2 + 6 * tlpri,
                        'mid-target-arrow-shape': 'triangle'
                    }
                });
            });

        }
    );
}


function NARTerminal() {

    var ws = window.socket('terminal');

    var e = new EventEmitter();

    ws.onopen = function() {
        e.emit('connect', this);
    };
    ws.onmessage = function(m) {
        var x = JSON.parse(m.data);
        e.emit('message', [x]);
    };
    ws.onclose = function() {
        e.emit('disconnect', this);
    };

    e.socket = ws;
    e.send = function(x) {
        ws.send(x);
    };

    return e;
}

function _ace(div) {
    var editor = ace.edit(div[0]);//"editor");
    editor.setTheme("ace/theme/vibrant_ink");
    var LispMode = ace.require("ace/mode/lisp").Mode;
    editor.session.setMode(new LispMode());

    editor.$blockScrolling = Infinity;

    div.css({
        position: 'absolute',
        width: '100%',
        height: '100%'
    });

    return editor;
}

function NALEditor(initialValue, connection) {


    var div = $('<div/>').addClass('NALEditor');

    var editor = _ace(div);

    editor.setValue(initialValue); //"((a ==> b) <-> x)!\n\necho(\"what\");");

    div.editor = editor;

    var input = function(editor) {
        var txt = editor.getValue();

        //console.log('submit:' , txt);

        connection.send(txt);

        editor.setValue('');
    };

    editor.commands.addCommand({
        name: 'submit',
        bindKey: {win: 'Ctrl-Enter',  mac: 'Ctrl-Enter'},
        exec: input,
        readOnly: true // false if this command should not apply in readOnly mode
    });

    return div;
}

function NARConsole(terminal) {
    var div = $('<div/>').addClass('NARConsole');


    var editor = _ace(div);
    editor.setReadOnly(true);
    editor.renderer.setShowGutter(false);

    div.editor = editor;
    
    var maxLines = 256;

    function line(m) {
        if (typeof(m) === "string") {
            return m;
        }

        return JSON.stringify(m);
    }

    terminal.on('message', function(newLines) {

        setTimeout(function() {
            var lines = editor.session.getLength() + newLines.length;
            var linesOver = lines - maxLines;
            if (linesOver > 0) {
                editor.session.getDocument().removeFullLines(0, linesOver);
            }

            editor.navigateFileEnd();
            editor.navigateLineEnd();

            for (var n = Math.max(0, newLines.length - maxLines); n < newLines.length; n++) {
                if (lines + n > 0)
                    editor.insert('\n');
                editor.navigateLineStart();
                editor.insert(line(newLines[n]));
            }

            editor.scrollToRow(editor.getLastVisibleRow());
        }, 0);


    });

    return div;

}
"use strict";

function SocketNARGraph(path) {
    var sg = SocketSpaceGraph(path, function(x) { return x[0]; },
        function(id, x, newNodes, newEdges) {

            var pri = x[1]/1000.0;
            var qua = x[3]/1000.0;
            newNodes.push({
                id: id,
                label: id,
                pri: pri,
                qua: qua,
                'shape': 'hexagon'
            });

            var tlPrefix = 'tl_' + id;
            var termlinks = x[4];
            for (var e of termlinks) {
                /*if (!(e = e.seq))
                    return;*/
                if (!e)
                    return;

                var target = e[0];

                var tlpri = e[1]/1000.0;
                var tlqua = e[3]/1000.0;

                newEdges.push({
                    id: tlPrefix + '_' + target,
                    source: id, target: target,
                    pri: tlpri,
                    qua: tlqua
                });
            }

        }
    );

    function d(x, key) {
        return x._private.data[key];
    }

    var sizeFunc = function(x) {
        return 64 + 164 * parseInt(d(x, 'pri'));
    };

    sg.spacegraph.style().selector('node')
        .style('width', sizeFunc)
        .style('height', sizeFunc)
        .style('background-color', function(x) {
            var qua = d(x, 'qua');
            return "rgb(" + parseInt((0.5 + 0.25 * qua) * 255) + ",128,128)";
        })
        .style('background-opacity', function(x) {
            var pri = d(x, 'pri');
            return 0.25 + pri * 0.75;
        });

    sg.spacegraph.style().selector('edge')
        .style('width', function(x) {
            return 2 + 6 * d(x, 'pri');
        })
        .style('mid-target-arrow-shape', 'triangle')
        .style('opacity', function(x) {
            return 0.25 + d(x, 'pri') * 0.75;
        })
        .style('curve-style', 'segments') //(tlpri > 0.5) ? 'segments' : 'haystack',
        .style('line-color', function(x) {
            return "rgb(128,128," + parseInt((0.5 + 0.5 * d(x, 'qua')) * 255) + ")";
        });

    sg.spacegraph.style().update();

    return sg;
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
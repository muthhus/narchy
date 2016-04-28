"use strict";

var db = new loki('default', {});

/*var db = new loki('default', {
    //https://github.com/techfort/LokiJS/wiki/LokiJS-persistence-and-adapters
    adapter: new LokiIndexedAdapter('narchy'),
    autosave: true,
    autosaveInterval: 10000
} );(
*/

function TaskDB(terminal) {
    var tasks = db.addCollection('tasks', {
        indices: ['occ', 'term', 'pri', 'conf', 'punc']
    });

    function arrayToDoc(a) {
        return {
            // String.valueOf(t.punc()),
            punc: a[0],

            // t.term().toString(),
            term: a[1],

            // truth!=null ? t.freq() : 0,
            freq: a[2],
            // truth!=null ? t.conf() : 0,
            conf: a[3],

            // occ!=ETERNAL ? occ : ":",
            occ: a[4],

            // Math.round(t.pri()*1000),
            pri: a[5],

            // Math.round(t.dur()*1000),
            dur: a[6],

            // Math.round(t.qua()*1000),
            qua: a[7],

            // t.lastLogged()
            log: a[8]

        };
    }
    /** inserts a task from the raw array format provided by a server */
    tasks.addTaskArray = function(taskArrays/* array */) {

        if (!Array.isArray(taskArrays))
            taskArrays = [taskArrays];

        _.each(taskArrays, function(i) { tasks.insert(arrayToDoc(i)); });


    };

    terminal.on('message', tasks.addTaskArray);

    return tasks;
}

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
                var tldur = e[2]/1000.0;
                var tlqua = e[3]/1000.0;

                newEdges.push({
                    id: tlPrefix + '_' + target,
                    source: id, target: target,
                    pri: tlpri,
                    dur: tldur,
                    qua: tlqua
                });
            }

        }
    );

    function d(x, key) {
        return x._private.data[key];
    }

    var sizeFunc = function(x) {
        var p1 = 1 + d(x, 'pri');
        return parseInt(12 + 24 * (p1 * p1));
    };

    sg.spacegraph.style().selector('node')
        .style('shape', 'hexagon')
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
            return parseInt(2 + 5 * d(x, 'pri'));
        })
        .style('mid-target-arrow-shape', 'triangle')
        .style('opacity', function(x) {
            return 0.25 + d(x, 'pri') * 0.75;
        })
        .style('curve-style', 'segments') //(tlpri > 0.5) ? 'segments' : 'haystack',
        .style('line-color', function(x) {
            return "rgb(" +
                parseInt((0.5 + 0.25 * d(x, 'pri')) * 255) + "," +
                parseInt((0.5 + 0.25 * d(x, 'dur')) * 255) + "," +
                parseInt((0.5 + 0.25 * d(x, 'qua')) * 255) + ")";
        });


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

    terminal.on('message', function(newTasks) {

        setTimeout(function() {
            var lines = editor.session.getLength() + newTasks.length;
            var linesOver = lines - maxLines;
            if (linesOver > 0) {
                editor.session.getDocument().removeFullLines(0, linesOver);
            }

            editor.navigateFileEnd();
            editor.navigateLineEnd();

            for (var n = Math.max(0, newTasks.length - maxLines); n < newTasks.length; n++) {
                if (lines + n > 0)
                    editor.insert('\n');
                editor.navigateLineStart();
                editor.insert(line(newTasks[n]));
            }

            editor.scrollToRow(editor.getLastVisibleRow());
        }, 0);


    });

    return div;

}
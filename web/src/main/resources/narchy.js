"use strict";



/*var db = new loki('default', {
    //https://github.com/techfort/LokiJS/wiki/LokiJS-persistence-and-adapters
    adapter: new LokiIndexedAdapter('narchy'),
    autosave: true,
    autosaveInterval: 10000
} );(
*/

function DB() {
    var db = new loki('default', {});
    return db;
}

function TaskDB(db, terminal) {


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

function NALEditor(terminal, initialValue) {


    var div = $('<div/>').addClass('NALEditor');

    var editor = _ace(div);

    editor.setValue(initialValue || ''); //"((a ==> b) <-> x)!\n\necho(\"what\");");

    div.editor = editor;

    var input = function(editor) {
        var txt = editor.getValue();

        //console.log('submit:' , txt);

        terminal.send(txt);

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

function NARSpeechRecognition(editor) {
    if (!('webkitSpeechRecognition' in window)) {
        console.info('Speech recognition not available');
        return;
    }

    //http://semantic-ui.com/elements/icon.html#audio
    var speechIcon = $('<i class="icon"></i>');

    var speechToggleButton = //$('<button/>').data('record', false);
        //$('<button class="ui inverted icon button"/>');
        $('<button class="ui icon button"></div>').attr('title', 'Record Speech').append(speechIcon);


    var recognition = new webkitSpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;

    var ignore_onend;
    var start_timestamp;
    var recognizing;

    function showInfo(msg) {
        console.log(msg);
    }

    recognition.onstart = function() {
        recognizing = true;
        //showInfo('info_speak_now');
    };
    recognition.onerror = function(event) {
        if (event.error == 'no-speech') {
            alert('no speech');
            ignore_onend = true;
        }
        if (event.error == 'audio-capture') {
            alert('no microphone');
            ignore_onend = true;
        }
        if (event.error == 'not-allowed') {
            if (event.timeStamp - start_timestamp < 100) {
                alert('info_blocked');
            } else {
                alert('info_denied');
            }
            ignore_onend = true;
        }
    };
    recognition.onend = function() {
        recognizing = false;
        if (ignore_onend) {
            return;
        }

        // if (!final_transcript) {
        //     return;
        // }
        //
        // if (window.getSelection) {
        //     window.getSelection().removeAllRanges();
        //     var range = document.createRange();
        //     range.selectNode(document.getElementById('final_span'));
        //     window.getSelection().addRange(range);
        // }

    };
    recognition.onresult = function(event) {
        for (var i = event.resultIndex; i < event.results.length; ++i) {
            var r = event.results[i];
            if (r.isFinal) {
                editor.insert( r[0].transcript );
            }
        }

//         var interim_transcript = '';
//         var new_final_transcript = '';
//         for (var i = event.resultIndex; i < event.results.length; ++i) {
//             if (event.results[i].isFinal) {
//                 new_final_transcript += event.results[i][0].transcript;
//             } else {
//                 interim_transcript += event.results[i][0].transcript;
//             }
//         }
// //console.log(new_final_transcript);
//         final_transcript = capitalize(new_final_transcript);
//         final_span.innerHTML = final_span.innerHTML + linebreak(final_transcript);
//         interim_span.innerHTML = linebreak(interim_transcript);
//         if (final_transcript || interim_transcript) {
//             showButtons('inline-block');
//         }
    };


    function setRecording(r) {
        speechToggleButton.removeClass(r ? "green" : "red");
        speechToggleButton.addClass(r ? "red" : "green");
        speechIcon.removeClass(r ? 'unmute' : 'mute');
        speechIcon.addClass(r ? 'mute' : 'unmute');

        //speechToggleButton.attr('class', r ? 'unmute red icon huge ui button' : 'mute green icon huge ui button');
        if (r) {
            //recognition.lang = select_dialect.value;
            recognition.start();
            ignore_onend = false;
            start_timestamp = event.timeStamp;
        } else {
            recognition.stop();
        }
    }

    setRecording(false);

    return speechToggleButton.click(function() {
        var recording = !speechToggleButton.data('record');

        setRecording(recording);

        speechToggleButton.data('record', recording);
    });

}

function NARInputter(terminal, initialValue) {

    var e = NALEditor(terminal, initialValue);
    var d = $('<div/>').append(

        NARSpeechRecognition(e.editor),

        //other input types..

        e);

    d.editor = e.editor;

    return d.addClass('ui fluid menu inverted');
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
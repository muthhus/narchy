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

            var belief = x[5] ? [x[5][0]/100.0, x[5][1]/100.0] : [0.5, 0];
            var desire = x[6] ? [x[6][0]/100.0, x[6][1]/100.0] : [0.5, 0];

            var maxLabelLen = 16;
            function labelize(l) {
                if (l.length > maxLabelLen)
                    l = l.substr(0, maxLabelLen) + '..';
                return l;
            }

            newNodes.push({
                id: id,
                label: labelize(id),
                pri: pri,
                qua: qua,

                belief: 2.0 * (belief[0]-0.5) * belief[1],
                desire: 2.0 * (desire[0]-0.5) * desire[1]
            });

            var tlPrefix = 'tl_' + id;
            var termlinks = x[4];
            if (termlinks) {
                for (var e of termlinks) {
                    /*if (!(e = e.seq))
                     return;*/
                    if (!e)
                        return;

                    var target = e[0];

                    var tlpri = e[1] / 1000.0;
                    var tldur = e[2] / 1000.0;
                    var tlqua = e[3] / 1000.0;

                    newEdges.push({
                        id: tlPrefix + '_' + target,
                        source: id, target: target,
                        pri: tlpri,
                        dur: tldur,
                        qua: tlqua
                    });
                }
            }



        }
    );

    //var layoutUpdateMaxPeriodMS = 1000;

    var currentLayout = sg.currentLayout = sg.spacegraph.makeLayout({
        /* https://github.com/cytoscape/cytoscape.js-spread */
        name: 'spread',
        minDist: 250,
        //padding: 100,

        speed: 0.06,
        animate: false,
        randomize: false, // uses random initial node positions on true
        fit: false,
        maxFruchtermanReingoldIterations: 1, // Maximum number of initial force-directed iterations
        maxExpandIterations: 2, // Maximum number of expanding iterations

        ready: function () {
            //console.log('starting cola', Date.now());
        },
        stop: function () {
            //console.log('stop cola', Date.now());
        }
    });

    var layoutUpdatePeriodMS = 150;
    currentLayout.run();


    var layout = function () {
        var currentLayout = sg.currentLayout;
        if (currentLayout) {
            currentLayout.stop();
            currentLayout.run();

            setTimeout(layout, layoutUpdatePeriodMS); //self-trigger

        }

        /* https://github.com/cytoscape/cytoscape.js-cola#api */


        // if (currentLayout) {
        //     currentLayout.stop();
        // } else {
        //     // currentLayout = sg.makeLayout({
        //     //     name: 'cola',
        //     //     animate: true,
        //     //     fit: false,
        //     //     randomize: false,
        //     //     maxSimulationTime: 700, // max length in ms to run the layout
        //     //     speed: 1,
        //     //     refresh: 2,
        //     //     //infinite: true,
        //     //     nodeSpacing: function (node) {
        //     //         return 70;
        //     //     }, // extra spacing around nodes
        //     //
        //     //     ready: function () {
        //     //         //console.log('starting cola', Date.now());
        //     //     },
        //     //     stop: function () {
        //     //         //console.log('stop cola', Date.now());
        //     //     }
        //     // });
        //
        //
        // }
        //
        // currentLayout.run();


        // sg.layout({ name: 'cose',
        //     animate: true,
        //     fit: false,
        //     refresh: 1,
        //     //animationThreshold: 1,
        //     iterations: 5000,
        //     initialTemp: 100,
        //     //coolingfactor: 0.98,
        //     ready: function() {
        //         //console.log('starting cose', Date.now());
        //     },
        //     stop: function() {
        //         //console.log('stop cose', Date.now());
        //     }
        // });

    };
    setTimeout(layout, layoutUpdatePeriodMS);


    function d(x, key) {
        return x._private.data[key] || 0;
    }

    var sizeFunc = function(x) {
        var p1 = 1 + d(x, 'pri');// * d(x, 'belief');
        return parseInt(24 + 48 * (p1 * p1));
    };


    var priToOpacity = function(x) {
        var pri = d(x, 'pri');
        return (25 + parseInt(pri*75))/100.0;
    };
    sg.spacegraph.style().selector('node')
        .style('shape', 'hexagon')
        .style('width', sizeFunc)
        .style('height', sizeFunc)
        .style('background-color', function(x) {
            var belief = d(x, 'belief');
            var aBelief = Math.abs(belief);
            var pri = d(x, 'pri');
            var priColor = parseInt((0.5 + 0.5 * pri) * 128);
            var beliefColor = parseInt(( aBelief) * 255);
            var qua = d(x, 'qua');
            var quaColor = parseInt((0.5 + 0.5 * qua) * 128);
            if (belief >= 0.05) {
                return "rgb(" + priColor + "," + beliefColor + "," + quaColor + ")";
            } else if (belief <= -0.05) {
                return "rgb(" + beliefColor + "," + priColor + "," + quaColor + ")";
            } else {
                return "rgb(" + priColor + "," + priColor + "," + quaColor + ")";
            }

        })
        .style('background-opacity', priToOpacity);

    sg.spacegraph.style().selector('edge')
        .style('width', function(x) {
            return parseInt(2 + 5 * d(x, 'pri'));
        })
        .style('mid-target-arrow-shape', 'triangle')
        .style('opacity', priToOpacity)
        .style('curve-style', function(x) {
            var pri = d(x, 'pri');
            if (pri < 0.25) return 'haystack';
            return 'segments';
        })
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
        try {
            var x = JSON.parse(m.data);
            e.emit('message', [x]);
        } catch (c) {
            console.error(c, m.data);
        }

    };
    ws.onclose = function() {
        e.emit('disconnect', this);
    };

    e.socket = ws;
    e.send = function(x) {
        ws.send(x);
    };
    e.close = ws.close;

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

function TopTable(path) {
    var d = $('<div/>');
    var e = $('<div/>').attr('class', 'ConceptTable').css('overflow', 'scroll');

    function row(c) {
        var pri = c[1]/1000.0;
        var dur = c[2]/1000.0;
        var qua = c[3]/1000.0;

        return $(document.createElement('span'))
            .attr('class', 'ConceptRow')
            .attr('style',
                    'font-size:' +
                        parseInt(100.0 + 3 * 100.0*pri) +
                    '%; color: rgb(' +
                        parseInt(128 + 128 * pri) + ',' +
                        parseInt(64 + 192 * dur) + ',' +
                        parseInt(64 + 192 * qua) +
                    ')'
            )
            .text(c[0]);
    }

    d.append(e);

    var sv = SocketView(path,

        function(p) {
            return d;
        },

        function(msg) {
            var m = JSON.parse(msg.data);

            var rr = [];
            for (var k of m) {
                rr.push(row(k));
            }

            e.empty().append(rr);

        }
    );

    return sv;

}

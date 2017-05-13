"use strict";

function dock(socket, target) {


    const layout = new GoldenLayout({
        content: [
            {
                type: 'column',
                content: [
                    //{ componentName: 'graph', type: 'component', componentState: {} },
                    { componentName: 'terminal', type: 'component', componentState: {} },

                    {
                        type: 'column',
                        content: [
                            //{ componentName: 'terminal', type: 'component' },
                            //{ componentName: 'options', type: 'component' },
                            //{ componentName: 'edit', type: 'component' },
                            { componentName: 'input', type: 'component' }
                        ]
                    }
                ]
            }

        ]
    }, target);
    // {
    //     type: 'column',
    //     content: [{
    //         type: 'component',
    //         componentName: 'edit',
    //         componentState: {
    //
    //             //http://codemirror.net/doc/manual.html#usage
    //             lineNumbers: false,
    //             theme: 'night',
    //             mode: 'clojure',
    //             inputStyle: 'contenteditable'
    //
    //             //scrollbarStyle: null //disables scrollbars
    //         }
    //     }]
    // },
//                    {
//                        type: 'column',
//                        content: [{
//                            type: 'component',
//                            componentName: 'options',
//                            componentState: {}
//                        }]
//                    },
//                    {
//                        type: 'column',
//                        content: [{
//                            type: 'component',
//                            componentName: 'terminal',
//                            componentState: {}
//                        }, {
//                            type: 'component',
//                            componentName: 'input',
//                            componentState: {}
//                        }]
//                    }
    /*{
     type: 'component',
     componentName: 'terminal',
     componentState: { label: 'A' }
     }*/


    layout.on('stackCreated', function (stack) {

        /*
         * Accessing the DOM element that contains the popout, maximise and * close icon
         */
        stack.header.controlsContainer.append(MainMenuButton());
    });
    layout.registerComponent('terminal', function (tgt, state) {
        tgt.getElement().html(taskFeed(socket));
    });
    layout.registerComponent('input', function (tgt, state) {
        tgt.getElement().html(
            NALInputEditor(socket).attr('id', 'input')
        );
    });
    layout.registerComponent('edit', function (tgt, state) {
        tgt.getElement().html(
            Editor(state)
        );
    });
    layout.registerComponent('options', function (tgt, state) {
        var ee = Editor({
            lineNumbers: false,
            theme: 'night',
            mode: 'clojure',
            inputStyle: 'contenteditable'
        });
        tgt.getElement().html(ee);
        var e = ee.editor;

        socket.on('task', (t) => {
            // //HACK
            // if (t.punc === ';') {
            //     const pi = t.term.indexOf("(nar(?1),( &&+0 ,");
            //     if (pi !== -1) {
            //         var src = t.term;
            //         src = src.substring(pi + 17, src.length - 2);
            //         src = src.replace(/,nar/gi, ",\nnar");
            //         e.setValue(src);
            //     }
            // }
        });

        socket.send('nar(?1)');

    });


    function jsonedit() { //TODO
        layout.registerComponent('demo_jsonedit', function (tgt, state) {

            // Set default options
            //console.log(JSONEditor());
            //JSONEditor().defaults.options.theme = 'jqueryui';

            // Initialize the editor
            const dd = div('max');
            let editor = new JSONEditor(dd[0], {
                schema: {
                    type: "object",
                    properties: {
                        name: {"type": "string"}
                    }
                }
            });

            //            // Set the value
            //            editor.setValue({
            //                name: "John Smith"
            //            });
            //
            //            // Get the value
            //            var data = editor.getValue();
            //
            //            // Validate
            //            var errors = editor.validate();
            //            if(errors.length) {
            //                // Not valid
            //            }
            //
            //            // Listen for changes
            //            editor.on("change",  function() {
            //                // Do something...
            //            });

            tgt.getElement().html(dd);

        });
    }

    layout.registerComponent('graph', function (tgt) {

        tgt.getElement().html(graphConcepts(tgt));

    });

    layout.init();

    return layout;
}


function toptable() { //TODO
    layout.registerComponent('top', function (tgt, state) {
        tgt.getElement().html(TopTable(ioActive));
    });
}

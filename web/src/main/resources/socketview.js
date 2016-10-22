"use strict";

const defaultHostname = window.location.hostname || 'localhost';
const defaultWSPort = window.location.port || 8080;


/** creates a websocket connection to a path on the server that hosts the currently visible webpage */
const NARSocket = function(path) {
    var ws = new WebSocket('ws://' +
        defaultHostname + ':' +
        defaultWSPort + '/' +
        path);
    ws.binaryType = 'arraybuffer';
    return ws;
};



/** stores metawidget live data; TODO use WeakMap */
const mwdata = { };
function SocketView(path, pathToElement, onData) {

    mwdata[path] = ''; //initially empty

    const view = pathToElement(path);

    const ws = window.socket(path);

    ws.onopen = function () {
        //state.html("Connected");
    };
    ws.onmessage = onData;
    ws.onclose = function () {
        //state.html("Disconnected");
    };

    view.data('socket', ws);
    view.on("remove", function () {
        //disconnect the socket when element is removed
        $(this).data('socket').close();
    });


    return view;
}




function SocketMetaWidget(path) {

    const varPath = "mwdata['" + path + "']";
    const view = $('<x-metawidget readonly=true path="' + varPath + '"></x-metawidget>');

    return SocketView(path,

        function(path) {
            return view;
        },

        function(msg) {
            let v;
            try {
                v = JSON.parse(msg.data);
            } catch (e) {
                v = 'Error parsing: ' + e.data;
            }
            mwdata[path] = v;

            view.attr('path', "").attr('path', varPath);

            //target.html(view);
            //view.text(e.data);
        }
    );
}


function SocketSpaceGraph(path, idFunc, nodeFunc) {


    const view = $('<div/>');
    const sg = spacegraph(view, {
        //options
    });


    sg.onMsg = function(msg) {
        let v;

        if (msg.data) {
            if (typeof msg.data === "string") {
                try {
                    v = JSON.parse(msg.data);
                } catch (e) {
                    v = 'Error parsing: ' + e.data;
                }
            } else {
                v = msg;
            }
        }

        const nodesToRemove = view.nodesShown || new Set();

        const newNodeSet = new Set();

        const nodesToShow = [];
        const edgesToShow = [];

        //console.log(prev.size, 'previously');
        _.each(v, function(x) {

            const id = idFunc(x); //x[1];
            //if (!toRemove.delete(id)) {

            /** nodeFunc can return false to cause any previous node to be removed */
            if (nodeFunc(id, x, nodesToShow, edgesToShow)!==false) {
                nodesToRemove.delete(id);
            }
            newNodeSet.add(id);
            //}
        });




        const edgesToRemove = view.edgesShown || new Set();

        _.each(edgesToShow, function(e) { edgesToRemove.delete(e.id); });

        //console.log(prev.size, 'to delete');

        let changed = false;
        const shownEdgeSet = new Set();

        sg.batch(function() {


            //anything remaining in prev is inactive
            if (nodesToRemove.size > 0) {
                sg.removeNodes(Array.from(nodesToRemove));
                changed = true;
            }
            if (nodesToShow.length > 0) {
                _.each(nodesToShow, sg.addNode);

                changed = true;
            }
            //});
            //sg.batch(function() {

            if (edgesToRemove.length > 0) {
                _.each(edgesToRemove, sg.removeEdge);
                changed = true;
            }

            if (edgesToShow.length > 0) {
                _.each(edgesToShow, function (e) {
                    const target = e.target;

                    if (sg.get(target)) { //if target exists
                        sg.addEdge(e);
                        shownEdgeSet.add(e.id);
                    }
                });
                changed = true;
            }

            // if (changed) {
            //     layout(); //trigger layout
            // }



            //sg.elements().style();
        });



        view.nodesShown = newNodeSet;
        view.edgesShown = shownEdgeSet;
    };

    let sv;
    if (path) { //TODO different types of data interfaces/loaders
        sv = SocketView(path,

            function (path) {
                return view;
            },

            sg.onMsg
        );
    } else {
        sv = view;
    }

    sv.spacegraph = sg;

    sv.stop = function() {

        sg.destroy();

        if (sv.currentLayout) {
            sv.currentLayout.destroy();
            sv.currentLayout = null;
        }

    };

    return sv;
}


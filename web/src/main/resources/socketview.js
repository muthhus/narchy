"use strict";


function SocketView(path, pathToElement, onData) {


    mwdata[path] = ''; //initially empty

    var view = pathToElement(path);

    var ws = window.socket(path);

    ws.onopen = function() {
        //state.html("Connected");
    };
    ws.onmessage = onData;
    ws.onclose = function() {
        //state.html("Disconnected");
    };

    view.data('socket', ws);
    view.on("remove", function () {
        //disconnect the socket when element is removed
        $(this).data('socket').close();
    });

    return view;
}


/** stores metawidget live data; TODO use WeakMap */
var mwdata = { };

function SocketMetaWidget(path) {

    var varPath = "mwdata['" + path + "']";
    var view = $('<x-metawidget readonly=true path="' + varPath + '"></x-metawidget>');

    return SocketView(path,

        function(path) {
            return view;
        },

        function(msg) {
            var v;
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


    var view = $('<div/>');
    var sg = spacegraph(view, {
        //options
    });

    //var layoutUpdateMaxPeriodMS = 1000;

    var currentLayout = sg.makeLayout({
        /* https://github.com/cytoscape/cytoscape.js-spread */
        name: 'spread',
        minDist: 125,
        speed: 0.05,
        animate: false,
        randomize: false, // uses random initial node positions on true
        fit: false,
        maxFruchtermanReingoldIterations: 2, // Maximum number of initial force-directed iterations
        maxExpandIterations: 1, // Maximum number of expanding iterations

        ready: function () {
            //console.log('starting cola', Date.now());
        },
        stop: function () {
            //console.log('stop cola', Date.now());
        }
    });
    var layoutUpdatePeriodMS = 200;
    currentLayout.run();
    setInterval(function() {
        currentLayout.stop();
        currentLayout.run();
    }, layoutUpdatePeriodMS);

    var layout = function () {
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

    return SocketView(path,

        function(path) {
            return view;
        },

        function(msg) {
            var v;
            try {
                v = JSON.parse(msg.data);
            } catch (e) {
                v = 'Error parsing: ' + e.data;
            }

            var nodesToRemove = view.nodesShown || new Set() /* empty */;

            var newNodeSet = new Set();

            var newNodes = [];
            var newEdges = [];

            //console.log(prev.size, 'previously');
            _.each(v.seq, function(x) {
                x = x.seq;
                if (!x) return;
                var id = idFunc(x); //x[1];
                //if (!toRemove.delete(id)) {

                /** nodeFunc can return false to cause any previous node to be removed */
                if (nodeFunc(id, x, newNodes, newEdges)!==false) {
                    nodesToRemove.delete(id);
                }
                newNodeSet.add(id);
                //}
            });




            var edgesToRemove = view.edgesShown || new Set() /* empty */;

            _.each(newEdges, function(e) { edgesToRemove.delete(e.id); });

            //console.log(prev.size, 'to delete');

            var changed = false;

            sg.batch(function() {
                //anything remaining in prev is inactive
                if (nodesToRemove.size > 0) {
                    sg.removeNodes(Array.from(nodesToRemove));
                    changed = true;
                }
                if (newNodes.length > 0) {
                    //sg.addNodes(newNodes);
                    _.each(newNodes, function (n) {
                        sg.addNode(n);
                    });
                    changed = true;
                }
            });

            var newEdgeSet = new Set();

            sg.batch(function() {

                if (edgesToRemove.length > 0) {
                    _.each(edgesToRemove, function (e) {
                        sg.removeEdge(e);
                    });
                    changed = true;
                }

                if (newEdges.length > 0) {
                    _.each(newEdges, function (e) {
                        var tt = e.target;
                        if (sg.get(tt)) {
                            sg.addEdge(e);
                            newEdgeSet.add(e.id)
                        }
                    });
                    changed = true;
                }

                if (changed) {
                    layout(); //trigger layout
                }

            });



            view.nodesShown = newNodeSet;
            view.edgesShown = newEdgeSet;
        }
    );
}

function SocketNARGraph(path) {
    return SocketSpaceGraph(path, function(x) { return x[1]; },
        function(id, x, newNodes, newEdges) {
            var pri = x[2];
            var qua = x[4];
            var baseSize = 32, extraSize = 132;
            newNodes.push({
                 id: id,
                 label: id,
                 style: {
                     width: baseSize + extraSize * pri,
                     height: baseSize + extraSize * pri,
                     'background-color': //'HSL(' + parseInt( (0.1 * qua + 0.4) * 100) + '%, 60%, 60%)',
                            "rgb(" + ((0.5 + 0.5 * qua) * 255) + ", 128, 128)",
                     'background-opacity': 0.25 + pri * 0.75
                 }
             });

            var termlinks = x[5];
            _.each(termlinks.seq, function(e) {
                if (!(e = e.seq))
                    return;

                var target = e[1];
                var tlpri = e[2];

                newEdges.push({
                    id: 'tl' + '_' + id + '_' + target, source: id, target: target,
                    style: {
                        'line-color': 'orange',
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

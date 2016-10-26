"use strict";

function MainMenuButton() {
    return $('<div>[@]</div>').addClass('MainMenuButton').click(() => setTimeout(Menu, 0));
}

function Menu() {
    const menulayer = div('MenuLayer').attr('oncontextmenu', "return false;");
    const graphCanvas = div('MenuGraph max').appendTo(menulayer);

    let c;

    function close() {
        menulayer.fadeOut(250, () => {
            setTimeout(() => {
                if (c)
                    c.destroy();
                menulayer.remove();
            }, 0);
        });
    }


    const menu = {
        nodes: [],
        edges: [],

        add: function (item, parent, options) {
            options = options || {};
            options.id = item;

            //disable label if widget is provided
            options.label = options.widget ? "" : item;

            this.nodes.push({data: options});
            if (parent)
                this.edges.push({data: {source: parent, target: item}});
        }
    };

    const body = $('body');

    const _return = 'return';

    menu.add(_return, null, {
        widget: $('<button>[x]</button>').css({opacity: 0.75, border: 0}).click(() => close())
    });
    menu.add('System', null, {});
    menu.add('Network', 'System', {});
    menu.add('Memory', 'System', {});
    {
        menu.add('Concepts', 'Memory', {});
        menu.add('Goals', 'Memory', {});
    }
    menu.add('CPU', 'System', {});
    {
        menu.add('Restart', 'System', {});
        menu.add('Shutdown', 'System', {});
    }

    menu.add('Status', null, {});
    {

        menu.add('Weather', 'Status', {
            //widget: $('<iframe width="400" height="400" src="http://wunderground.org"></iframe>')
            widget: $('<div><h3>Weather</h3></div>').append(
                $('<button>Forecast</button>').click(()=>{
                    var q = 'https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22FUKUSHIMA%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys';
                    $.getJSON(q, (d)=>{
                        d = d.query.results.channel.item.forecast; //WTF

                        for (var i = 0; i < d.length; i++) {
                            //console.log(d);
                            //console.log(JSON.stringify(d));
                            //TODO convert to: NARSocket.send('json/in', JSON.stringify(d))
                            const msg = JSON.stringify(['Fukushima', d[i]]);
                            NARSocket('json/in', null, {
                                onopen: function (socket) {
                                    socket.send(msg);
                                    socket.close();
                                }
                            });
                        }
                    });
                })
            )
        });

        {
            menu.add('Temperature', 'Weather', {
                widget: $('<div>Temperature<br/><input type="range" min="0" max="15"/></div>')
            });
            menu.add('Precipitation', 'Weather', {});
            menu.add('Humidity', 'Weather', {});
            menu.add('Air Quality', 'Weather', {});
        }

        menu.add('Bio', 'Status', {});
        {
            menu.add('Food', 'Bio', {});
            menu.add('Medicine', 'Bio', {});
            menu.add('Shelter', 'Bio', {});
            menu.add('Defense', 'Bio', {});
        }

        menu.add('News', 'Status', {});
    }

    menu.add('Find', null, {
        //widget: $('<div><input type="text" placeholder="?"></input></div>')
        widget: $('<textarea rows="12" cols="50">ABC 123 XYZ</textarea>')
    });


    c = cytoscape({
        container: graphCanvas,

        layout: {
            name: 'concentric',
            // concentric: function (node) { // returns numeric value for each node, placing higher nodes in levels towards the centre
            //
            //     const aStar = node.cy().elements().aStar({root: "#" + _return, goal: node});
            //
            //     return 100 - (aStar.distance);
            // },

            levelWidth: function(n) { return 1; }, // the variation of concentric values in each level

            //equidistant: true,
            //minNodeSpacing: 30,

            //sweep: Math.PI*2.0
        },

        // layout: {
        //     name: 'breadthfirst',
        //     //name: 'cose',
        //     //randomize: true,
        //     fit: true
        // },

        ready: (a) => {


            const cc = a.cy;

            const pr = cc.elements()
                .closenessCentralityNormalized();
            //.degreeCentralityNormalized();
            //.pageRank();

            cc.nodes().each((i, n) => {

                var v = //1 / Math.pow(pr.rank(n), 2);
                    pr.closeness(n);

                if (v === 0)
                    v = 1; //root

                n.style({
                    width: v * 32,
                    height: v * 24
                });

            });

        },

        style: [
            {
                selector: 'node',
                style: {
                    'background-color': '#888',
                    'label': 'data(label)',
                    'text-color': 'white',
                    'shape': 'hexagon',
                    //'width':
                    //  node => 48 * (1+Math.sqrt(node.outdegree())),
                    //'height': node => 30 * (1+Math.sqrt(node.outdegree())),
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'color': '#fff',
                    'font-family': 'Monospace',
                    'font-size': 4

                }
            }
        ],

        elements: menu,
    });

    const activeWidgets = new Map();

    function onAdd(node) {

        //console.log(node);

        const data = node._private.data;
        if (data.widget) {

            const widget = (data.widget.jquery) ? data.widget[0] : data.widget; //HACK un-querify

            const nid = node.id();
            widget.setAttribute('id', 'node_' + nid);

            const style = widget.style;
            style.position = 'fixed';
            style.transformOrigin = '0 0';

            menulayer.append(widget);
            activeWidgets.set(nid, node);

            setTimeout(() => updateWidget(node), 0)


        }
    }

    c.nodes().each((i, v) => onAdd(v));
    c.on('add', /* select unselect  */ function (e) {
        onAdd(e.cyTarget);
    });

    c.on('remove', /* select unselect  */ function (e) {

        const node = e.cyTarget;
        const data = node._private.data;
        if (data.widget) {
            const widget = (data.widget[0]) ? data.widget[0] : data.widget; //HACK un-querify
            activeWidgets.remove(node.id());
            widget.detach();
        }

    });

    function updateAll() {

        fastdom.mutate(() => {
            activeWidgets.forEach((node, nid) => {
                updateWidget(node);
            });
        });
    }

    c.on('pan zoom ready', /* select unselect  */ function (e) {
        updateAll();
    });

    c.on('position style data', /* select unselect  */ function (e) {

        const node = e.cyTarget;
        const data = node._private.data;
        if (data && data.widget) {
            updateWidget(node);
            //console.log(this, that, target);
            //that.commit();
        }
    });


    function zoomTo(ele, zoomDuration) {
        // var pos;
        // if (!ele || !ele.position)
        //     pos = { x: 0, y: 0 };
        // else
        //     pos = ele.position();

        c.animate({
            fit: {
                eles: ele,
                padding: 40
            }
        }, {
            duration: zoomDuration
            /*step: function() {
             }*/
        });
    }

    //--------------
    //right-click autozoom:
    c.on('cxttapstart', function (e) {
        let target = e.cyTarget;
        zoomTo(!target ? undefined : target, 128 /* ms */)
    });

//            const layout = c.makeLayout({
//                /* https://github.com/cytoscape/cytoscape.js-spread */
//                name: 'spread',
//                    minDist: 250,
//                    //padding: 100,
//
//                    speed: 0.06,
//                    animate: false,
//                    randomize: true, // uses random initial node positions on true
//                    fit: false,
//                    maxFruchtermanReingoldIterations: 1, // Maximum number of initial force-directed iterations
//                    maxExpandIterations: 2, // Maximum number of expanding iterations
//
//                    ready: function () {
//                    //console.log('starting spread', Date.now());
//                },
//                stop: function () {
//                    //console.log('stop spread', Date.now());
//                }
//            });
//            c.onRender(()=>{
//               layout.run();
//            });


    menulayer.hide();

    fastdom.mutate(() => {
        menulayer.appendTo(body);
        menulayer.fadeIn();
    });


}

$(document).ready(() => {

    const io = NARSocket('terminal', decodeTasks);
    window.io = io;

    const layout = new GoldenLayout({
        content: [{
            type: 'row',
            content: [
                {
                    type: 'column',
                    content: [{
                        type: 'component',
                        componentName: 'graph',
                        componentState: {}
                    }, {
                        type: 'component',
                        componentName: 'top',
                        componentState: {}
                    }]
                },
                {
                    type: 'column',
                    content: [{
                        type: 'component',
                        componentName: 'terminal',
                        componentState: {}
                    }, {
                        type: 'component',
                        componentName: 'input',
                        componentState: {}
                    }]
                }
                /*{
                 type: 'component',
                 componentName: 'terminal',
                 componentState: { label: 'A' }
                 }*/
            ]
        }]
    }, $('body'));

    layout.on('stackCreated', function (stack) {

        /*
         * Accessing the DOM element that contains the popout, maximise and * close icon
         */
        stack.header.controlsContainer.append(MainMenuButton());
    });
    layout.registerComponent('terminal', function (tgt, state) {
        tgt.getElement().html(IO(io));
    });
    layout.registerComponent('input', function (tgt, state) {
        tgt.getElement().html(
            NALEditor(io).attr('id', 'input')
        );
    });
    layout.registerComponent('options', function (tgt, state) {

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
    layout.registerComponent('graph', function (tgt, state) {

        const c = spacegraph({});

        io.on('task', function (x) {

            const id = x.term + x.punc + x.freq + ';' + x.conf; //HACK for Task's
            x.label = x.term; //HACK


            let existing = c.graph.get(id);

            if (!existing) {

                //add
                c.graph.add({group: "nodes", data: x});

            } else {
                //replace / merge
                existing.data = x;
            }

            c.changed = true;
        });

        tgt.getElement().html(c);
        tgt.on('resize', () => {
            setTimeout(() => c.graph.resize(), 0);
        });

    });
    layout.registerComponent('top', function (tgt, state) {
        tgt.getElement().html(TopTable());
    });

    layout.init();

});



//    function gridCell(contents) {
//        return div('grid-stack-item').append(div('grid-stack-item-content').append(contents));
//    }

function IO(term) {

    return new NARConsole(term, (x) => {


        const label = x.term + x.punc + truthString(x.freq, x.conf);

        //const fontSize = 2 * (1 + parseInt(x.pri * 99.0)) + '%';
        const fontSize = (75 + 8 * Math.sqrt(1 + 10 * parseInt(x.pri * 9.0))) + '%';


        const d = document.createElement('div');
        switch (x.punc) {
            case '.':
                d.className = 'belief';
                break;
            case '?':
                d.className = 'question';
                break;
            case '!':
                d.className = 'goal';
                break;
            case ';':
                d.className = 'command';
                break;
        }
        d.style.opacity = 0.5 + 0.5 * x.dur;
        d.style.fontSize = fontSize;
        d.innerText = label;
        return d;


    }).addClass('terminal');

//
//        ).attr('id', 'console');

    //return c;
}


function truthComponentStr(x) {

    const i = parseInt(Math.round(100 * x));
    if (i == 100)
        return '1.0';
    else if (i == 0)
        return '0.0';
    else
        return i / 100.0;
}

function truthString(f, c) {
    return c ?
        ("%" + truthComponentStr(f) +
        ";" + truthComponentStr(c) + "%") :
        "";
}

function div(cssklass) {
    const d = document.createElement('div');
    if (cssklass) {
        d.className = cssklass;
    }
    return $(d);
}


//            div('max grid-stack').append(
//
//                gridCell([


//                ]),
//
//                //gridCell(div().html('<h1>ABC</h1>')),
//                //gridCell( NALTimeline(term).attr('id', 'graph') ),
//
//                gridCell( TopTable("active") )

//            ).gridstack({
//                cellHeight: 160,
//                cellWidth: 160,
//                verticalMargin: 20,
//                horizontalMargin: 20
//            })


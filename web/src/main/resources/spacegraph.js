"use strict";

const BUDGET_EPSILON = 0.001;

function uuid() {
    //Mongo _id = 12 bytes (BSON) = Math.pow(2, 12*8) = 7.922816251426434e+28 permutations
    //UUID = 128 bit = Math.pow(2, 128) = 3.402823669209385e+38 permutations
    //RFC 2396 - Allowed characters in a URI - http://www.ietf.org/rfc/rfc2396.txt
    //		removing all that would confuse jquery
    //var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz-_.!~*\'()";
    //var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz-_";
    //TODO recalculate this
    //70 possible chars
    //	21 chars = 5.58545864083284e+38 ( > UUID) permutations
    //		if we allow author+objectID >= 21 then we can guarantee approximate sparseness as UUID spec
    //			so we should choose 11 character Nobject UUID length
    //TODO recalculate, removed the '-' which affects some query selectors if - is first
    var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz_";
    var string_length = 11;
    var randomstring = '';
    for (var i = 0; i < string_length; i++) {
        var rnum = Math.floor(Math.random() * chars.length);
        randomstring += chars[rnum];
    }
    return randomstring;
}

function spacegraph(opt) {

    opt = opt || {};

    const d = _.extend(div('graph max'), opt || {});


    var oo  = cytoscapeOptions(opt, function() {}, d);

    const c = cytoscape(oo);
    const cypp = c._private.elements._private.ids;
    c.get = function(id) {
        return cypp[id];
    };


    const colorFunc = function (r, g, b) {

        const colorDivisor = 8; //integer, to minimize total unique colors by rounding to nearest N color values

        const scale = 256.0 / colorDivisor;

        const R = parseInt(r * scale) * colorDivisor;
        const G = parseInt(g * scale) * colorDivisor;
        const B = parseInt(b * scale) * colorDivisor;

        return "rgb(" + R + "," + G + "," + B + ")";

    };



//        c.onRender(()=>{
//           console.log('render');
//           changed = false;
//        });

    const maxNodes = 96;
    const updatePeriodMS = 250;


    const localPriDecay = 0.999;

    const minRad = 16.0;
    const maxRad = 256.0;


    const minThick = 1;
    const maxThick = 24;

    const seedRadius = 512.0;

    const speed = 0.85;
    const friction = 0.05;

    const perturbThresh = 2;


    const attract = 2;
    const repel = 15;
    const maxRepelDistance = 1500;

    const minAttractDistance = 50; //padding

    const noiseLevel = 0;

    var busy = false;
    setInterval(() => {

        if (busy)
            return;

        busy = true;


        const nodes = c.nodes();
        const toRemove = nodes.cap() - maxNodes;
        var sorted;
        if (toRemove > 0) {
            sorted = nodes.sort((a, b) => {
                if (a == b)
                    return 0;

                //increasing priority
                let ad = a._private.data;
                let bd = b._private.data;

                const x = ad.pri - bd.pri;
                if (x <= BUDGET_EPSILON)
                    return ad.term.localeCompare( bd.term );
                else
                    return x;
            });

        } else {
            sorted = null;
        }

        c.batch(() => {

            busy = false;

            //remove weakest items from the graph
            for (let i = 0; i < toRemove; i++) {
                sorted[i].remove();
            }

            const nn = new Array();

            c.nodes().forEach((n) => {
                const x = n._private.data; //HACK
                if (x) {
                    nn.push(x);

                    const p = x.pri;
                    x.pri *= localPriDecay;

                    const r =
                            minRad + (maxRad-minRad) * Math.sqrt( p )  //for pi^2 area
                        ;

                    const ri = 1 + parseInt(r);

                    let pos = x.pos;
                    if (!pos) {
                        //initialization
                        x.pos = pos = [ (Math.random()-0.5)*seedRadius, (Math.random()-0.5)*seedRadius ];
                        x.vel = [0.0,0.0];
                    } else {
                        const vispos = n.position();

                        const px = pos[0];
                        const py = pos[1];

                        var nx, ny;
                        if ((Math.abs(vispos.x - px) >= perturbThresh) || (Math.abs(vispos.y - py) >= perturbThresh)) {
                            //interaction overrides:
                            nx = vispos.x;
                            ny = vispos.y;
                        } else {

                            const mass = 1 + p;

                            //then this isnt velocity exactly, more like force or impulse
                            nx = (px * (1.0 - speed) + (px + x.vel[0]/mass) * speed);
                            ny = (py * (1.0 - speed) + (py + x.vel[1]/mass) * speed);

                        }

                        pos[0] = nx;
                        pos[1] = ny;

                    }

                    const pp = 0.25 + 0.75 * p;
                    n.style({
                        //                       sg.spacegraph.style().selector('node')
                        //                       .style('background-color', function(x) {
                        //                           const belief = 0.25 + 0.75 * d(x, 'belief');
                        //                           const aBelief = 0.25 + 0.75 * Math.abs(belief);
                        //                           const pri = 0.25 + 0.75 * d(x, 'pri');
                        width: ri,
                        height: ri,
                        fontSize: 1 + parseInt(r/10.0),
                        shape: 'hexagon',
                        backgroundColor: colorFunc(pp, pp, pp),
                        //position: n.position() || [ Math.random() * 10, Math.random() * 10]

                    });
                    n.position({ x: parseInt(pos[0]), y: parseInt(pos[1]) }); //pos);


                }
            });


            c.edges().each((e, i) => {
                const x = e._private.data; //HACK
                const p = x.pri;


                const ooo = 0.25 + 0.75 * p;
                e.style({
                    'lineColor': colorFunc(ooo, ooo, ooo),
                    'width': parseInt(minThick + (maxThick-minThick) * Math.sqrt(p))
                });

            });


            //force-directed layout update
            later( ()=> {

                //cache:
                const antifriction = (1.0 - friction);
                const maxRepelDistanceSq = maxRepelDistance*maxRepelDistance;
                const minAttractDistanceSq = minAttractDistance*minAttractDistance;

                for (var i = 0; i < nn.length; i++) {

                    const nni = nn[i];
                    const iv = nni.vel;


                    //FRICTION
                    var vx = iv[0] * antifriction;
                    var vy = iv[1] * antifriction;

                    //JITTER NOISE
                    if (noiseLevel > 0) {
                        vx += (Math.random() - 0.5) * noiseLevel;
                        vy += (Math.random() - 0.5) * noiseLevel;
                    }

                    //REPEL NEIGHBORS
                    const a = nni.pos;
                    for (var j = i + 1; j < nn.length; j++) {
                    //for (var j = 0; j < nn.length; j++) {
                        //if (i == j) continue;

                        const b = nn[j].pos;

                        const dx = (a[0] - b[0]);
                        const dy = (a[1] - b[1]);
                        var distsq = dx * dx + dy * dy;
                        if (distsq <= maxRepelDistanceSq) {
                            const rr = repel / distsq;
                            vx += rr * dx;
                            vy += rr * dy;
                        }
                    }

                    //ATTRACT LINKS
                    for (const tl of nni.termlinks) {
                        const target = c.get(tl.target);
                        if (target) {
                            const y = target._private.data;
                            const b = y.pos;
                            if (b) {
                                const dx = (a[0] - b[0]);
                                const dy = (a[1] - b[1]);

                                const distSq = dx * dx + dy * dy;
                                if (distSq >= minAttractDistanceSq) {
                                    const dist = Math.sqrt(distSq);

                                    const rr = (tl.pri) * attract / dist;
                                    // / dist; //hooks law, spring ? TODO check
                                    vx -= rr * dx;
                                    vy -= rr * dy;
                                }
                            }
                        }
                    }


                    iv[0] = vx;
                    iv[1] = vy;
                }



            });

        });


    }, updatePeriodMS);


    d.graph = c; //DEPRECATED, use proper extension

    return d;
}



function cytoscapeOptions(opt, ready, target) {

    return _.defaults(opt, {
        ready: ready,
        // initial viewport state:
        zoom: 1,
        pan: {x: 0, y: 0},
        // interaction options:
        minZoom: 1e-15,
        maxZoom: 1e15,
        zoomingEnabled: true,
        userZoomingEnabled: true,
        panningEnabled: true,
        userPanningEnabled: true,
        selectionType: 'single',
        boxSelectionEnabled: false,
        autolock: false,
        autoungrabify: false,
        autounselectify: false,
        //fps: 25, //target max fps (frames per second)
        // rendering options:
        headless: false,
        styleEnabled: true,
        hideEdgesOnViewport: false,
        hideLabelsOnViewport: false,
        textureOnViewport: true, //true = higher performance, lower quality
        motionBlur: false,
        wheelSensitivity: 0.5,
        //pixelRatio: 0.25, //downsample pixels
        //pixelRatio: 0.5,
        //pixelRatio: 1,

        initrender: function (evt) { /* ... */
        },


        renderer: {
            /* ... */
            name: 'canvas',
            showFps: false
        },

        container: target[0],

        style: [
            {
                selector: 'node',
                style: {
                    'color': '#fff',
                    'background-color': '#888',
                    'label': 'data(label)',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'font-family':  //keep short because this gets repeated as part of strings in the style system badly
                        //'Arial',
                        'Monospace',


                    'text-outline-width': 0,

                    //'outside-texture-bg-opacity': 1,
                    //'shadow-blur': 0,
                    //'text-shadow-blur': 0,
                    //'shadow-opacity': 1,
                    'min-zoomed-font-size': 6,
                    'text-events': false,
                    'border-width': 0
                    /*border-width : The size of the node’s border.
                                         border-style : The style of the node’s border; may be solid, dotted, dashed, or double.
                                         border-color : The colour of the node’s border.
                                         border-opacity : The opacity of the node’s border.*/
                    /*'text-background-opacity': 1,
                                         'text-background-color': '#ccc',
                                         'text-background-shape': 'roundrectangle',
                                         text-border-opacity : The width of the border around the label; the border is disabled for 0 (default value).
                                         text-border-width : The width of the border around the label.
                                         text-border-style : The style of the border around the label; may be solid, dotted, dashed, or double.
                                         text-border-color : The colour of the border around the label.
                                         text-shadow-blur : The shadow blur distance.
                                         text-shadow-color : The colour of the shadow.
                                         text-shadow-offset-x : The x offset relative to the text where the shadow will be displayed, can be negative. If you set blur to 0, add an offset to view your shadow.
                                         text-shadow-offset-y : The y offset relative to the text where the shadow will be displayed, can be negative. If you set blur to 0, add an offset to view your shadow.
                                         text-shadow-opacity : The opacity of the shadow on the text; the shadow is disabled for 0 (default value).
                                         */
                }
            }
            /*,
                         {
                         selector: '.background',
                         style: {
                         'text-background-opacity': 1,
                         'text-background-color': '#ccc',
                         'text-background-shape': 'roundrectangle',
                         'text-border-color': '#000',
                         'text-border-width': 1,
                         'text-border-opacity': 1
                         }
                         }*/
        ]
    });
}



function updateWidget(node) {
    const data = node._private.data;
    const widget = (data.widget[0]) ? data.widget[0] : data.widget; //HACK un-querify

     //unjquery-ify
    var minPixels= widget.minPixels;

    const pixelScale = parseFloat(widget.pixelScale || 128.0); //# pixels wide

    var pos = node.renderedPosition();

    const bb = node.renderedBoundingBox({
        includeNodes: true,
        useCache: true,
        includeOverlays: false,
        includeShadows: false,
        includeEdges: false,
        includeLabels: false
    });
    const pw = parseFloat(bb.w); //parseFloat(node.renderedWidth());
    const ph = parseFloat(bb.h); //parseFloat(node.renderedHeight());


    var scale = parseFloat(widget.scale || 0.75);

    var cw, ch;
    var narrower = parseInt(pixelScale);
    if (pw < ph) {
        cw = narrower;
        ch = parseInt(pixelScale*(ph/pw));
    }
    else {
        ch = narrower;
        cw = parseInt(pixelScale*(pw/ph));
    }




    //get the effective clientwidth/height if it has been resized
    var style = widget.style;
    if ((( widget.specWidth !== style.width ) || (widget.specHeight !== style.height))) {
        var hcw = widget.clientWidth;
        var hch = widget.clientHeight;

        widget.specWidth = style.width = cw;
        widget.specHeight = style.height = ch;


        // cw = hcw;
        // ch = hch;

    }
    if (minPixels) {
        var hidden = ('none' === style.display);

        if (Math.min(wy, wx) < minPixels / pixelScale) {
            if (!hidden) {
                style.display = 'none';
                return;
            }
        }
        else {
            if (hidden) {
                style.display = 'block';
            }
        }
    }

    //console.log(html[0].clientWidth, cw, html[0].clientHeight, ch);


    var globalToLocalW = pw / cw;
    var globalToLocalH = ph / ch;

    var transformPrecision = 3;

    var wx = (scale * globalToLocalW).toPrecision(transformPrecision);
    var wy = (scale * globalToLocalH).toPrecision(transformPrecision);

    //TODO check extents to determine node visibility for hiding off-screen HTML
    //for possible improved performance

    //parseInt here to reduce precision of numbers for constructing the matrix string
    //TODO replace this with direct matrix object construction involving no string ops

    var halfScale = scale/2.0;
    var px = (pos.x - (halfScale*pw)).toPrecision(transformPrecision);
    var py = (pos.y - (halfScale*ph)).toPrecision(transformPrecision);

    //px = parseInt(pos.x - pw / 2.0 + pw * paddingScale / 2.0); //'e' matrix element
    //py = parseInt(pos.y - ph / 2.0 + ph * paddingScale / 2.0); //'f' matrix element
    //px = pos.x;
    //py = pos.y;

    //nextCSS['transform'] = tt;
    //html.css(nextCSS);

    //TODO non-String way to do this
    //var matb = 0, matc = 0;
    //style.transform = 'matrix(' + wx+ ',' + 0/*matb*/ + ',' + 0/*matc*/ + ',' + wy + ',' + px + ',' + py + ')';;
    style.transform = 'matrix(' + wx+ ',0,0,' + wy + ',' + px + ',' + py + ')';
}


// class Channel extends EventEmitter {
//
//     //EVENTS
//     //.on("graphChange", function(graph, nodesAdded, edgesAdded, nodesRemoved, edgesRemoved) {
//
//     constructor(initialData) {
//         super();
//
//         this.ui = null;
//
//         this.prev = { };
//         this.commit = function() { }; //empty
//
//         //set channel name
//         if (typeof(initialData)==="string")
//             initialData = { id: initialData };
//
//         this.data = initialData || { };
//         if (!this.data.id) {
//             //assign random uuid
//             this.data.id = uuid();
//         }
//
//         if (!this.data.nodes) this.data.nodes =[];
//         if (!this.data.edges) this.data.edges =[];
//
//         var u = uuid();
//         var uc = 0;
//
//         var ensureID = function(x) {
//             if (!x.id) x.id = u + (uc++);
//         };
//
//         //assign unique uuid to any nodes missing an id
//         _.each(this.data.nodes, ensureID);
//         _.each(this.data.edges, ensureID);
//
//     }
//
//
//     init(ui) {
//         this.ui = ui;
//     }
//
//     id() {
//         return this.data.id;
//     }
//
//     clear() {
//         //TODO
//     }
//
//
//     removeNode(n) {
//         n.data().removed = true;
//
//         var removedAny = false;
//         var id = n.data().id;
//         this.data.nodes = _.filter(this.data.nodes, function(e) {
//             if (e.id === id) {
//                 removedAny = true;
//                 return false;
//             }
//         });
//
//         if (removedAny)
//             this.emit('graphChange', [this, null, null, n, null]);
//
//         return removedAny;
//     }
//
//     //TODO: removeEdge
//
//     //TODO batch version of addNode([n])
//     addNode(n) {
//         this.data.nodes.push(n);
//         this.emit('graphChange', [this, [n], null, null, null]);
//     }
//
//     addEdge(e) {
//         this.data.edges.push(e);
//         this.emit('graphChange', [this, null, [e], null, null]);
//     }
//
//
//     //nodes and edges are arrays
//     add(nodes, edges) {
//         var that = this;
//         _.each(nodes, function(n) { that.data.nodes.push(n); });
//         _.each(edges, function(e) { that.data.edges.push(e); });
//         //nodes.forEach(this.data().nodes.push); //??
//         //edges.forEach(this.data().edges.push);
//         this.emit('graphChange', [this, nodes, edges, null, null]);
//     }
// }
// function _spacegraph(targetWrapper, opt) {
//
//     targetWrapper.addClass("spacegraph");
//
//
//     //var overlaylayer = $('<div class="overlay"></div>').prependTo(targetWrapper);
//
//     //where cytoscape renders to:
//     var target = $('<div class="graph"/>')
//         .attr('oncontextmenu', "return false;")
//         .appendTo(targetWrapper);
//
//     var suppressCommit = false;
//     var zoomDuration = 128; //ms
//
//     var frame =
//         //NodeFrame(this);
//         null;
//
//
//     var ready = function() {
//
//
//         //opt.start.apply(this);
//
//         //http://js.cytoscape.org/#events/collection-events
//
//         //overlay framenode --------------
//
//         var that = this;
//
//
//         if (frame) {
//
//
//             const updateWidget = function(target, target2) {
//                 if (target2)
//                     target = target2; //extra parameter to match the callee's list
//
//                 //if (widget(target)) {
//                 //var data = target.data();
//                 s.updateNodeWidget(target); //that.updateNodeWidget(target);
//                 //if (!data.updating) {
//                 //data.updating = setTimeout(s.updateNodeWidget, widgetUpdatePeriodMS, target); //that.updateNodeWidget(target);
//                 //}
//                 //}
//             }
//
//             this.updateAllWidgets = (/*_.throttle(*/function () {
//
//                 if (suppressCommit)
//                     return;
//
//                 that.nodes().filterFn(function (ele) {
//                     if (ele.data('widget') !== undefined)
//                         updateWidget(ele);
//                 });
//                 //that.each(refresh);
//
//             }); //, widgetUpdatePeriodMS);
//
//             this.on('data position select unselect add remove grab drag style', function (e) {
//
//                 if (suppressCommit)
//                     return;
//
//                 /*console.log( evt.data.foo ); // 'bar'
//
//                  var node = evt.cyTarget;
//                  console.log( 'tapped ' + node.id() );*/
//
//                 var target = e.cyTarget;
//                 var widget = target.data('widget');
//                 if (widget) {
//                     setTimeout(updateWidget, 0, target);
//                     //console.log(this, that, target);
//                     //that.commit();
//                 }
//
//             });
//
//             if (this.updateAllWidgets)
//                 this.on('layoutstop pan zoom', this.updateAllWidgets);
//
//         }
//
//
//
//
//
//
//
//         /*
//          var baseRedraw = this._private.renderer.redraw;
//
//          this._private.renderer.redraw = function(options) {
//          baseRedraw.apply(this, arguments);
//
//          //frame.hoverUpdate();
//
//          };
//          */
//
//
//         /*var baseDrawNode = this._private.renderer.drawNode;
//          this._private.renderer.drawNode = function (context, node, drawOverlayInstead) {
//          baseDrawNode.apply(this, arguments);
//          };*/
//
//
//
//
// //            function scaleAndTranslate( _element , _x , _y, wx, wy )  {
// //
// //                var mat = _element.style.transform.baseVal.getItem(0).matrix;
// //                // [1 0 0 1 tx ty],
// //                mat.a = wx; mat.b = 0; mat.c = 0; mat.d = wy; mat.e = _x; mat.f = _y;
// //
// //            }
//     };
//
//
//     opt = cytoscapeOptions(opt, ready, target);
//
//
//     var s = cytoscape(opt);
//
//     if (opt.edgehandles) {
//         // EdgeHandler: the default values of each option are outlined below:
//         s.edgehandles(opt.edgehandles);
//     }
//
//
// //    s.noderesize({
// //      handleColor: '#000000', // the colour of the handle and the line drawn from it
// //      hoverDelay: 150, // time spend over a target node before it is considered a target selection
// //      enabled: false, // whether to start the plugin in the enabled state
// //      minNodeWidth: 30,
// //      minNodeHeight: 30,
// //      triangleSize: 10,
// //      lines: 3,
// //      padding: 5,
// //
// //      start: function(sourceNode) {
// //        console.log('resize', sourceNode);
// //        // fired when noderesize interaction starts (drag on handle)
// //      },
// //      complete: function(sourceNode, targetNodes, addedEntities) {
// //        // fired when noderesize is done and entities are added
// //      },
// //      stop: function(sourceNode) {
// //        // fired when noderesize interaction is stopped (either complete with added edges or incomplete)
// //      }
// //    });
//
//     //s.channels = { };
//     //s.overlay = overlaylayer;
//
//
//     //var ren = s.renderer();
//
//     /*var time;
//      function draw() {
//      requestAnimationFrame(draw);
//      var now = new Date().getTime(),
//      dt = now - (time || now);
//
//      time = now;
//
//      // Drawing code goes here... for example updating an 'x' position:
//      this.x += 10 * dt; // Increase 'x' by 10 units per millisecond
//      }*/
//
//     // //IMPROVED CANVAS RENDERER FUNCTION THAT CAN THROTTLE FPS
//     // s.renderer().redraw = function(roptions) {
//     //
//     //
//     //         var minRedrawLimit = 1000.0/opt.fps; // people can't see much better than 60fps
//     //         //var maxRedrawLimit = 1000.0;  // don't cap max b/c it's more important to be responsive than smooth
//     //
//     //         roptions = roptions || {}; //util.staticEmptyObject();
//     //
//     //         var r = this;
//     //
//     //         if( r.averageRedrawTime === undefined ){ r.averageRedrawTime = 0; }
//     //         if( r.lastRedrawTime === undefined ){ r.lastRedrawTime = 0; }
//     //
//     //         //var redrawLimit = r.lastRedrawTime; // estimate the ideal redraw limit based on how fast we can draw
//     //         //redrawLimit = minRedrawLimit > redrawLimit ? minRedrawLimit : redrawLimit;
//     //         //redrawLimit = redrawLimit < maxRedrawLimit ? redrawLimit : maxRedrawLimit;
//     //
//     //
//     //         var nowTime = Date.now();
//     //         if (r.lastDrawAt === undefined) r.lastDrawAt = 0;
//     //
//     //         var timeElapsed = nowTime - r.lastDrawAt;
//     //         var callAfterLimit = (timeElapsed + r.lastRedrawTime) >= minRedrawLimit;
//     //
//     //
//     //         //if( !forcedContext ) {
//     //             if( !callAfterLimit ){
//     //                 r.skipFrame = true;
//     //                 console.log( 'skip', timeElapsed, minRedrawLimit);
//     //                 return;
//     //             }
//     //         //}
//     //         console.log( 'draw', timeElapsed, minRedrawLimit);
//     //
//     //         r.lastDrawAt = nowTime;
//     //         r.requestedFrame = true;
//     //         r.renderOptions = roptions;
//     //
//     // };
//     //
//
//     /** adapts spacegraph node to cytoscape node */
//     function spacegraphToCytoscape(d) {
//         var w = { data: d };
//
//         //var css = w.css = d.style || {};
//
//         //if (d.shape) {
//         //css.shape = d.shape;
//         //}
//
//
//
//         return w;
//     }
//
//     s.updateNodeWidget = function(node, nodeOverride) {
//
//         node = nodeOverride || node;
//
//         var widget = node.data('widget');
//         if (widget)
//             s.positionNodeHTML(node, widget);
//
//     };
//
//
//     /** html=html dom element */
//     s.positionNodeHTML = function(node, widget) {
//         var h = widget.element || widget;
//
//         var pixelScale=widget.pixelScale,
//             minPixels= widget.minPixels;
//
//         pixelScale = parseFloat(pixelScale) || 128.0; //# pixels wide
//
//         var pw, ph;
//
//         try {
//             pw = parseFloat(node.renderedWidth());
//             ph = parseFloat(node.renderedHeight());
//         }
//         catch (e) {
//
//             return;
//
//         }
//
//
//
//         var scale = parseFloat(widget.scale) || 1.0;
//
//         var cw, ch;
//         var narrower = parseInt(pixelScale);
//         if (pw < ph) {
//             cw = narrower;
//             ch = parseInt(pixelScale*(ph/pw));
//         }
//         else {
//             ch = narrower;
//             cw = parseInt(pixelScale*(pw/ph));
//         }
//
//
//
//
//         //get the effective clientwidth/height if it has been resized
//         var html = widget; //HACK
//         var hs = h.style;
//         if ((( html.specWidth !== hs.width ) || (html.specHeight !== hs.height))) {
//             var hcw = h.clientWidth;
//             var hch = h.clientHeight;
//
//             html.specWidth = hs.width = cw;
//             html.specHeight = hs.height = ch;
//
//             cw = hcw;
//             ch = hch;
//         }
//         if (minPixels) {
//             var hidden = ('none' === hs.display);
//
//             if (Math.min(wy, wx) < minPixels / pixelScale) {
//                 if (!hidden) {
//                     hs.display = 'none';
//                     return;
//                 }
//             }
//             else {
//                 if (hidden) {
//                     hs.display = 'block';
//                 }
//             }
//         }
//
//
//         //console.log(html[0].clientWidth, cw, html[0].clientHeight, ch);
//
//         var pos = node.renderedPosition();
//
//         var globalToLocalW = pw / cw;
//         var globalToLocalH = ph / ch;
//
//         var wx = scale * globalToLocalW;
//         var wy = scale * globalToLocalH;
//
//
//         //TODO check extents to determine node visibility for hiding off-screen HTML
//         //for possible improved performance
//
//
//
//         //console.log(html, pos.x, pos.y, minPixels, pixelScale);
//
//         var transformPrecision = 3;
//
//         var matb = 0, matc = 0;
//         wx = wx.toPrecision(transformPrecision);
//         wy = wy.toPrecision(transformPrecision);
//
//         //parseInt here to reduce precision of numbers for constructing the matrix string
//         //TODO replace this with direct matrix object construction involving no string ops
//
//         var halfScale = scale/2.0;
//         var px = (pos.x - (halfScale*pw)).toPrecision(transformPrecision);
//         var py = (pos.y - (halfScale*ph)).toPrecision(transformPrecision);
//
//         //px = parseInt(pos.x - pw / 2.0 + pw * paddingScale / 2.0); //'e' matrix element
//         //py = parseInt(pos.y - ph / 2.0 + ph * paddingScale / 2.0); //'f' matrix element
//         //px = pos.x;
//         //py = pos.y;
//
//         //nextCSS['transform'] = tt;
//         //html.css(nextCSS);
//
//         //TODO non-String way to do this
//         hs.transform = 'matrix(' + wx+ ',' + matb + ',' + matc + ',' + wy + ',' + px + ',' + py + ')';;
//     };
//
//     // s.nodeProcessor = [];
//     //
//     // s.updateNode = function(n) {
//     //     var np = s.nodeProcessor;
//     //     for (var i = 0; i < np.length; i++)
//     //         np[i].apply(n);
//     // };
//     //
//     // s.addNode = function(n) {
//     //     var ee = s.get(n.id);
//     //     var se = spacegraphToCytoscape(n);
//     //
//     //     if (!ee) {
//     //         var added = s.add(se);
//     //         added.position({x: Math.random(), y: Math.random() });
//     //
//     //     } else {
//     //         ee.data(se);
//     //
//     //         if (n.style) {
//     //             _.each(n.style, function(v, k) {
//     //                 ee.css(k, v);
//     //             });
//     //         }
//     //
//     //
//     //     }
//     // };
//     //
//     // s.addNodes = function(nn) {
//     //     //HACK create a temporary channel and run through addChannel
//     //     // var cc = new Channel({
//     //     //     nodes: nn
//     //     // });
//     //     // this.addChannel(cc);
//     //     // return cc;
//     //
//     //     for (var n of nn)
//     //         s.addNode(n);
//     // };
//     //
//     //
//     // s.removeEdge= function(e) {
//     //     var ee = s.get(e);
//     //
//     //     if (ee) {
//     //         ee.remove();
//     //         return true;
//     //     }
//     //     return false;
//     // };
//     //
//     //
//     // s.addEdge = function(e) {
//     //     return s.addNode(e);
//     //     // var ee = s.get(e.id);
//     //     // var se = spacegraphToCytoscape(e);
//     //     // if (!ee) {
//     //     //     //s.addEdges([e]);
//     //     //     s.add(se);
//     //     //     return true;
//     //     // } else {
//     //     //     ee.data(se);
//     //     //     return false;
//     //     // }
//     // };
//     //
//     // s.addEdges = function(ee) {
//     //
//     //     for (var e of ee)
//     //         s.addEdge(e);
//     //
//     // };
//     //
//     // s.updateChannel = function(c) {
//     //
//     //     //TODO assign channel reference to each edge as done above with nodes
//     //
//     //     var that = this;
//     //
//     //     s.batch(function() {
//     //
//     //         var e = {
//     //             nodes: c.data.nodes ? c.data.nodes.map(spacegraphToCytoscape) : [], // c.data.nodes,
//     //             edges: c.data.edges ? c.data.edges.map(spacegraphToCytoscape) : [] //c.data.edges
//     //         };
//     //
//     //         if (c.data.style) {
//     //             var s = [       ];
//     //             for (var sel in c.data.style) {
//     //                 s.push({
//     //                     selector: sel,
//     //                     css: c.data.style[sel]
//     //                 });
//     //             }
//     //
//     //             if (s.length > 0) {
//     //                 //TODO merge style; this will replace it with c's
//     //
//     //                 var style = that.style();
//     //                 style.clear();
//     //
//     //                 style.fromJson(s);
//     //                 style.update();
//     //             }
//     //         }
//     //
//     //         /*
//     //          for (var i = 0; i < e.nodes.length; i++) {
//     //          var n = e.nodes[i];
//     //          if (n.data && n.data.id)
//     //          that.remove('#' + n.data.id);
//     //          }
//     //          */
//     //
//     //         that.add( e );
//     //
//     //         var channelID = c.id();
//     //
//     //         //add position if none exist
//     //         for (var i = 0; i < e.nodes.length; i++) {
//     //             var n = e.nodes[i];
//     //             var ndata = n.data;
//     //             if (ndata && ndata.id) {
//     //                 var nn = that.nodes('[id="' + ndata.id + '"]');
//     //
//     //                 nn.addClass(channelID);
//     //
//     //                 that.updateNode(nn);
//     //
//     //                 var ep = nn.position();
//     //                 if (!ep || !ep.x) {
//     //                     //var ex = that.extent();
//     //                     var cx = Math.random() * 2 - 1; // * (ex.x1 + ex.x2);
//     //                     var cy = Math.random() * 2 - 1; // * (ex.y1 + ex.y2);
//     //
//     //                     //try {
//     //                         nn.position({x: cx, y: cy});
//     //                     //} catch (e) { }
//     //                 }
//     //             }
//     //         }
//     //
//     //         that.resize();
//     //
//     //         //suppressCommit = false;
//     //
//     //     });
//     //
//     // };
//     //
//     // // /** set and force layout update */
//     // // s.setLayout = function(l){
//     // //
//     // //     if (this.currentLayout)
//     // //         if (this.currentLayout.stop)
//     // //             this.currentLayout.stop();
//     // //
//     // //     var layout;
//     // //     if (l.name) {
//     // //         layout = this.makeLayout(l);
//     // //     }
//     // //     else {
//     // //         layout = l;
//     // //     }
//     // //
//     // //     this.currentLayout = layout;
//     // //
//     // //     if (layout)
//     // //         layout.run();
//     // //
//     // //
//     // //     //this.layout(this.currentLayout);
//     // //
//     // //     /*if (this.currentLayout.eles)
//     // //         delete this.currentLayout.eles;*/
//     // //
//     // //     //http://js.cytoscape.org/#layouts
//     // //
//     // //
//     // // };
//     //
//     // /** depreated */
//     // s.addChannel = function(c) {
//     //
//     //     var cid = c.id();
//     //
//     //     //var nodesBefore = this.nodes().size();
//     //     var existing = s.channels[cid];
//     //     if (existing) {
//     //         if (existing == c) {
//     //             this.updateChannel(c);
//     //             return;
//     //         } else {
//     //             this.removeChannel(existing);
//     //         }
//     //     }
//     //
//     //
//     //     this.channels[cid] = c;
//     //
//     //
//     //     this.updateChannel(c);
//     //
//     //     var that = this;
//     //     var l;
//     //     c.on('graphChange', function(graph, nodesAdded, edgesAdded, nodesRemoved, edgesRemoved) {
//     //         "use strict";
//     //         that.updateChannel(c);
//     //     });
//     //
//     //     /*if (s.currentLayout)
//     //         this.setLayout(s.currentLayout);*/
//     //
//     // };
//     //
//     // s.clear = function() {
//     //     for (var c in this.channels) {
//     //         var chan = s.channels[c];
//     //         this.removeChannel(chan);
//     //     }
//     // };
//
//     /** get an element (node or edge) */
//     s.get = function(id) {
//         //return s.nodes()._private.ids[id];
//
//         return s._private.elements._private.ids[id];
//     };
//
//
//
//     // s.removeNode = function(id) {
//     //     s.get(id).remove();
//     // };
//     //
//     // s.removeNodes = function(ids) {
//     //     //TODO use the right multi selector
//     //     var that = this;
//     //     that.batch(function() {
//     //         _.each(ids, function(id) {
//     //             that.removeNode(id);
//     //         });
//     //     });
//     // };
//     //
//     // s.removeChannel = function(c) {
//     //
//     //     c.off();
//     //
//     //     s.removeNode(c.id());
//     //
//     //     //TODO remove style
//     //
//     //     delete s.channels[c.id()];
//     //     //c.destroy();
//     //
//     //     //s.layout();
//     //
//     // };
//     //
//     // s.commit = _.throttle(function() {
//     //     var cc = s.channels;
//     //     for (var i in cc)
//     //         cc[i].commit();
//     // }, commitPeriodMS);
//     //
//     // // ----------------------
//
//     s.on('cxttapstart', function(e) {
//         var target = e.cyTarget;
//         this.zoomTo(!target ? undefined : target)
//     });
//
//     s.on('add', function(e) {
//
//         //var node = e.cyTarget;
//
//         //var widget = node.data('widget');
//         // if (widget) {
//         //
//         //     var nid = node.id();
//         //
//         //     var wEle = widget.element;
//         //     if (!wEle) { //if widget doesnt already exists
//         //
//         //         var style = widget.style || {};
//         //         style.position = 'fixed';
//         //         style.transformOrigin = '0 0';
//         //
//         //         var wid = 'widget_' + nid;
//         //         var w = $(document.createElement('div')).attr('id', wid).addClass('widget').css(style).appendTo(overlaylayer);
//         //
//         //         w.html(widget.html).data('when', Date.now());
//         //
//         //         widget.element = w[0];
//         //
//         //         updateWidget(node);
//         //     }
//         //}
//
//         //OTHER content handlers
//
//         //     var n = null;
//         //     if (type === 'text') {
//         //         n = {
//         //             id: 'txt' + parseInt(Math.random() * 1000),
//         //             style: {
//         //                 width: 64,
//         //                 height: 32
//         //             },
//         //             widget: {
//         //                 html: "<div contenteditable='true' class='editable' style='width: 100%; height: 100%; overflow:auto'></div>",
//         //                 scale: 0.85,
//         //                 style: {}
//         //             }
//         //         };
//         //     }
//         //     else if (type === 'www') {
//         //         var uurrll = param;
//         //         if (!uurrll.indexOf('http://') === 0)
//         //             uurrll = 'http://' + uurrll;
//         //
//         //         n = {
//         //             id: 'www' + parseInt(Math.random() * 1000),
//         //             style: {
//         //                 width: 64,
//         //                 height: 64
//         //             },
//         //             widget: {
//         //                 html: '<iframe width="100%" height="100%" src="' + uurrll + '"></iframe>',
//         //                 scale: 0.85,
//         //                 pixelScale: 600,
//         //                 style: {},
//         //             }
//         //         };
//         //     }
//         //
//         //     //
//         //
//         //     if (n) {
//         //
//         //         c.addNode(n);
//         //
//         //         this.updateChannel(c);
//         //
//         //         /*
//         //          if (!pos) {
//         //          var ex = this.extent();
//         //          var cx = 0.5 * (ex.x1 + ex.x2);
//         //          var cy = 0.5 * (ex.y1 + ex.y2);
//         //          pos = {x: cx, y: cy};
//         //          }
//         //          */
//         //         if (pos)
//         //             this.getElementById(n.id).position(pos);
//         //
//         //         c.commit();
//         //
//         //     }
//
//     });
//
//     if (frame) {
//         s.on('remove', function (e) {
//             var node = e.cyTarget;
//
//             var widget = node.data('widget');
//             if (widget) {
//                 //remove an associated widget (html div in overlay)
//                 widget.element.remove();
//             }
//         });
//     }
//
//
//
//     s.zoomTo = function(ele) {
//         // var pos;
//         // if (!ele || !ele.position)
//         //     pos = { x: 0, y: 0 };
//         // else
//         //     pos = ele.position();
//
//         this.animate({
//             fit: {
//                 eles: ele,
//                 padding: 120
//             }
//         }, {
//             duration: zoomDuration
//             /*step: function() {
//              }*/
//         });
//     };
//
//
//     // //enable popup menu
//     // newSpacePopupMenu(s);
//
//
//     return s;
// }

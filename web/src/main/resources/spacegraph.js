
"use strict";

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


class Channel extends EventEmitter {

    //EVENTS
    //.on("graphChange", function(graph, nodesAdded, edgesAdded, nodesRemoved, edgesRemoved) {

    constructor(initialData) {
        super();

        this.ui = null;

        this.prev = { };
        this.commit = function() { }; //empty

        //set channel name
        if (typeof(initialData)==="string")
            initialData = { id: initialData };

        this.data = initialData || { };
        if (!this.data.id) {
            //assign random uuid
            this.data.id = uuid();
        }

        if (!this.data.nodes) this.data.nodes =[];
        if (!this.data.edges) this.data.edges =[];

        var u = uuid();
        var uc = 0;

        var ensureID = function(x) {
            if (!x.id) x.id = u + (uc++);
        };

        //assign unique uuid to any nodes missing an id
        _.each(this.data.nodes, ensureID);
        _.each(this.data.edges, ensureID);

    }


    init(ui) {
        this.ui = ui;
    }

    id() {
        return this.data.id;
    }

    clear() {
        //TODO
    }


    removeNode(n) {
        n.data().removed = true;

        var removedAny = false;
        var id = n.data().id;
        this.data.nodes = _.filter(this.data.nodes, function(e) {
            if (e.id === id) {
                removedAny = true;
                return false;
            }
        });

        if (removedAny)
            this.emit('graphChange', [this, null, null, n, null]);

        return removedAny;
    }

    //TODO: removeEdge

    //TODO batch version of addNode([n])
    addNode(n) {
        this.data.nodes.push(n);
        this.emit('graphChange', [this, [n], null, null, null]);
    }

    addEdge(e) {
        this.data.edges.push(e);
        this.emit('graphChange', [this, null, [e], null, null]);
    }


    //nodes and edges are arrays
    add(nodes, edges) {
        var that = this;
        _.each(nodes, function(n) { that.data.nodes.push(n); });
        _.each(edges, function(e) { that.data.edges.push(e); });
        //nodes.forEach(this.data().nodes.push); //??
        //edges.forEach(this.data().edges.push);
        this.emit('graphChange', [this, nodes, edges, null, null]);
    }
}


function cytoscapeOptions(opt, ready, target) {

    return _.defaults(opt, {
        ready: ready,
        // initial viewport state:
        zoom: 1,
        pan: {x: 0, y: 0},
        // interaction options:
        minZoom: 1e-50,
        maxZoom: 1e50,
        zoomingEnabled: true,
        userZoomingEnabled: true,
        panningEnabled: true,
        userPanningEnabled: true,
        selectionType: 'single',
        boxSelectionEnabled: false,
        autolock: false,
        autoungrabify: false,
        autounselectify: true,
        //fps: 25, //target max fps (frames per second)
        // rendering options:
        headless: false,
        styleEnabled: true,
        hideEdgesOnViewport: false,
        hideLabelsOnViewport: false,
        textureOnViewport: true, //true = higher performance, lower quality
        motionBlur: false,
        wheelSensitivity: 1,
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
                    'background-color': '#888',
                    'label': 'data(label)',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'color': '#fff',
                    'font-family':  //keep short because this gets repeated as part of strings in the style system badly
                        //'Arial',
                        'Monospace',
                    //'outside-texture-bg-opacity': 1,
                    'shadow-blur': 0,
                    'text-shadow-blur': 0,
                    'shadow-opacity': 1,
                    'min-zoomed-font-size': 5,
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
function spacegraph(targetWrapper, opt) {

    targetWrapper.addClass("spacegraph");


    //var overlaylayer = $('<div class="overlay"></div>').prependTo(targetWrapper);

    //where cytoscape renders to:
    var target = $('<div class="graph"/>')
                    .attr('oncontextmenu', "return false;")
                    .appendTo(targetWrapper);

    var suppressCommit = false;
    var zoomDuration = 128; //ms

    var frame =
        //NodeFrame(this);
        null;


    var ready = function() {


        //opt.start.apply(this);

        //http://js.cytoscape.org/#events/collection-events

        //overlay framenode --------------

        var that = this;


        if (frame) {


            const updateWidget = function(target, target2) {
                if (target2)
                    target = target2; //extra parameter to match the callee's list

                //if (widget(target)) {
                //var data = target.data();
                s.updateNodeWidget(target); //that.updateNodeWidget(target);
                //if (!data.updating) {
                //data.updating = setTimeout(s.updateNodeWidget, widgetUpdatePeriodMS, target); //that.updateNodeWidget(target);
                //}
                //}
            }

            this.updateAllWidgets = (/*_.throttle(*/function () {

                if (suppressCommit)
                    return;

                that.nodes().filterFn(function (ele) {
                    if (ele.data('widget') !== undefined)
                        updateWidget(ele);
                });
                //that.each(refresh);

            }); //, widgetUpdatePeriodMS);

            this.on('data position select unselect add remove grab drag style', function (e) {

                if (suppressCommit)
                    return;

                /*console.log( evt.data.foo ); // 'bar'

                 var node = evt.cyTarget;
                 console.log( 'tapped ' + node.id() );*/

                var target = e.cyTarget;
                var widget = target.data('widget');
                if (widget) {
                    setTimeout(updateWidget, 0, target);
                    //console.log(this, that, target);
                    //that.commit();
                }

            });

            if (this.updateAllWidgets)
                this.on('layoutstop pan zoom', this.updateAllWidgets);

        }







        /*
         var baseRedraw = this._private.renderer.redraw;

         this._private.renderer.redraw = function(options) {
         baseRedraw.apply(this, arguments);

         //frame.hoverUpdate();

         };
         */


        /*var baseDrawNode = this._private.renderer.drawNode;
         this._private.renderer.drawNode = function (context, node, drawOverlayInstead) {
         baseDrawNode.apply(this, arguments);
         };*/




//            function scaleAndTranslate( _element , _x , _y, wx, wy )  {
//                
//                var mat = _element.style.transform.baseVal.getItem(0).matrix;
//                // [1 0 0 1 tx ty], 
//                mat.a = wx; mat.b = 0; mat.c = 0; mat.d = wy; mat.e = _x; mat.f = _y;
//                
//            }        
    };


    opt = cytoscapeOptions(opt, ready, target);


    var s = cytoscape(opt);

    if (opt.edgehandles) {
        // EdgeHandler: the default values of each option are outlined below:
        s.edgehandles(opt.edgehandles);
    }


//    s.noderesize({
//      handleColor: '#000000', // the colour of the handle and the line drawn from it
//      hoverDelay: 150, // time spend over a target node before it is considered a target selection
//      enabled: false, // whether to start the plugin in the enabled state
//      minNodeWidth: 30,
//      minNodeHeight: 30,
//      triangleSize: 10,
//      lines: 3,
//      padding: 5,
//
//      start: function(sourceNode) {
//        console.log('resize', sourceNode);
//        // fired when noderesize interaction starts (drag on handle)
//      },
//      complete: function(sourceNode, targetNodes, addedEntities) {
//        // fired when noderesize is done and entities are added
//      },
//      stop: function(sourceNode) {
//        // fired when noderesize interaction is stopped (either complete with added edges or incomplete)
//      }
//    });

    //s.channels = { };
    //s.overlay = overlaylayer;


    //var ren = s.renderer();

    /*var time;
    function draw() {
        requestAnimationFrame(draw);
        var now = new Date().getTime(),
            dt = now - (time || now);

        time = now;

        // Drawing code goes here... for example updating an 'x' position:
        this.x += 10 * dt; // Increase 'x' by 10 units per millisecond
    }*/

    // //IMPROVED CANVAS RENDERER FUNCTION THAT CAN THROTTLE FPS
    // s.renderer().redraw = function(roptions) {
    //
    //
    //         var minRedrawLimit = 1000.0/opt.fps; // people can't see much better than 60fps
    //         //var maxRedrawLimit = 1000.0;  // don't cap max b/c it's more important to be responsive than smooth
    //
    //         roptions = roptions || {}; //util.staticEmptyObject();
    //
    //         var r = this;
    //
    //         if( r.averageRedrawTime === undefined ){ r.averageRedrawTime = 0; }
    //         if( r.lastRedrawTime === undefined ){ r.lastRedrawTime = 0; }
    //
    //         //var redrawLimit = r.lastRedrawTime; // estimate the ideal redraw limit based on how fast we can draw
    //         //redrawLimit = minRedrawLimit > redrawLimit ? minRedrawLimit : redrawLimit;
    //         //redrawLimit = redrawLimit < maxRedrawLimit ? redrawLimit : maxRedrawLimit;
    //
    //
    //         var nowTime = Date.now();
    //         if (r.lastDrawAt === undefined) r.lastDrawAt = 0;
    //
    //         var timeElapsed = nowTime - r.lastDrawAt;
    //         var callAfterLimit = (timeElapsed + r.lastRedrawTime) >= minRedrawLimit;
    //
    //
    //         //if( !forcedContext ) {
    //             if( !callAfterLimit ){
    //                 r.skipFrame = true;
    //                 console.log( 'skip', timeElapsed, minRedrawLimit);
    //                 return;
    //             }
    //         //}
    //         console.log( 'draw', timeElapsed, minRedrawLimit);
    //
    //         r.lastDrawAt = nowTime;
    //         r.requestedFrame = true;
    //         r.renderOptions = roptions;
    //
    // };
    //

    /** adapts spacegraph node to cytoscape node */
    function spacegraphToCytoscape(d) {
        var w = { data: d };

        //var css = w.css = d.style || {};

        //if (d.shape) {
            //css.shape = d.shape;
        //}



        return w;
    }

    s.updateNodeWidget = function(node, nodeOverride) {

        node = nodeOverride || node;

        var widget = node.data('widget');
        if (widget)
            s.positionNodeHTML(node, widget);

    };


    /** html=html dom element */
    s.positionNodeHTML = function(node, widget) {
        var h = widget.element || widget;

        var pixelScale=widget.pixelScale,
            minPixels= widget.minPixels;

        pixelScale = parseFloat(pixelScale) || 128.0; //# pixels wide

        var pw, ph;

        try {
            pw = parseFloat(node.renderedWidth());
            ph = parseFloat(node.renderedHeight());
        }
        catch (e) {
            
            return;

        }



        var scale = parseFloat(widget.scale) || 1.0;

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
        var html = widget; //HACK
        var hs = h.style;
        if ((( html.specWidth !== hs.width ) || (html.specHeight !== hs.height))) {
            var hcw = h.clientWidth;
            var hch = h.clientHeight;

            html.specWidth = hs.width = cw;
            html.specHeight = hs.height = ch;

            cw = hcw;
            ch = hch;
        }
        if (minPixels) {
            var hidden = ('none' === hs.display);

            if (Math.min(wy, wx) < minPixels / pixelScale) {
                if (!hidden) {
                    hs.display = 'none';
                    return;
                }
            }
            else {
                if (hidden) {
                    hs.display = 'block';
                }
            }
        }


        //console.log(html[0].clientWidth, cw, html[0].clientHeight, ch);

        var pos = node.renderedPosition();

        var globalToLocalW = pw / cw;
        var globalToLocalH = ph / ch;

        var wx = scale * globalToLocalW;
        var wy = scale * globalToLocalH;


        //TODO check extents to determine node visibility for hiding off-screen HTML
        //for possible improved performance



        //console.log(html, pos.x, pos.y, minPixels, pixelScale);

        var transformPrecision = 3;

        var matb = 0, matc = 0;
        wx = wx.toPrecision(transformPrecision);
        wy = wy.toPrecision(transformPrecision);

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
        hs.transform = 'matrix(' + wx+ ',' + matb + ',' + matc + ',' + wy + ',' + px + ',' + py + ')';;
    };

    // s.nodeProcessor = [];
    //
    // s.updateNode = function(n) {
    //     var np = s.nodeProcessor;
    //     for (var i = 0; i < np.length; i++)
    //         np[i].apply(n);
    // };
    //
    // s.addNode = function(n) {
    //     var ee = s.get(n.id);
    //     var se = spacegraphToCytoscape(n);
    //
    //     if (!ee) {
    //         var added = s.add(se);
    //         added.position({x: Math.random(), y: Math.random() });
    //
    //     } else {
    //         ee.data(se);
    //
    //         if (n.style) {
    //             _.each(n.style, function(v, k) {
    //                 ee.css(k, v);
    //             });
    //         }
    //
    //
    //     }
    // };
    //
    // s.addNodes = function(nn) {
    //     //HACK create a temporary channel and run through addChannel
    //     // var cc = new Channel({
    //     //     nodes: nn
    //     // });
    //     // this.addChannel(cc);
    //     // return cc;
    //
    //     for (var n of nn)
    //         s.addNode(n);
    // };
    //
    //
    // s.removeEdge= function(e) {
    //     var ee = s.get(e);
    //
    //     if (ee) {
    //         ee.remove();
    //         return true;
    //     }
    //     return false;
    // };
    //
    //
    // s.addEdge = function(e) {
    //     return s.addNode(e);
    //     // var ee = s.get(e.id);
    //     // var se = spacegraphToCytoscape(e);
    //     // if (!ee) {
    //     //     //s.addEdges([e]);
    //     //     s.add(se);
    //     //     return true;
    //     // } else {
    //     //     ee.data(se);
    //     //     return false;
    //     // }
    // };
    //
    // s.addEdges = function(ee) {
    //
    //     for (var e of ee)
    //         s.addEdge(e);
    //
    // };
    //
    // s.updateChannel = function(c) {
    //
    //     //TODO assign channel reference to each edge as done above with nodes
    //
    //     var that = this;
    //
    //     s.batch(function() {
    //
    //         var e = {
    //             nodes: c.data.nodes ? c.data.nodes.map(spacegraphToCytoscape) : [], // c.data.nodes,
    //             edges: c.data.edges ? c.data.edges.map(spacegraphToCytoscape) : [] //c.data.edges
    //         };
    //
    //         if (c.data.style) {
    //             var s = [       ];
    //             for (var sel in c.data.style) {
    //                 s.push({
    //                     selector: sel,
    //                     css: c.data.style[sel]
    //                 });
    //             }
    //
    //             if (s.length > 0) {
    //                 //TODO merge style; this will replace it with c's
    //
    //                 var style = that.style();
    //                 style.clear();
    //
    //                 style.fromJson(s);
    //                 style.update();
    //             }
    //         }
    //
    //         /*
    //          for (var i = 0; i < e.nodes.length; i++) {
    //          var n = e.nodes[i];
    //          if (n.data && n.data.id)
    //          that.remove('#' + n.data.id);
    //          }
    //          */
    //
    //         that.add( e );
    //
    //         var channelID = c.id();
    //
    //         //add position if none exist
    //         for (var i = 0; i < e.nodes.length; i++) {
    //             var n = e.nodes[i];
    //             var ndata = n.data;
    //             if (ndata && ndata.id) {
    //                 var nn = that.nodes('[id="' + ndata.id + '"]');
    //
    //                 nn.addClass(channelID);
    //
    //                 that.updateNode(nn);
    //
    //                 var ep = nn.position();
    //                 if (!ep || !ep.x) {
    //                     //var ex = that.extent();
    //                     var cx = Math.random() * 2 - 1; // * (ex.x1 + ex.x2);
    //                     var cy = Math.random() * 2 - 1; // * (ex.y1 + ex.y2);
    //
    //                     //try {
    //                         nn.position({x: cx, y: cy});
    //                     //} catch (e) { }
    //                 }
    //             }
    //         }
    //
    //         that.resize();
    //
    //         //suppressCommit = false;
    //
    //     });
    //
    // };
    //
    // // /** set and force layout update */
    // // s.setLayout = function(l){
    // //
    // //     if (this.currentLayout)
    // //         if (this.currentLayout.stop)
    // //             this.currentLayout.stop();
    // //
    // //     var layout;
    // //     if (l.name) {
    // //         layout = this.makeLayout(l);
    // //     }
    // //     else {
    // //         layout = l;
    // //     }
    // //
    // //     this.currentLayout = layout;
    // //
    // //     if (layout)
    // //         layout.run();
    // //
    // //
    // //     //this.layout(this.currentLayout);
    // //
    // //     /*if (this.currentLayout.eles)
    // //         delete this.currentLayout.eles;*/
    // //
    // //     //http://js.cytoscape.org/#layouts
    // //
    // //
    // // };
    //
    // /** depreated */
    // s.addChannel = function(c) {
    //
    //     var cid = c.id();
    //
    //     //var nodesBefore = this.nodes().size();
    //     var existing = s.channels[cid];
    //     if (existing) {
    //         if (existing == c) {
    //             this.updateChannel(c);
    //             return;
    //         } else {
    //             this.removeChannel(existing);
    //         }
    //     }
    //
    //
    //     this.channels[cid] = c;
    //
    //
    //     this.updateChannel(c);
    //
    //     var that = this;
    //     var l;
    //     c.on('graphChange', function(graph, nodesAdded, edgesAdded, nodesRemoved, edgesRemoved) {
    //         "use strict";
    //         that.updateChannel(c);
    //     });
    //
    //     /*if (s.currentLayout)
    //         this.setLayout(s.currentLayout);*/
    //
    // };
    //
    // s.clear = function() {
    //     for (var c in this.channels) {
    //         var chan = s.channels[c];
    //         this.removeChannel(chan);
    //     }
    // };

    /** get an element (node or edge) */
    s.get = function(id) {
        //return s.nodes()._private.ids[id];

        return s._private.elements._private.ids[id];
    };



    // s.removeNode = function(id) {
    //     s.get(id).remove();
    // };
    //
    // s.removeNodes = function(ids) {
    //     //TODO use the right multi selector
    //     var that = this;
    //     that.batch(function() {
    //         _.each(ids, function(id) {
    //             that.removeNode(id);
    //         });
    //     });
    // };
    //
    // s.removeChannel = function(c) {
    //
    //     c.off();
    //
    //     s.removeNode(c.id());
    //
    //     //TODO remove style
    //
    //     delete s.channels[c.id()];
    //     //c.destroy();
    //
    //     //s.layout();
    //
    // };
    //
    // s.commit = _.throttle(function() {
    //     var cc = s.channels;
    //     for (var i in cc)
    //         cc[i].commit();
    // }, commitPeriodMS);
    //
    // // ----------------------

    s.on('cxttapstart', function(e) {
        var target = e.cyTarget;
        this.zoomTo(!target ? undefined : target)
    });

    s.on('add', function(e) {

        //var node = e.cyTarget;

        //var widget = node.data('widget');
        // if (widget) {
        //
        //     var nid = node.id();
        //
        //     var wEle = widget.element;
        //     if (!wEle) { //if widget doesnt already exists
        //
        //         var style = widget.style || {};
        //         style.position = 'fixed';
        //         style.transformOrigin = '0 0';
        //
        //         var wid = 'widget_' + nid;
        //         var w = $(document.createElement('div')).attr('id', wid).addClass('widget').css(style).appendTo(overlaylayer);
        //
        //         w.html(widget.html).data('when', Date.now());
        //
        //         widget.element = w[0];
        //
        //         updateWidget(node);
        //     }
        //}

        //OTHER content handlers

        //     var n = null;
        //     if (type === 'text') {
        //         n = {
        //             id: 'txt' + parseInt(Math.random() * 1000),
        //             style: {
        //                 width: 64,
        //                 height: 32
        //             },
        //             widget: {
        //                 html: "<div contenteditable='true' class='editable' style='width: 100%; height: 100%; overflow:auto'></div>",
        //                 scale: 0.85,
        //                 style: {}
        //             }
        //         };
        //     }
        //     else if (type === 'www') {
        //         var uurrll = param;
        //         if (!uurrll.indexOf('http://') === 0)
        //             uurrll = 'http://' + uurrll;
        //
        //         n = {
        //             id: 'www' + parseInt(Math.random() * 1000),
        //             style: {
        //                 width: 64,
        //                 height: 64
        //             },
        //             widget: {
        //                 html: '<iframe width="100%" height="100%" src="' + uurrll + '"></iframe>',
        //                 scale: 0.85,
        //                 pixelScale: 600,
        //                 style: {},
        //             }
        //         };
        //     }
        //
        //     //
        //
        //     if (n) {
        //
        //         c.addNode(n);
        //
        //         this.updateChannel(c);
        //
        //         /*
        //          if (!pos) {
        //          var ex = this.extent();
        //          var cx = 0.5 * (ex.x1 + ex.x2);
        //          var cy = 0.5 * (ex.y1 + ex.y2);
        //          pos = {x: cx, y: cy};
        //          }
        //          */
        //         if (pos)
        //             this.getElementById(n.id).position(pos);
        //
        //         c.commit();
        //
        //     }

    });

    if (frame) {
        s.on('remove', function (e) {
            var node = e.cyTarget;

            var widget = node.data('widget');
            if (widget) {
                //remove an associated widget (html div in overlay)
                widget.element.remove();
            }
        });
    }



    s.zoomTo = function(ele) {
        // var pos;
        // if (!ele || !ele.position)
        //     pos = { x: 0, y: 0 };
        // else
        //     pos = ele.position();

        this.animate({
            fit: {
                eles: ele,
                padding: 120
            }
        }, {
            duration: zoomDuration
            /*step: function() {
            }*/
        });
    };


    // //enable popup menu
    // newSpacePopupMenu(s);


    return s;
}

"use strict";

function NodeFrame(spacegraph) {

    const f = {
        hoverUpdate: function () { /* implemented below */
        },
        hide: function () { /* see below */
        },
        hovered: null
    };

    const frameTimeToFade = 200; //ms
    const frameNodePixelScale = 300;
    const frameNodeScale = 1.15;

    let frameVisible = false;
    let frameHiding = -1;
    let frameEleNode = null;
    let frameEleResizing = false;


    // $.ajax({
    //     url: "frame.html",
    //     //data: 'foo',
    //     success: function(){
    //         alert('bar');
    //     },
    //     cache: false
    // });
    $.get('frame.html',
        //{ "_": $.now() /* overrides cache */},
        function (x) {

        spacegraph.overlay.append(x);

        const frameEle = $('#nodeframe');
        f.hovered = null;


        initFrameDrag(f);


        //http://threedubmedia.com/code/event/drag/demo/resize2

        spacegraph.on('zoom', function () {
            setTimeout(f.hoverUpdate, 0);
        });

        spacegraph.on('mouseover mouseout mousemove', function (e) {

            let target = e.cyTarget;
            let over = (e.type !== "mouseout");

            if (frameEleResizing) {
                target = frameEleNode;
                frameEle.show();
                over = true;
            }

            if (target && target.isNode && target.isNode()) {
                if ((over || (f.hovered !== target)) && (frameHiding !== -1)) {
                    //cancel any hide fade
                    clearTimeout(frameHiding);
                    frameHiding = -1;
                }

                if (f.hovered !== target) {

                    /*if (f.hovered) {
                        f.hovered.unlock(); //TODO restore the state it was before locking, which could have been already locked (then we dont want to unlock it)
                    }*/

                    frameEle.hide();
                    frameVisible = true;
                    frameEle[0].style.width = undefined; //reset width
                    frameEle.show();
                    f.hovered = target;

                    //f.hovered.lock();
                }
            } else {
                frameVisible = false;
            }


            f.hoverUpdate();
            //setTimeout(f.hoverUpdate, 0);

        });

        f.hide = function () {
            frameVisible = false;
            this.hovered = null;
            f.hoverUpdate();
            //setTimeout(f.hoverUpdate, 0);


            //TODO fadeOut almost works, but not completely. so hide() for now
            frameEle.hide();
        };

        f.hoverUpdate = function () {

            const that = this;
            if (frameVisible) {
                if (!this.currentlyVisible) {
                    this.currentlyVisible = true;
                    frameEle[0].style.width = undefined; //reset width
                    frameEle.show();
                }
            }
            else {
                if ((frameHiding === -1) && (this.currentlyVisible) && !frameEleResizing) {
                    frameHiding = setTimeout(function () {
                        //if still set for hiding, actually hide it
                        if (!frameEleResizing && !frameVisible) {
                            frameEle.fadeOut(function () {
                                f.hovered = null;
                                frameEleNode = null;
                                frameHiding = -1;
                            });
                            that.currentlyVisible = false;
                        }
                    }, frameTimeToFade);
                }
            }

            if (this.currentlyVisible && f.hovered) {
                spacegraph.positionNodeHTML(f.hovered, frameEle[0], frameNodePixelScale, frameNodeScale);
                frameEleNode = f.hovered;
            }

        };


    });

    function initFrameDrag() {
        const nodeFrame = $('#nodeframe');

        const frameEle = nodeFrame;


        const close = nodeFrame.find('#close');
        close.click(function () {
            const node = frameEleNode;
            if (node) {
                const space = node.cy();

                space.removeNode(node);

                frameEleNode = null;
                nodeFrame.hide();
            }
        });

        const sese = '#nodeframe #resizeSE';
        const se = $(sese);
        se.draggable({
                        //revert: true,
                        helper: "clone",
                        stack: sese,
                        cursor: "se-resize",
                        //appendTo: sese,
                        //appendTo: "body",

                        //http://api.jqueryui.com/draggable/#event-drag
                        start: function (event, ui) {


                            let node = frameEleNode;

                            if (!node)
                                return;

                            let pos = node.position();
                            if (!pos) {
                                console.error('node ', node, 'has no position');
                                return;
                            }

                            frameEleResizing = true;

                            this.originalNode = node;
                            this.originalPos = [parseFloat(pos.x), parseFloat(pos.y)];
                            this.originalSize = [node.width(), node.height(), node.renderedWidth(), node.renderedHeight()];
                            this.originalOffset = [ui.offset.left, ui.offset.top];

                            se.next().hide(); //hide the clone, its displayed position is confusing

                            event.stopPropagation();

                        },
                        drag: function (event, ui) {

                            event.stopPropagation();

                            const node = this.originalNode; //frameEleNode;
                            /*if (node !== this.originalNode)
                                return;*/

                            const oos = this.originalOffset;
                            const dx = parseFloat(ui.offset.left - oos[0]);
                            const dy = parseFloat(ui.offset.top - oos[1]);

                            const p = this.originalPos;
                            const os = this.originalSize;

                            const os0 = os[0];
                            const os1 = os[1];

                            const dw = dx * (os0 / os[2]);
                            const dh = dy * (os1 / os[3]);

                            const ncy = node.cy();
                            ncy.startBatch();

                            node.position({
                                x: p[0] + dw / 2.0,
                                y: p[1] + dh / 2.0
                            });
                            node.css({
                                width: os0 + dw,
                                height: os1 + dh
                            });
                            ncy.endBatch();


                            frameVisible = true;
                            nodeFrame.currentlyVisible = true;
                            f.hovered = node;
                            
                            nodeFrame.hoverUpdate();
                            //setTimeout(nodeFrame.hoverUpdate, 0);

                        },
                        stop: function (event) {
                            frameEle[0].style.width = undefined; //reset width

                            event.stopPropagation();

                            setTimeout(function() {
                                nodeFrame.hoverUpdate();
                                frameEleResizing = false;
                                se.next().show(); //unhide the clone
                            }, 0);

                        }

        });

//        $(function () {
//            //$( "#draggable" ).draggable({ revert: true });
//            se.draggable({
//
//            });
//        });
    }

    return f;
}


class PopupMenu {

    //var prevMenu = null;

    //hit background
    //this.newNode(s.defaultChannel, 'text', e.cyPosition);
    //var handler = {
    //
    //    addText: function(position) {
    //        s.newNode(s.defaultChannel, 'text', position);
    //    },
    //
    //    addURL: function(position) {
    //
    //    }
    //
    //};

    constructor(id) {
        "use strict";
        this.id = id;
    }

    hide() {
        $('#' + this.id).remove();
        if (this.onHidden) this.onHidden();
    }

    /** position = { left: .. , top: .. }  */
    show(menu, position, target) {
        "use strict";


        const that = this;

        const handler = {};

        //remove existing
        //$('#' + this.id).each(function(x) { $(x).fadeOut(function() { $(this).remove(); }); });
        this.hide();

        //add
        menu.attr('id', this.id).css('position', 'fixed').appendTo(target || $('body'));

        //http://codepen.io/MarcMalignan/full/xlAgJ/

        const radius = this.radius || 70.0;

        const cx = position.left - radius / 2.0;
        const cy = position.top - radius / 2.0;

        const items = menu.find('li');

        const closebutton = menu.find('.closebutton');
        closebutton.unbind();
        items.unbind();

        items.css({left: 0, top: 0});
        menu.hide().css({
            left: cx,
            top: cy,
            opacity: 0.0,
            display: 'block'
        }).animate({
            opacity: 1.0
        }, {
            duration: 400,
            step: function (now) {
                for (let i = 0; i < items.length; i++) {
                    let a = (i / (items.length)) * Math.PI * 2.0;
                    a += now;
                    const x = Math.cos(a) * now * radius;
                    const y = Math.sin(a) * now * radius;
                    $(items[i]).css({
                        left: x, top: y
                    });
                }
            }
        });
        items.click(function (e) {

            const a = $(e.target).attr('action');
            if (a && handler[a])
                handler[a](position);

            that.hide();
        });
        closebutton.click(function () {
            that.hide();
        });

        console.log('popup menu visible');
    }


}

function newSpacePopupMenu(s) {

    let prevMenu = null;

    //hit background
    //this.newNode(s.defaultChannel, 'text', e.cyPosition);
    const handler = {

        addText: function (position) {
            //s.newNode(s.defaultChannel, 'text', position);
        },

        addURL: function (position) {

        }

    };

    s.on('tap', function (e) {
        const target = e.cyTarget;
        const position = e.cyPosition;

        if (target.isNode) {
            //hit an element (because its isNode function is defined)
            return;
        }

        if (prevMenu) {
            //click to remove existing one, then click again to popup again
            prevMenu.destroy();
            prevMenu = null;
            return;
        }

        const menu = prevMenu = $('#ContextPopup').clone();
        menu.appendTo(s.overlay);
        menu.destroy = function () {
            if (prevMenu === this)
                prevMenu = null;
            menu.fadeOut(function () {
                menu.remove();
            });
        };



        //http://codepen.io/MarcMalignan/full/xlAgJ/

        const radius = 70.0;

        const cx = e.originalEvent.clientX - radius / 2.0;
        const cy = e.originalEvent.clientY - radius / 2.0;

        const items = menu.find('li');

        const closebutton = menu.find('.closebutton');
        closebutton.unbind();
        items.unbind();

        items.css({left: 0, top: 0});
        menu.hide().css({
            left: cx,
            top: cy,
            opacity: 0.0,
            display: 'block'
        }).animate({
            opacity: 1.0
        }, {
            duration: 400,
            step: function (now) {
                for (let i = 0; i < items.length; i++) {
                    let a = (i / (items.length)) * Math.PI * 2.0;
                    a += now;
                    const x = Math.cos(a) * now * radius;
                    const y = Math.sin(a) * now * radius;
                    $(items[i]).css({
                        left: x, top: y
                    });
                }
            }
        });
        items.click(function (e) {

            const a = $(e.target).attr('action');
            if (a && handler[a])
                handler[a](position);

            menu.destroy();

            return true;
        });
        closebutton.click(function () {
            menu.destroy();
        });

    });

}
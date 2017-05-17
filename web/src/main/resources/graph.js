"use strict";

/** https://github.com/kapouer/node-lfu-cache/blob/master/index.js */
class LFU extends Map {

    constructor(cap, halflife) {
        super();
        this.cap = cap;
        this.halflife = halflife || null;
        this.head = this.freq();
        this.lastDecay = Date.now();
    }

    get(key) {
        var el = super.get(key);
        if (!el) return;
        var cur = el.parent;
        var next = cur.next;
        if (!next || next.weight !== cur.weight + 1) {
            next = this.entry(cur.weight + 1, cur, next);
        }
        this.removeFromParent(el.parent, key);
        next.items.add(key);
        el.parent = next;
        var now = Date.now();
        el.atime = now;
        if (this.halflife && now - this.lastDecay >= this.halflife) this.decay(now);
        this.atime = now;
        return el.data;
    }

    decay(now) {
        // iterate over all entries and move the ones that have
        // this.atime - el.atime > this.halflife
        // to lower freq nodes
        // the idea is that if there is 10 hits / minute, and a minute gap,

        this.lastDecay = now;
        var diff = now - this.halflife;
        //var halflife = this.halflife;
        var weight, cur, prev;
        for (var [key, value] of this) {
            if (diff > value.atime) {
                // decay that one
                // 1) find freq
                cur = value.parent;
                weight = Math.round(cur.weight / 2);
                if (weight === 1) continue;
                prev = cur.prev;
                while (prev && prev.weight > weight) {
                    cur = prev;
                    prev = prev.prev;
                }
                if (!prev || !cur) {
                    throw new Error("Empty before and after halved weight - please report");
                }
                // 2) either prev has the right weight, or we must insert a freq with
                // the right weight
                if (prev.weight < weight) {
                    prev = this.entry(weight, prev, cur);
                }
                this.removeFromParent(value.parent, key);
                value.parent = prev;
                prev.items.add(key);
            }
        }
    }

    set(key, obj) {
        var el = super.get(key);
        if (el) {
            el.data = obj;
            return;
        }

        var now = Date.now();

        while (this.size+1 > this.cap) {
            if (this.halflife && now - this.lastDecay >= this.halflife) {
                this.decay(now);
            }

            try {
                this.evict();
            } catch (e) {
                console.error(e);
            }
        }


        var cur = this.head.next;
        if (!cur || cur.weight !== 1) {
            cur = this.entry(1, this.head, cur);
        }
        cur.items.add(key);


        super.set(key, { //TODO store this as a 3 element tuple
            data: obj,
            atime: now,
            parent: cur
        });


    }

    remove(key) {
        var el = super.get(key);
        if (!el)
            return;
        this.delete(key);
        this.removeFromParent(el.parent, key);
        return el.data;
    }


    removeFromParent(parent, key) {
        if (parent.items.delete(key)) {
            if (parent.items.size === 0) {
                parent.prev.next = parent.next;
                if (parent.next) parent.next.prev = parent;
            }
        }
    }

    evict() {
        const least = this.next();
        if (least) {
            const victim = this.remove(least);
            if (victim) {
                this.evicted(least, victim);
            } else {
                throw new Error("null victim");
            }
        } else {
            throw new Error("Cannot find an element to evict - please report issue");
        }
    }

    next() {
        if (this.head.next) {
            const items = this.head.next.items;
            if (items.size > 0)
                return items.keys().next().value;
        }
        return null;
    }

    evicted(key, value) {

    }

    freq() {
        return {
            weight: 0,
            items: new Set()
        }
    }

    item(obj, parent) {
        return {
            obj: obj,
            parent: parent
        };
    }

    entry(weight, prev, next) {
        var node = this.freq();
        node.weight = weight;
        node.prev = prev;
        node.next = next;
        prev.next = node;
        if (next) next.prev = node;
        return node;
    }
}

class Node {
    constructor() {
        //edge maps: (target, edge_value)
        //this.i = new LFU(8)
        this.i = new LFU(8);
        this.o = new LFU(8); //LFU(8);
    }

}



class LFUGraph extends LFU {

    constructor(maxNodes) {
        super(maxNodes);
    }

    node(nid, createIfMissing) {
        const x = this.get(nid);
        if (x || !createIfMissing)
            return x;

        const n = new Node();
        this.set(nid, n);
        this.nodeAdded(nid, n);
        return n;
    }

    nodeIfPresent(nodeID) {
        return this.entry(nodeID, false);
    }

    evicted(nid, n) {
        super.evicted(nid, n);

        this.nodeRemoved(nid, n);

        for (const tgt of n.o.keys()) {
            const tgtNode = this.get(tgt);
            if (tgtNode) //not sure why if ever this would return null
                tgtNode.i.remove(nid);
        }
        delete n.o;
        for (const src of n.i.keys()) {
            const srcNode = this.get(src);
            if (srcNode) //not sure why if ever this would return null
                srcNode.o.remove(nid);
        }
        delete n.i;
    }

    nodeAdded(nid, n) { }
    nodeRemoved(nid, n) { }
    edgeAdded(srcID, tgtID, e) { }
    edgeRemoved(srcID, tgtID, e) { }

    edge(src, tgt, edgeSupplier) {
        if (src == tgt)
            return null; //no self-loop

        const T = this.node(tgt, edgeSupplier);
        if (!T)
            return null;

        const S = this.node(src, edgeSupplier /* if edgeSupplier provided, create a node if missing */);
        if (!S)
            return null;

        const ST = S.o.get(tgt);
        if (ST) {
            return ST;
        } else if (edgeSupplier) {
            const newST = (typeof edgeSupplier === "function") ?  edgeSupplier() : edgeSupplier;
            S.o.set(tgt, newST);
            T.i.set(src, newST);
            return newST;
        } else {
            return null;
        }
    }

    edgeIfPresent(src, tgt) {
        return this.edge(src, tgt, null);
    }

    forEachEdge(edgeConsumer) {
        for (var [vertexID, vertex] of this) {
            const vv = vertex.data;
            for (var [edgeID, edge] of vv.o) {
                edgeConsumer(vertexID, vv, edgeID, edge.data);
            }
        }
    }

    /** computes a node-centric Map snapshot of the values */
    treeOut() {
        var x = {};
        this.forEachEdge((vid, v, eid, e) => {
            var ex = x[vid];
            if (!ex)
                x[vid] = ex = {};
            var ee = ex[eid];
            if (!ee)
                ex[eid] = ee = [];
            ee.push(eid);
        });
        return x;
    }

    edgeList() {
        var x =  [ ];
        this.forEachEdge((vid, v, eid, e) => {
            x.push( [vid, eid ] );
        });
        return x;
    }



}

(function(exports){

    exports.LFUGraph = LFUGraph;

}(typeof exports === 'undefined' ? this.share = {} : exports));


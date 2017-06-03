function Graph(settings) {

    const nodes = {};
    const edges = {};
    var settings = {
        epoch: 'Event Time',
        epochFormat: 'YYYY-M-D H:m:s',
        source: 'source',
        target: 'target'
    };

    this.edges = edges;
    this.nodes = nodes;
    this.settings = settings;

    let nodesCount = 0;
    let edgesCount = 0;


    function getNode(node) {

        return nodes[node];

    }

    function getEdge(source, target) {

        return edges[source + '<>' + target];

    }


    function createEdge(source, target, data) {

        let edge = getEdge(source, target) || getEdge(target, source);

        if (!edge) {

            edge = new Edge(source, target, edgesCount);
            edgesCount++;
            edges[source + '<>' + target] = edge;

        }

        if (data) {

            edge.data.push(data);

        }

    }


    this.addNode = function (nodeName, nodeData) {

        if (!nodeName) {

            return null;

        }

        let node = getNode(nodeName);

        if (!node) {

            node = new Node(nodesCount);
            nodesCount++;
            nodes[nodeName] = node;
        }

        if (nodeData) {

            node.data.push(nodeData);
        }

        return node;

    };


    this.addEdge = function (source, target, data) {

        // first see if they are different preventing data getting stored twice
        if (source === target) {

            this.addNode(source, data);


        } else {

            const fromNode = this.addNode(source, data);
            const toNode = this.addNode(target, data);

            if (fromNode && toNode) {
                // a valid edge appears!

                // record node instances for node epochs
                fromNode.edges.push(toNode.id);
                toNode.edges.push(fromNode.id);

                // record edge instances for edge epochs
                createEdge(source, target, data);

            }

        }

    };


    this.addCSVRow = function (data) {

        this.addEdge(data[settings.source], data[settings.target], data)

    };


    this.getNodesAndEdgesArray = function () {

        const edgesArray = [];

        $.each(this.nodes, function (key, value) {

            edgesArray[value.id] = /*_.uniq*/(value.edges);

        });

        return edgesArray;

    };


    this.getLookupTable = function () {

        const lookupTable = {};
        let texStartX;
        let texStartY;

        let i = 0;

        $.each(this.nodes, function (key, value) {


            texStartX = (i % nodesWidth) / nodesWidth;
            texStartY = (Math.floor(i / nodesWidth)) / nodesWidth;

            //console.log(i, texStartX);

            lookupTable[key] = {texPos: [texStartX, texStartY]};

            i++;

        });

        return lookupTable;

    };


    this.getEpochTextureArray = function (type) {

        let thing;

        if (type === 'nodes') thing = this.nodes;
        if (type === 'edges') thing = this.edges;

        const epochArray = [];


        $.each(thing, function (key, value) {

            const epochs = [];
            $.each(value.data, function (dkey, dvalue) {

                epochs.push(moment(dvalue[settings.epoch], [settings.epochFormat]).unix());

            });

            epochArray[value.id] = /*_.uniq*/(epochs);

        });

        return epochArray;

    };

}


function Node(id) {

    this.id = id;
    this.edges = [];
    this.data = [];

}


function Edge(source, target, id) {

    this.source = source;
    this.target = target;
    this.id = id;
    this.data = [];

}

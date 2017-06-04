/** the default impl that was included, not fully adapted yet */
function MutableGraph(settings) {

    const nodes = {};
    const edges = {};
    var settings = {
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

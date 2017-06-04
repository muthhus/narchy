// star
function generatorsStar(num) {
    const bn = 50;
    const bm = 50;
    for (var i = 0; i < bn; ++i) {
        for (var j = bn; j < bn + bm; ++j) {
            g.edge(i, j);
        }
    }
}


// balanced tree, don't go above 11
function generatorsBalancedTree(g, num) {
    const n = num, count = Math.pow(2, n);
    let level;

    if (n === 0) {
        g.node(1);
    }

    for (level = 1; level < count; ++level) {
        const root = level,
            left = root * 2,
            right = root * 2 + 1;

        g.edge(root, left);
        g.edge(root, right);
    }
}


function generatorCube(g, id, num) {
// n dimensinonal cube don't go past 14
    const n = num;
    const m = num;
    const z = num;
    if (n < 1 || m < 1 || z < 1) {
        throw new Error("Invalid number of nodes in grid3 graph");
    }
    for (var k = 0; k < z; ++k) {
        for (var i = 0; i < n; ++i) {
            for (var j = 0; j < m; ++j) {
                const level = k * n * m;
                const node = (i + j * n + level).toString();
                if (i > 0) {
                    g.edge(id + node, id + (i - 1 + j * n + level).toString());
                }
                if (j > 0) {
                    g.edge(id + node, id + (i + (j - 1) * n + level).toString());
                }
                if (k > 0) {
                    g.edge(id + node, id + (i + j * n + (k - 1) * n * m).toString());
                }
            }
        }
    }
}


// 2d lattice
//var n = 20;
//var m = 20;
//for (i = 0; i < n; ++i) {
//    for (j = 0; j < m; ++j) {
//        var node = i + j * n;
//        if (i > 0) { g.addLink(node, i - 1 + j * n); }
//        if (j > 0) { g.addLink(node, i + (j - 1) * n); }
//    }
//}

function generatorChain(num) {
    n = num + 1;
    g.node(0);
    for (i = 1; i < n; ++i) {
        g.edge(i - 1, i);
    }
}

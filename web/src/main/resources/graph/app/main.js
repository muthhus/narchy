var canvas, stats;
var camera, scene, renderer, controls;
var composer, vignette2Pass, multiPassBloomPass;
var backgroundColor = new THREE.Color(0x3c3c3c);

var nodeMesh,
    nodeGeometry,
    nodeUniforms, labelUniforms;

var simulate = false;
var graphStructure;
var mouse = new THREE.Vector2();
var mouseUp = true;
var mouseDown = false;
var mouseDblClick = false;
var temperature = 100;
var lastPickedNode = {};
var last = performance.now();
var simulator, interface;
var pickingTexture, pickingScene;
var k = 0;
var shaders;
var slider;

var epochMin, epochMax;
var epochOffset;


var nodesAndEdges, nodesAndEpochs, nodesWidth, edgesWidth, epochsWidth, nodesCount, edgesCount, edgesAndEpochs,
    edgesLookupTable;

var nodesRendered = {};

var g;


$(document).ready(() => {

    shaders = new ShaderLoader('./shaders');
    shaders.load('vs-edge', 'edge', 'vertex');
    shaders.load('fs-edge', 'edge', 'fragment');
    shaders.load('vs-node', 'node', 'vertex');
    shaders.load('fs-node', 'node', 'fragment');
    shaders.load('vs-passthru', 'passthru', 'vertex');
    shaders.load('fs-passthru', 'passthru', 'fragment');
    shaders.load('sim-velocity', 'velocity', 'simulation');
    shaders.load('sim-position', 'position', 'simulation');
    shaders.load('sim-nodeAttrib', 'nodeAttrib', 'simulation');
    shaders.load('vs-text', 'text', 'vertex');
    shaders.load('fs-text', 'text', 'fragment');
    shaders.shaderSetLoaded = function () {
        start().animate();
    };
});

function start() {
    g = new Graph();


    // generatorChain(100);
    generatorsBalancedTree(3);
    setTimeout(() => {

        console.log('regen');
        generatorCube('a', 2);
        updateGraph();


    }, 4000);
    // generatorCube(5);
    // generatorsStar(50);


    canvas = document.getElementById('c');

    camera = new THREE.PerspectiveCamera(50, window.innerWidth / window.innerHeight, 0.0001, 100000);
    camera.position.x = 0;
    camera.position.y = 0;
    camera.position.z = 1500;

    controls = new THREE.OrbitControls(camera, canvas);
    controls.damping = 0.2;
    controls.enableDamping = true;

    scene = new THREE.Scene();

    pickingScene = new THREE.Scene();
    pickingTexture = new THREE.WebGLRenderTarget(window.innerWidth, window.innerHeight);
    pickingTexture.texture.minFilter = THREE.LinearFilter;
    pickingTexture.texture.generateMipmaps = false;


    renderer = new THREE.WebGLRenderer({
        antialias: true,
        alpha: true,
        canvas: canvas
    });

    // WAGNER.vertexShadersPath = './shaders';
    // WAGNER.fragmentShadersPath = './shaders';
    // composer = new WAGNER.Composer(renderer, {useRGBA: false});

    // vignette2Pass = new WAGNER.Vignette2Pass();
    // vignette2Pass.params.boost = 1.0;
    // vignette2Pass.params.reduction = 0.5;


    // vignette2Pass = new WAGNER.VignettePass();
    // vignette2Pass.params.amount = 0.45;
    // vignette2Pass.params.falloff = 0.35;

    // multiPassBloomPass = new WAGNER.MultiPassBloomPass();
    // multiPassBloomPass.params.blurAmount = 0.2;


    // stats = new Stats();
    // stats.domElement.style.position = 'absolute';
    // stats.domElement.style.top = '50px';
    // container.appendChild(stats.domElement);

    var gridHelper1 = new THREE.GridHelper(2000, 500);
    scene.add(gridHelper1);

    var gridHelper2 = new THREE.GridHelper(2000, 500);
    gridHelper2.rotation.z = Math.PI / 2;
    scene.add(gridHelper2);

    // gridHelper3 = new THREE.GridHelper(2000, 500);
    // gridHelper3.rotation.x = Math.PI / 2;
    // scene.add(gridHelper3);


    gpupicking = new GPUPick();

    slider = new Slider();
    slider.init();


    onWindowResize();
    window.addEventListener('resize', onWindowResize, false);
    document.addEventListener('mousemove', onMouseMove, false);
    document.addEventListener('mouseup', onMouseUp, false);
    document.addEventListener('mousedown', onMouseDown, false);
    document.addEventListener('dblclick', onDoubleClick, false);


    //epochOffset = min;


    interface = new GUIInterface(simulator);
    interface.init();


    //
    //const gui = new dat.GUI();
    // var effects = gui.addFolder('effects');
    //
    // var colorConfig = function () {
    // this.color = "#2a2a2a";
    // };
    // var conf = new colorConfig();
    //
    // //var colorPicker = effects.addColor(conf, 'color');
    // //colorPicker.onChange(function (colorValue) {
    // // console.log(colorValue);
    // // backgroundColor = colorValue;
    // //});
    //
    //
    //
    // effects.add(vignette2Pass.params, 'amount').min(0).max(1);
    // effects.add(vignette2Pass.params, 'falloff').min(0).max(1);


// var gui = new dat.GUI();
// gui.close();


    // var effectController = {
    //     k: 10
    // };
    // var valuesChanger = function () {
    //
    //     simulator.velocityUniforms.k.value = 100;
    //     // if (temperature < 200) temperature = 200;
    //
    // };
    //
    // valuesChanger();
    //gui.add(effectController, "k", 1, 10000).onChange(valuesChanger);


    function updateGraph() {

        if (graphStructure) {
            scene.remove(graphStructure);
        }

        graphStructure = new THREE.Object3D();
        scene.add(graphStructure);


        nodesAndEdges = g.getNodesAndEdgesArray();


        nodesAndEpochs = g.getEpochTextureArray('nodes');
        edgesAndEpochs = g.getEpochTextureArray('edges');


        nodesCount = nodesAndEdges.length;
        edgesCount = countDataArrayItems(nodesAndEdges);

        nodesWidth = indexTextureSize(nodesAndEdges.length);
        edgesWidth = dataTextureSize(countDataArrayItems(nodesAndEdges));
        epochsWidth = dataTextureSize(countDataArrayItems(nodesAndEpochs));

        // console.log('nodesAndEdges', nodesAndEdges);
        // console.log('nodesAndEpochs', nodesAndEpochs);
        // console.log('nodesCount', nodesCount);
        // console.log('edgesCount', edgesCount);
        // console.log('nodesWidth', nodesWidth);
        // console.log('edgesWidth', edgesWidth);
        // console.log('epochsWidth', epochsWidth);

        edgesLookupTable = g.getLookupTable(); // needs to be after nodesWidth

        temperature = nodesAndEdges.length / 2;


        // get the min and max values for epoch.


        var bigArray = [];
        for (var i = 0; i < nodesAndEpochs.length; i++) {

            for (var j = 0; j < nodesAndEpochs[i].length; j++) {

                bigArray.push(nodesAndEpochs[i][j]);
            }

        }

        // console.log(bigArray);
        min = _.min(bigArray);
        max = _.max(bigArray);

        // console.log('min epoch:', min, 'max epoch:', max);

        // slider.setLimits(min, max);


        /*
         *   requires globals nodesAndEdges, nodesWidth, edgesWidth, nodesCount, edgesCount
         *
         * */
        // NODES
        nodeRegular = THREE.ImageUtils.loadTexture('textures/new_circle.png', {}, function () {
            renderer.render(scene, camera);
        });
        nodeThreat = THREE.ImageUtils.loadTexture('textures/crosshair.png', {}, function () {
            renderer.render(scene, camera);
        });
        nodeGeometry = new THREE.BufferGeometry();
        pickingNodeGeometry = new THREE.BufferGeometry();
// visible geometry attributes
        var nodePositions = new THREE.BufferAttribute(new Float32Array(nodesCount * 3), 3);
        var nodeReferences = new THREE.BufferAttribute(new Float32Array(nodesCount * 2), 2);
        var nodeColors = new THREE.BufferAttribute(new Float32Array(nodesCount * 3), 3);
        var nodePick = new THREE.BufferAttribute(new Float32Array(nodesCount), 1);
        var hover = new THREE.BufferAttribute(new Float32Array(nodesCount), 1);
        var threat = new THREE.BufferAttribute(new Float32Array(nodesCount), 1);
        nodeGeometry.addAttribute('position', nodePositions);
        nodeGeometry.addAttribute('texPos', nodeReferences);
        nodeGeometry.addAttribute('customColor', nodeColors);
        nodeGeometry.addAttribute('pickingNode', nodePick);
        nodeGeometry.addAttribute('threat', threat);
// picking geometry attributes (different colors)
        var pickingColors = new THREE.BufferAttribute(new Float32Array(nodesCount * 3), 3);
        var pickingPick = new THREE.BufferAttribute(new Float32Array(nodesCount), 1);
        pickingNodeGeometry.addAttribute('position', nodePositions);
        pickingNodeGeometry.addAttribute('texPos', nodeReferences);
        pickingNodeGeometry.addAttribute('customColor', pickingColors);
        pickingNodeGeometry.addAttribute('pickingNode', pickingPick);
        pickingNodeGeometry.addAttribute('threat', threat);
        var color = new THREE.Color(0x999999);
        var chromaColor;
        //console.log(nodesCount);
        var scale = ['#a6cee3', '#1f78b4', '#b2df8a', '#33a02c', '#fb9a99', '#fdbf6f', '#ff7f00', '#cab2d6', '#6a3d9a', '#ffff99', '#b15928'];
        var chromaScale = chroma.scale(scale).domain([0, nodesCount]);
        var threatValue = 0;
        var v = 0;
        nodesRendered = [];
        console.log(g.nodes);
        $.each(g.nodes, function (key, value) {
            nodesRendered.push(key);
            threatValue = 0;
            $.each(value.data, function (dkey, dvalue) {
                if (key === dvalue['source']) {
                    if (dvalue['source_hit'] === true) threatValue = 1;
                }
                if (key === dvalue['target']) {
                    if (dvalue['target_hit'] === true) threatValue = 1;
                }
            });
            nodePositions.array[v * 3] = 0;
            nodePositions.array[v * 3 + 1] = 0;
            nodePositions.array[v * 3 + 2] = 0;
            if (threatValue === 1) {
                chromaColor = [1.0, 0.0, 0.0];  // red
            } else {
                chromaColor = chromaScale(v).gl();  // returns a RGB array normalized from 0.0 - 1.0
            }
            nodeColors.array[v * 3] = chromaColor[0];
            nodeColors.array[v * 3 + 1] = chromaColor[1];
            nodeColors.array[v * 3 + 2] = chromaColor[2];
            edgesLookupTable[key]['color'] = chromaColor;  // used later for labels and edges
            color.setHex(v + 1);
            pickingColors.array[v * 3] = color.r;
            pickingColors.array[v * 3 + 1] = color.g;
            pickingColors.array[v * 3 + 2] = color.b;
            nodePick.array[v] = 1.0;
            pickingPick.array[v] = 0.0;
            //TODO: this is now in a lookup table.
            nodeReferences.array[v * 2] = (v % nodesWidth) / nodesWidth;
            nodeReferences.array[(v * 2) + 1] = (Math.floor(v / nodesWidth)) / nodesWidth;
            // threats
            threat.array[v] = threatValue;
            v++;
        });
        //console.log(nodeReferences.array);
        nodeUniforms = {
            positionTexture: {type: "t", value: null},
            nodeAttribTexture: {type: "t", value: null},
            sprite: {type: "t", value: nodeRegular},
            threatSprite: {type: "t", value: nodeThreat},
            currentTime: {type: "f", value: null}
        };
        // ShaderMaterial
        nodeMaterial = new THREE.ShaderMaterial({
            uniforms: nodeUniforms,
            defines: {
                EPOCHSWIDTH: epochsWidth.toFixed(2)
            },
            vertexShader: shaders.vs.node,
            fragmentShader: shaders.fs.node,
            blending: THREE.AdditiveBlending,
            //blending: THREE.AdditiveBlending,
            depthTest: false,
            transparent: true
        });
        pickingMaterial = new THREE.ShaderMaterial({
            uniforms: nodeUniforms,
            defines: {
                EPOCHSWIDTH: epochsWidth.toFixed(2),
                NODESWIDTH: nodesWidth.toFixed(2)
            },
            vertexShader: shaders.vs.node,
            fragmentShader: shaders.fs.node,
            depthTest: false,
            transparent: false
        });
        nodeMesh = new THREE.Points(nodeGeometry, nodeMaterial);
        pickingMesh = new THREE.Points(pickingNodeGeometry, pickingMaterial);
        pickingScene.add(pickingMesh);
        graphStructure.add(nodeMesh);
        //EDGES
        edgeGeometry = new THREE.BufferGeometry();
        var edgePositions = new THREE.BufferAttribute(new Float32Array(edgesCount * 2 * 3), 3);
        var edgeReferences = new THREE.BufferAttribute(new Float32Array(edgesCount * 2 * 2), 2);
        var edgeColors = new THREE.BufferAttribute(new Float32Array(edgesCount * 2 * 3), 3);
        edgeGeometry.addAttribute('position', edgePositions);
        edgeGeometry.addAttribute('texPos', edgeReferences);
        edgeGeometry.addAttribute('customColor', edgeColors);
        //which vertex we're on
        v = 0;
        var line;
        $.each(g.edges, function (key) {
            line = key.split('<>');
            var startNode = edgesLookupTable[line[0]];
            var endNode = edgesLookupTable[line[1]];
            //start of line
            edgeReferences.array[v * 2] = startNode.texPos[0];
            edgeReferences.array[v * 2 + 1] = startNode.texPos[1];
            // positions will be set by mapped texture
            edgePositions.array[v * 3] = 0;
            edgePositions.array[v * 3 + 1] = 0;
            edgePositions.array[v * 3 + 2] = 0;
            edgeColors.array[v * 3] = startNode.color[0];
            edgeColors.array[v * 3 + 1] = startNode.color[1];
            edgeColors.array[v * 3 + 2] = startNode.color[2];
            v++;
            //end of line
            edgeReferences.array[v * 2] = endNode.texPos[0];
            edgeReferences.array[v * 2 + 1] = endNode.texPos[1];
            // positions will be set by mapped texture
            edgePositions.array[v * 3] = 0;
            edgePositions.array[v * 3 + 1] = 0;
            edgePositions.array[v * 3 + 2] = 0;
            edgeColors.array[v * 3] = endNode.color[0];
            edgeColors.array[v * 3 + 1] = endNode.color[1];
            edgeColors.array[v * 3 + 2] = endNode.color[2];
            v++;
        });
        //now we get all the endpoints and put in the data
        edgeUniforms = {
            positionTexture: {type: "t", value: null},
            nodeAttribTexture: {type: "t", value: null}
        };
        edgeMaterial = new THREE.ShaderMaterial({
            uniforms: edgeUniforms,
            vertexShader: shaders.vs.edge,
            fragmentShader: shaders.fs.edge,
            //blending: THREE.AdditiveBlending,
            depthTest: false,
            transparent: true,
            linewidth: 5
        });
        cloudLines = new THREE.LineSegments(edgeGeometry, edgeMaterial);
        graphStructure.add(cloudLines);

        var font = UbuntuMono('lib/UbuntuMono.png');
        var texture = font.texture;
        var letterWidth = 20;
        var letterSpacing = 15;

        function getTextCoordinates(letter) {
            var index;
            var charCode = letter.charCodeAt(0);
            //console.log('  charCode is:', charCode);
            var charString = "" + charCode;
            // Some weird CHAR CODES
            if (charCode === 8216) {
                charCode = 39;
            }
            if (charCode === 8217) {
                charCode = 39;
            }
            if (charCode === 8212) {
                charCode = 45;
            }
            for (var z in font) {
                if (z === charCode) {
                    index = font[z];
                }
            }
            if (!index) {

                //console.log('  NO LETTER');
                index = [0, 0];
            }
            var left = index[0] / 1024;
            var top = index[1] / 1024;
            var width = index[2] / 1024;
            var height = index[3] / 1024;
            var xoffset = index[4] / 1024;
            var yoffset = index[5] / 1024;
            var array = [left, top, width, height, xoffset, yoffset];
        }

        // need to get particle count.
        var particleCount = 0;
        $.each(g.nodes, function (key) {
            particleCount += key.length;
        });
        //console.log('character count:', particleCount);
        labelGeometry = new THREE.BufferGeometry();
        var positions = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 3), 3);
        var labelPositions = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 3), 3);
        var labelColors = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 3), 3);
        var uvs = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 2), 2);
        var ids = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 1), 1);
        var textCoords = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 4), 4);
        var labelReferences = new THREE.BufferAttribute(new Float32Array(particleCount * 6 * 2), 2);
        labelGeometry.addAttribute('position', positions);
        labelGeometry.addAttribute('labelPositions', labelPositions);
        labelGeometry.addAttribute('uv', uvs);
        labelGeometry.addAttribute('id', ids);
        labelGeometry.addAttribute('textCoord', textCoords);
        labelGeometry.addAttribute('texPos', labelReferences);
        labelGeometry.addAttribute('customColor', labelColors);
        var counter = 0;
        var nodeLookup;
        $.each(g.nodes, function (key) {
            nodeLookup = edgesLookupTable[key];
            //console.log('working on word:', key);
            for (var i = 0; i < key.length; i++) {
                //console.log(' counter:', counter);
                var index = counter * 3 * 2;
                //console.log('  character:', key[i]);
                var tc = getTextCoordinates(key[i]);
                //console.log('  tc:', tc);
                // Left is offset
                var l = tc[4];
                // Right is offset + width
                var r = tc[4] + tc[2];
                // bottom is y offset
                var b = tc[5] - tc[3];
                // top is y offset + height
                var t = tc[5];
                ids.array[index + 0] = i;
                ids.array[index + 1] = i;
                ids.array[index + 2] = i;
                ids.array[index + 3] = i;
                ids.array[index + 4] = i;
                ids.array[index + 5] = i;
                positions.array[index * 3 + 0] = 0;
                positions.array[index * 3 + 1] = 0;
                positions.array[index * 3 + 2] = 0;
                positions.array[index * 3 + 3] = 0;
                positions.array[index * 3 + 4] = 0;
                positions.array[index * 3 + 5] = 0;
                positions.array[index * 3 + 6] = 0;
                positions.array[index * 3 + 7] = 0;
                positions.array[index * 3 + 8] = 0;
                positions.array[index * 3 + 9] = 0;
                positions.array[index * 3 + 10] = 0;
                positions.array[index * 3 + 11] = 0;
                positions.array[index * 3 + 12] = 0;
                positions.array[index * 3 + 13] = 0;
                positions.array[index * 3 + 14] = 0;
                positions.array[index * 3 + 15] = 0;
                positions.array[index * 3 + 16] = 0;
                positions.array[index * 3 + 17] = 0;
                labelPositions.array[index * 3 + 0] = (i * letterSpacing) + l * letterWidth * 10;
                labelPositions.array[index * 3 + 1] = t * letterWidth * 10;
                labelPositions.array[index * 3 + 2] = 0 * letterWidth * 10;
                labelPositions.array[index * 3 + 3] = (i * letterSpacing) + l * letterWidth * 10;
                labelPositions.array[index * 3 + 4] = b * letterWidth * 10;
                labelPositions.array[index * 3 + 5] = 0 * letterWidth * 10;
                labelPositions.array[index * 3 + 6] = (i * letterSpacing) + r * letterWidth * 10;
                labelPositions.array[index * 3 + 7] = t * letterWidth * 10;
                labelPositions.array[index * 3 + 8] = 0 * letterWidth * 10;
                labelPositions.array[index * 3 + 9] = (i * letterSpacing) + r * letterWidth * 10;
                labelPositions.array[index * 3 + 10] = b * letterWidth * 10;
                labelPositions.array[index * 3 + 11] = 0 * letterWidth * 10;
                labelPositions.array[index * 3 + 12] = (i * letterSpacing) + r * letterWidth * 10;
                labelPositions.array[index * 3 + 13] = t * letterWidth * 10;
                labelPositions.array[index * 3 + 14] = 0 * letterWidth * 10;
                labelPositions.array[index * 3 + 15] = (i * letterSpacing) + l * letterWidth * 10;
                labelPositions.array[index * 3 + 16] = b * letterWidth * 10;
                labelPositions.array[index * 3 + 17] = 0 * letterWidth * 10;
                uvs.array[index * 2 + 0] = 0;
                uvs.array[index * 2 + 1] = 1;
                uvs.array[index * 2 + 2] = 0;
                uvs.array[index * 2 + 3] = 0;
                uvs.array[index * 2 + 4] = 1;
                uvs.array[index * 2 + 5] = 1;
                uvs.array[index * 2 + 6] = 1;
                uvs.array[index * 2 + 7] = 0;
                uvs.array[index * 2 + 8] = 1;
                uvs.array[index * 2 + 9] = 1;
                uvs.array[index * 2 + 10] = 0;
                uvs.array[index * 2 + 11] = 0;
                textCoords.array[index * 4 + 0] = tc[0];
                textCoords.array[index * 4 + 1] = tc[1];
                textCoords.array[index * 4 + 2] = tc[2];
                textCoords.array[index * 4 + 3] = tc[3];
                textCoords.array[index * 4 + 4] = tc[0];
                textCoords.array[index * 4 + 5] = tc[1];
                textCoords.array[index * 4 + 6] = tc[2];
                textCoords.array[index * 4 + 7] = tc[3];
                textCoords.array[index * 4 + 8] = tc[0];
                textCoords.array[index * 4 + 9] = tc[1];
                textCoords.array[index * 4 + 10] = tc[2];
                textCoords.array[index * 4 + 11] = tc[3];
                textCoords.array[index * 4 + 12] = tc[0];
                textCoords.array[index * 4 + 13] = tc[1];
                textCoords.array[index * 4 + 14] = tc[2];
                textCoords.array[index * 4 + 15] = tc[3];
                textCoords.array[index * 4 + 16] = tc[0];
                textCoords.array[index * 4 + 17] = tc[1];
                textCoords.array[index * 4 + 18] = tc[2];
                textCoords.array[index * 4 + 19] = tc[3];
                textCoords.array[index * 4 + 20] = tc[0];
                textCoords.array[index * 4 + 21] = tc[1];
                textCoords.array[index * 4 + 22] = tc[2];
                textCoords.array[index * 4 + 23] = tc[3];
                labelReferences.array[index * 2 + 0] = nodeLookup.texPos[0];
                labelReferences.array[index * 2 + 1] = nodeLookup.texPos[1];
                labelReferences.array[index * 2 + 2] = nodeLookup.texPos[0];
                labelReferences.array[index * 2 + 3] = nodeLookup.texPos[1];
                labelReferences.array[index * 2 + 4] = nodeLookup.texPos[0];
                labelReferences.array[index * 2 + 5] = nodeLookup.texPos[1];
                labelReferences.array[index * 2 + 6] = nodeLookup.texPos[0];
                labelReferences.array[index * 2 + 7] = nodeLookup.texPos[1];
                labelReferences.array[index * 2 + 8] = nodeLookup.texPos[0];
                labelReferences.array[index * 2 + 9] = nodeLookup.texPos[1];
                labelReferences.array[index * 2 + 10] = nodeLookup.texPos[0];
                labelReferences.array[index * 2 + 11] = nodeLookup.texPos[1];
                labelColors.array[index * 3 + 0] = nodeLookup.color[0];
                labelColors.array[index * 3 + 1] = nodeLookup.color[1];
                labelColors.array[index * 3 + 2] = nodeLookup.color[2];
                labelColors.array[index * 3 + 3] = nodeLookup.color[0];
                labelColors.array[index * 3 + 4] = nodeLookup.color[1];
                labelColors.array[index * 3 + 5] = nodeLookup.color[2];
                labelColors.array[index * 3 + 6] = nodeLookup.color[0];
                labelColors.array[index * 3 + 7] = nodeLookup.color[1];
                labelColors.array[index * 3 + 8] = nodeLookup.color[2];
                labelColors.array[index * 3 + 9] = nodeLookup.color[0];
                labelColors.array[index * 3 + 10] = nodeLookup.color[1];
                labelColors.array[index * 3 + 11] = nodeLookup.color[2];
                labelColors.array[index * 3 + 12] = nodeLookup.color[0];
                labelColors.array[index * 3 + 13] = nodeLookup.color[1];
                labelColors.array[index * 3 + 14] = nodeLookup.color[2];
                labelColors.array[index * 3 + 15] = nodeLookup.color[0];
                labelColors.array[index * 3 + 16] = nodeLookup.color[1];
                labelColors.array[index * 3 + 17] = nodeLookup.color[2];
                counter++;
            }
        });
        labelUniforms = {
            t_text: {type: "t", value: texture},
            positionTexture: {type: "t", value: null},
            nodeAttribTexture: {type: "t", value: null}
        };
        labelMaterial = new THREE.ShaderMaterial({
            uniforms: labelUniforms,
            vertexShader: shaders.vs.text,
            fragmentShader: shaders.fs.text,
            //blending: THREE.AdditiveBlending,
            depthTest: false,
            transparent: true
        });

        labelMesh = new THREE.Mesh(labelGeometry, labelMaterial);
        graphStructure.add(labelMesh);

        if (!simulator)
            simulator = new Simulator(renderer);

        simulator.update(); //reinit
    }


    function onWindowResize() {

        // console.log(canvas);

        var width = canvas.clientWidth * window.devicePixelRatio;
        var height = canvas.clientHeight * window.devicePixelRatio;

        camera.aspect = width / height;
        camera.updateProjectionMatrix();

        renderer.setSize(width, height, false);  // YOU MUST PASS FALSE HERE!
        gpupicking.pickingTexture.setSize(width, height);
        // pickingTexture.setSize(width, height);
        // composer.setSize(width, height);

    }


    function onMouseDown(event) {

        if (event.target.id === 'c') {

            mouseDown = true;
            mouseUp = false;

        }

    }


    function onMouseUp(event) {

        if (event.target.id === 'c') {

            mouseDown = false;
            mouseUp = true;

        }

    }

    function onDoubleClick(event) {

        if (event.target.id === 'c') {

            mouseDown = false;
            mouseUp = true;
            mouseDblClick = true;

        }


    }


    function onMouseMove(e) {

        mouse.x = e.clientX;
        mouse.y = e.clientY;

        // console.log(mouseX, mouseY);

    }


    document.onkeypress = function (e) {

        // console.log(e.charCode);

        if (e.charCode === 115) {
            simulate = !simulate;
        }
        if (e.charCode === 61) {
            if (slider) {
                slider.increaseStep();
            }
        }
        if (e.charCode === 45) {
            if (slider) {
                slider.decreaseStep();
            }
        }

        if (e.charCode === 93) {
            if (slider) {
                slider.increaseHandles();
            }
        }

        if (e.charCode === 91) {
            if (slider) {
                slider.decreaseHandles();
            }
        }

    };


    g.animate = function () {

        // stats.update();
        controls.update();
        slider.update();

        render();
        requestAnimationFrame(g.animate);
    };

    function highlightNode(idx, color) {
        nodeGeometry.attributes.customColor.array[idx * 3 + 0] = color[0];
        nodeGeometry.attributes.customColor.array[idx * 3 + 1] = color[1];
        nodeGeometry.attributes.customColor.array[idx * 3 + 2] = color[2];


    }

    function saveNodeColor(idx) {

        var r = nodeGeometry.attributes.customColor.array[idx * 3 + 0];
        var g = nodeGeometry.attributes.customColor.array[idx * 3 + 1];
        var b = nodeGeometry.attributes.customColor.array[idx * 3 + 2];

        return [r, g, b];

    }

    function pick() {

        // render the picking scene off-screen

        renderer.render(scene, camera, pickingTexture);

        // create buffer for reading single pixel
        var pixelBuffer = new Uint8Array(4);


        // read the pixel under the mouse from the texture
        renderer.readRenderTargetPixels(pickingTexture, mouse.x, pickingTexture.height - mouse.y, 1, 1, pixelBuffer);


        function restoreColor(idx, color) {

            nodeGeometry.attributes.customColor.array[idx * 3 + 0] = color[0];
            nodeGeometry.attributes.customColor.array[idx * 3 + 1] = color[1];
            nodeGeometry.attributes.customColor.array[idx * 3 + 2] = color[2];

        }

        var id = ( pixelBuffer[0] << 16 ) | ( pixelBuffer[1] << 8 ) | ( pixelBuffer[2] ) - 1;
        var data = nodesAndEdges[id];


        if (id !== lastPickedNode.id) {
            // console.log('new node selected:', id, 'last one was:',
            // lastPickedNode.id);

            // reset the old stuff to original values

            var lastData = nodesAndEdges[lastPickedNode.id];

            if (lastData) {
                // console.log('restoring', lastPickedNode.id);
                restoreColor(lastPickedNode.id, lastPickedNode.parent);
                for (var i = 0; i < lastData.length; i++) {
                    restoreColor(lastData[i], lastPickedNode.children[i]);
                }
                nodeGeometry.attributes.customColor.needsUpdate = true;

            }


            // clear and set the new stuff


            if (data) {

                lastPickedNode = {};
                lastPickedNode.children = [];  // id, r, g, b
                lastPickedNode.parent = [];
                lastPickedNode.id = id;

                console.log(nodesRendered.get(id));

                lastPickedNode.parent = saveNodeColor(id);
                highlightNode(id, [0, 255, 0]);
                for (var i = 0; i < data.length; i++) {
                    lastPickedNode.children.push(saveNodeColor(data[i]));
                    highlightNode(data[i], [0, 0, 255])
                }

                nodeGeometry.attributes.customColor.needsUpdate = true;

            } else {

                lastPickedNode.id = id;
            }

        }

    }


    function countDataArrayItems(dataArray) {

        var counter = 0;

        for (var i = 0; i < dataArray.length; i++) {

            counter += dataArray[i].length;

        }

        return counter;

    }


    function render() {


        var now = performance.now();
        var delta = (now - last) / 1000.0;

        if (delta > 1) delta = 1; // safety cap on large deltas
        last = now;


        if (simulator) {

            // temperature *= 0.99;
            simulator.simulate(delta, temperature, epochMin, epochMax);

            nodeUniforms.positionTexture.value = simulator.positionUniforms.positions.value;
            labelUniforms.positionTexture.value = simulator.positionUniforms.positions.value;

            nodeUniforms.nodeAttribTexture.value = simulator.nodeAttribUniforms.nodeAttrib.value;
            edgeUniforms.nodeAttribTexture.value = simulator.nodeAttribUniforms.nodeAttrib.value;
            labelUniforms.nodeAttribTexture.value = simulator.nodeAttribUniforms.nodeAttrib.value;
            // graphStructure.rotation.y += 0.0025;
        }

        if (nodeGeometry) {

            gpupicking.update();

            if (nodeUniforms) {
                nodeUniforms.currentTime.value = now;
                nodeUniforms.currentTime.value = Math.sin(now * 0.0005) * 1;
            }
        }

        // renderer.setClearColor(backgroundColor);
        // renderer.setClearColor( 0x000, 0.0);
        renderer.render(scene, camera);
        renderer.autoClearColor = true;


        // composer.reset();
        // composer.render(scene, camera);
        // composer.pass(vignette2Pass);
        // composer.pass(multiPassBloomPass);
        // composer.pass(fxaaPass);

        // composer.toScreen();

    }

    updateGraph();
    return g;
}
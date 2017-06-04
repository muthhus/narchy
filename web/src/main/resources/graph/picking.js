function GPUPick(width, height, renderer, simulator, pickingScene, camera, mouse, mouseDown, mouseUp, mouseDblClick) {

    let id;

    //var lastData = {};
    //var lastClickedNode = -1;
    var lastHovereddNode = -1;
    const nodeClicked = {down: null, up: null};

    //create buffer for reading single pixel
    var pixelBuffer = new Uint8Array(4);

    let selectedNode = null;


    var pickingTexture = this.pickingTexture = new THREE.WebGLRenderTarget(width, height);
    pickingTexture.texture.minFilter = THREE.LinearFilter;
    pickingTexture.texture.generateMipmaps = false;

    function clicked() {

        if (nodeClicked.down === nodeClicked.up) {

            if (nodeClicked.down >= 0) {

                selectedNode = nodeClicked.down;
                console.log('you successfully selected', selectedNode, nodesRendered[selectedNode]);
                simulator.nodeAttribUniforms.selectedNode.value = selectedNode;
                simulator.nodeAttribUniforms.hoverMode.value = 0;
            }

        }

        nodeClicked.down = null;
        nodeClicked.up = null;

    }

    function hoverOver(id) {

        //console.log('hovered over id', id);
        simulator.nodeAttribUniforms.selectedNode.value = id;

    }


    return {
        'pickingTexture': pickingTexture,

        'update': ()=> {


            //read the pixel under the mouse from the texture
            renderer.readRenderTargetPixels(pickingTexture, mouse.x * window.devicePixelRatio, pickingTexture.height - mouse.y * window.devicePixelRatio, 1, 1, pixelBuffer);

            //interpret the pixel as an ID

            id = ( pixelBuffer[0] << 16 ) | ( pixelBuffer[1] << 8 ) | ( pixelBuffer[2] - 1);

            // var data;
            // if (id > 0 && id < nodesAndEdges.length)
            //     data = nodesAndEdges[id];
            // else
            //     data = undefined;
            //
            // if (nodeClicked.down === null) {
            //
            //     if (mouseDown) {
            //
            //         nodeClicked.down = id;
            //         //console.log('down', id, data);
            //
            //     }
            //
            // }
            //
            // if (nodeClicked.down !== null && nodeClicked.up === null) {
            //
            //     if (mouseUp) {
            //
            //         nodeClicked.up = id;
            //         //console.log('up', id, data);
            //
            //         clicked();
            //
            //     }
            //
            // }
            //
            // if (selectedNode === null && nodeClicked.down === null) {
            //
            //     // we're just hovering around
            //
            //     if (lastHovereddNode !== id) {
            //
            //         lastHovereddNode = id;
            //         hoverOver(id);
            //
            //     }
            //
            // }
            //
            // if (mouseDblClick) {
            //
            //     //console.log('selection cleared!');
            //     simulator.nodeAttribUniforms.hoverMode.value = 1;
            //
            //     selectedNode = null;
            //     mouseDblClick = false;
            //
            // }


        }

    };


}

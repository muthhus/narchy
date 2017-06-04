function Simulator(renderer, nodesAndEdges, nodesAndEpochs, nodesWidth, edgesWidth, epochsWidth, shaders) {

	/*
	 * requires globals nodesAndEdges, nodesWidth, edgesWidth, nodesCount,
	 * edgesCount
	 * 
	 */

	const camera = new THREE.Camera();
	camera.position.z = 1;

	const scene = new THREE.Scene();

	const passThruUniforms = {
        texture: {
            type: "t",
            value: null
        }
    };

	const passThruShader = new THREE.ShaderMaterial({
        uniforms: passThruUniforms,
        defines: {
            NODESWIDTH: nodesWidth.toFixed(2)
        },
        vertexShader: shaders.vs.passthru,
        fragmentShader: shaders.fs.passthru
    });

	const mesh = new THREE.Mesh(new THREE.PlaneBufferGeometry(2, 2),
        passThruShader);
	scene.add(mesh);

	/*
	 * all shaders are initialized with null textures. This is so that we can
	 * create the simulator without having the data required for the init()
	 * function
	 */

	const velocityShader = new THREE.ShaderMaterial({

        uniforms: {
            delta: {
                type: "f",
                value: 0.0
            },
            k: {
                type: "f",
                value: 100.0
            },
            temperature: {
                type: "f",
                value: 0.0
            },
            positions: {
                type: "t",
                value: null
            },
            layoutPositions: {
                type: "t",
                value: null
            },
            velocities: {
                type: "t",
                value: null
            },
            edgeIndices: {
                type: "t",
                value: null
            },
            edgeData: {
                type: "t",
                value: null
            }
        },
        defines: {
            NODESWIDTH: nodesWidth.toFixed(2),
            EDGESWIDTH: edgesWidth.toFixed(2)
        },
        vertexShader: shaders.vs.passthru,
        fragmentShader: shaders.ss.velocity,
        blending: 0

    });

	const positionShader = new THREE.ShaderMaterial({

        uniforms: {
            delta: {
                type: "f",
                value: 0.0
            },
            temperature: {
                type: "f",
                value: 0.0
            },
            positions: {
                type: "t",
                value: null
            },
            velocities: {
                type: "t",
                value: null
            }
        },
        defines: {
            NODESWIDTH: nodesWidth.toFixed(2)
        },
        vertexShader: shaders.vs.passthru,
        fragmentShader: shaders.ss.position,
        blending: 0
    });

	const nodeAttribShader = new THREE.ShaderMaterial({

        uniforms: {
            nodeIDMappings: {
                type: "t",
                value: null
            },
            epochsIndices: {
                type: "t",
                value: null
            },
            epochsData: {
                type: "t",
                value: null
            },
            nodeAttrib: {
                type: "t",
                value: null
            },
            edgeIndices: {
                type: "t",
                value: null
            },
            edgeData: {
                type: "t",
                value: null
            },
            delta: {
                type: "f",
                value: 0.0
            },
            minTime: {
                type: "f",
                value: 0.0
            },
            maxTime: {
                type: "f",
                value: 0.0
            },
            selectedNode: {
                type: "f",
                value: -1.0
            },
            hoverMode: {
                type: "f",
                value: 1.0
            }
        },
        defines: {
            NODESWIDTH: nodesWidth.toFixed(2),
            EPOCHSWIDTH: epochsWidth.toFixed(2),
            EDGESWIDTH: edgesWidth.toFixed(2)
        },
        vertexShader: shaders.vs.passthru,
        fragmentShader: shaders.ss.nodeAttrib,
        blending: 0
    });

	// expose uniforms to the rest of the app
	this.velocityUniforms = velocityShader.uniforms;
	this.positionUniforms = positionShader.uniforms;
	this.nodeAttribUniforms = nodeAttribShader.uniforms;

	let flipflop = true;

	let rtPosition1, rtPosition2, rtVelocity1, rtVelocity2, rtNodeAttrib1, rtNodeAttrib2;

	var that = this;

	function init() {

		const dtPosition = generatePositionTexture(nodesAndEdges, nodesWidth,
            1000);
		const dtVelocity = generateVelocityTexture(nodesAndEdges, nodesWidth);
		const dtNodeAttrib = generateNodeAttribTexture(nodesAndEdges, nodesWidth);

		velocityShader.uniforms.edgeIndices.value = generateIndiciesTexture(
				nodesAndEdges, nodesWidth);
		velocityShader.uniforms.edgeData.value = generateDataTexture(
				nodesAndEdges, edgesWidth);
		velocityShader.uniforms.layoutPositions.value = generateZeroedPositionTexture(
				nodesAndEdges, edgesWidth);

		nodeAttribShader.uniforms.epochsIndices.value = generateIndiciesTexture(
				nodesAndEpochs, nodesWidth);
		nodeAttribShader.uniforms.epochsData.value = generateEpochDataTexture(
				nodesAndEpochs, epochsWidth);
		nodeAttribShader.uniforms.edgeIndices.value = velocityShader.uniforms.edgeIndices.value;
		nodeAttribShader.uniforms.edgeData.value = velocityShader.uniforms.edgeData.value;
		nodeAttribShader.uniforms.nodeIDMappings.value = generateIdMappings(
				nodesAndEpochs, nodesWidth);

		rtPosition1 = getRenderTarget(THREE.RGBAFormat);
		rtPosition2 = rtPosition1.clone();
		that.position = dtPosition;

		rtVelocity1 = getRenderTarget(THREE.RGBAFormat);
		rtVelocity2 = rtVelocity1.clone();

		rtNodeAttrib1 = getRenderTarget(THREE.RGBAFormat);
		rtNodeAttrib2 = rtNodeAttrib1.clone();

		renderTexture(dtPosition, rtPosition1);
		renderTexture(rtPosition1, rtPosition2);

		renderTexture(dtVelocity, rtVelocity1);
		renderTexture(rtVelocity1, rtVelocity2);

		renderTexture(dtNodeAttrib, rtNodeAttrib1);
		renderTexture(rtNodeAttrib1, rtNodeAttrib2);

	}
	this.update = init;


	function getRenderTarget(type) {

        return new THREE.WebGLRenderTarget(nodesWidth, nodesWidth,
				{
					wrapS : THREE.RepeatWrapping,
					wrapT : THREE.RepeatWrapping,
					minFilter : THREE.NearestFilter,
					magFilter : THREE.NearestFilter,
					format : type,
					type : THREE.FloatType,
					stencilBuffer : false
				});
	}

	function renderTexture(input, output) {

		mesh.material = passThruShader;
		passThruUniforms.texture.value = input;
		renderer.render(scene, camera, output);

	};
    this.renderTexture = renderTexture;

    function renderVelocity(position, velocity, output, delta,
			temperature) {

		mesh.material = velocityShader;
		velocityShader.uniforms.positions.value = position;
		velocityShader.uniforms.velocities.value = velocity;
		velocityShader.uniforms.temperature.value = temperature;
		velocityShader.uniforms.delta.value = delta;
		renderer.render(scene, camera, output);

	};

	function renderPosition(position, velocity, output, delta) {

		mesh.material = positionShader;
		positionShader.uniforms.positions.value = position;
		positionShader.uniforms.velocities.value = velocity;
		positionShader.uniforms.delta.value = delta;
		renderer.render(scene, camera, output);

	};

	function renderNodeAttrib(nodeAttrib, output, epochMin, epochMax,
			delta) {

		mesh.material = nodeAttribShader;
		nodeAttribShader.uniforms.nodeAttrib.value = nodeAttrib;
		nodeAttribShader.uniforms.minTime.value = epochMin;
		nodeAttribShader.uniforms.maxTime.value = epochMax;
		nodeAttribShader.uniforms.delta.value = delta;
		renderer.render(scene, camera, output);

	};

	this.simulate = function(delta, temperature, epochMin, epochMax) {

		// TODO: always run simulation, omit small temperatures in the shader
		// TODO: do node hovering in velocity

		if (flipflop) {

			/*if (temperature > 0.1)*/ {

				renderVelocity(rtPosition1, rtVelocity1, rtVelocity2,
						delta, temperature);
				renderPosition(rtPosition1, rtVelocity2, rtPosition2,
						delta);

			}

			renderNodeAttrib(rtNodeAttrib1, rtNodeAttrib2, epochMin,
					epochMax, delta)

		} else {

			/*if (temperature > 0.1)*/ {

				renderVelocity(rtPosition2, rtVelocity2, rtVelocity1,
						delta, temperature);
				renderPosition(rtPosition2, rtVelocity1, rtPosition1,
						delta);

			}

			renderNodeAttrib(rtNodeAttrib2, rtNodeAttrib1, epochMin,
					epochMax, delta)

		}

		// console.log(delta, temperature, epochMin, epochMax);
		// console.log(layoutPositions);

		flipflop = !flipflop;

	};

}
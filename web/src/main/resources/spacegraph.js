"use strict";

class SpaceGraph {

    constructor(targetElement) {
        const view = this.view = targetElement;

        const options = {alpha: true, antialias: true};

        const camera = new THREE.PerspectiveCamera(45, parseFloat(window.innerWidth) / window.innerHeight, 1, 4000);
        camera.position.z = 1750;

        //const controls = new THREE.OrbitControls(camera, view);

        const controls = this.controls = new THREE.TrackballControls(camera);
        controls.rotateSpeed = 1.0;
        controls.zoomSpeed = 5.2;
        controls.panSpeed = 0.8;
        controls.noZoom = false;
        controls.noPan = false;
        controls.staticMoving = true;
        controls.dynamicDampingFactor = 0.5;

        this.updates = new Set();

        const scene = this.scene = new THREE.Scene();
        { //LIGHTING
            scene.add(new THREE.AmbientLight(0x909090));

            var light = new THREE.SpotLight(0xffffff, 1.5);
            light.position.set(0, 500, 1000);
            light.castShadow = false;
            scene.add(light);
            //        light.castShadow = true;
            //        light.shadow = new THREE.LightShadow( new THREE.PerspectiveCamera( 50, 1, 200, 10000 ) );
            //        light.shadow.bias = - 0.00022;
            //        light.shadow.mapSize.width = 2048;
            //        light.shadow.mapSize.height = 2048;
        }

        //const group = this.group = new THREE.Group();
        //scene.add(group);


        /*var helper = new THREE.BoxHelper(new THREE.Mesh(new THREE.BoxGeometry(r, r, r)));
         helper.material.color.setHex(0x080808);
         helper.material.blending = THREE.AdditiveBlending;
         //helper.material.transparent = true;
         group.add(helper);
         */


        const renderer = this.renderer = new THREE.WebGLRenderer(options);
        renderer.autoClear = false;
        renderer.setClearColor(0x000000, 0.0);
        renderer.setPixelRatio(window.devicePixelRatio);
        renderer.gammaInput = true;
        renderer.gammaOutput = true;
        renderer.sortObjects = false;
        const elementGL = this.elementGL = renderer.domElement;


        const dragControls = this.dragControls = new THREE.DragControls(this.draggable = [], camera, elementGL);
        dragControls.addEventListener('dragstart', event => controls.enabled = false);
        dragControls.addEventListener('dragend', event => controls.enabled = true);


        this.EVENTS = new THREEx.DomEvents(camera, window);



//    THREEx.DomEvents.eventNames.forEach(function(eventName){
//        if( eventName === 'mousemove' )	return;
//
//        EVENTS.addEventListener(group, eventName, function(event){
//            console.log(event);
//        }, false);
//    });

        //init CSS3D
        const DOMs = this.DOMs = new THREEx.HtmlMixer.Context(renderer, scene, camera);
        const rendererCSS = DOMs.rendererCss;
        {

            var elementCSS = rendererCSS.domElement;
            elementCSS.style.position = 'absolute';
            elementCSS.style.top = '0px';
            elementCSS.style.width = '100%';
            elementCSS.style.height = '100%';
            view.appendChild(elementCSS);

            elementGL.style.position = 'absolute';
            elementGL.style.top = '0px';
            elementGL.style.width = '100%';
            elementGL.style.height = '100%';
            elementCSS.appendChild(elementGL);

            //elementGL.style.pointerEvents = 'none';

        }


        function resizer() {
            const ww = parseFloat(window.innerWidth);
            const hh = window.innerHeight;
            camera.aspect = ww / hh;
            camera.updateProjectionMatrix();

            renderer.setSize(ww, hh);
            rendererCSS.setSize(ww, hh);
        };


        var lastTimeMsec = null;

        var that = this;
        function animate(nowMsec) {

            that.updates.forEach(x => {
                x.update();
            });

            // measure time
            lastTimeMsec = lastTimeMsec || nowMsec - 1000 / 60.0;
            const deltaMsec = Math.min(200.0, nowMsec - lastTimeMsec);
            lastTimeMsec = nowMsec;
            // call each update function

            const delta = (deltaMsec / 1000);
            const now = nowMsec / 1000;

            update(delta, now);


            renderer.render(scene, camera);
            DOMs.update(delta, now);



            requestAnimationFrame(animate);
        }


        function update(delta, now) {
            controls.update();
        }

        resizer();
        window.addEventListener('resize', resizer, false);
        requestAnimationFrame(animate); //START
    }


    focusSteal(obj, enable) {

        const that = this;
        const eve = that.EVENTS;

        if (enable && !obj.focusSteal) {
            eve.bind(obj, 'mouseover', () => {

                //TODO do this ONLY after testing that nothing else has been picked in front of it

                //console.info('mouseover', elementGL.style.pointerEvents);
                //that.controls.enabled = false;
                //that.dragControls.enabled = false;
                //that.elementGL.style.pointerEvents = 'none';
                return true;
            });

            eve.bind(obj, 'mouseout', () => {
                //that.controls.enabled = true;
                //that.dragControls.enabled = true;
                //that.elementGL.style.pointerEvents = 'inherit';
                //console.info('mouseout', elementGL.style.pointerEvents);
                return true;
            });
            obj.focusSteal = true;
        } else if (!enable && obj.focusSteal) {
            eve.unbind(obj, 'mouseover');
            eve.unbind(obj, 'mouseout');

            obj.focusSteal = false;
        }
    }

    add(object, opt = {draggable: true}) {

        const that = this;

        object.delete = function () {
            that.remove(object);
        };

        that.scene.add(object);


        if (opt.draggable) {
            object.draggable = true;
            this.draggable.push(object);
        }
        if (object.update) {
            this.updates.add(object);
        }

        return object;
    }

    remove(object) {
        if (object.draggable) {
            this.draggable.splice(this.draggable.indexOf(object), 1);
            delete object.draggable;
        }
        if (object.update) {
            this.updates.delete(object);
        }

        if (object.focusSteal) {
            this.focusSteal(object, false);
            delete object.focusSteal;
        }

        this.scene.remove(object);
    }

    /** warning: modifies some of g's methods */
    addGraph(g) {

        const that = this;

        g.nodeAdded = function(nid, n) {
            if (n.spatial)
                throw "spatial field already present";

            //TODO supplied builder
            n.spatial = that.addIcon(nid);
        };

        g.nodeRemoved = function(nid, n) {
            const s = n.spatial;
            if (s) {
                that.remove(s);
                delete n.spatial;
            }
        };

        g.edgeAdded = function(src, tgt, e) {
            //console.log('added', src, target, e);

            //const ssp = src.spatial.position;
            //const ttp = tgt.spatial.position;


            // var shape = new THREE.Shape();
            // shape.moveTo( 0, 0 );
            // shape.lineTo( 1, 0.5 );
            // shape.lineTo( 0, 1 );

            /*shape.moveTo( 0, 0 );
            shape.lineTo( 0.5, 1 );
            shape.lineTo( 1, 0 );*/
            //var geometry = new THREE.ShapeGeometry( shape );


            //var material = new THREE.MeshBasicMaterial( { color: 0x00ff00 } );
            var geometry = new THREE.BoxGeometry(1, 1, 1);
            var object = new THREE.Mesh(geometry,
                new THREE.MeshLambertMaterial({color: Math.random() * 0xffffff})
            );
            //object.position.x = Math.random() * 1000 - 500;
            //object.position.y = Math.random() * 600 - 300;
            //object.position.z = Math.random() * 800 - 400;
            // object.rotation.x = Math.random() * 2 * Math.PI;
            // object.rotation.y = Math.random() * 2 * Math.PI;
            // object.rotation.z = Math.random() * 2 * Math.PI;
            object.up.x = 0;
            object.up.y = 0;
            object.up.z = 1;

            object.scale.x = 1;
            object.scale.y = 1;
            object.scale.z = 1;

            object.castShadow = false; object.receiveShadow = false;
            object.update = () => {
                const ssp = src.spatial.position;
                const ttp = tgt.spatial.position;
                //console.log(ssp, ttp);

                const dir =new THREE.Vector3().subVectors(ttp, ssp);
                const len = dir.length();
                /*object.scale.x = len;
                object.scale.y = len/4;
                object.scale.z = 1;*/

                var thick = 2;
                object.scale.x = thick;
                object.scale.y = thick;
                object.scale.z = len;
                object.position.x = 0.5 * (ssp.x + ttp.x);
                object.position.y = 0.5 * (ssp.y + ttp.y);
                object.position.z = 0.5 * (ssp.z + ttp.z);
                // object.position.x = (ssp.x);
                // object.position.y = (ssp.y);
                // object.position.z = (ssp.z);
                object.lookAt(ttp);

                //geometry.verticesNeedUpdate = true;
                //geometry.normalsNeedUpdate = true;


            };
            e.spatial = object;
            that.add( object, { draggable: false })

            //edges.push(object);

            //const arrow = new THREE.ArrowHelper( dir.normalize(), ssp, len, 0xffffff );

            // console.log(src.spatial);
            // // src.spatial.position.onChange(()=>{
            // //     console.log('ssp change');
            // // });

            //that.scene.add( e.spatial = arrow );

            //TODO supplied builder
        };

        g.edgeRemoved = function(src, target, e) {
            //console.log('removed', src, target, e);
            const s = e.spatial;
            if (s) {
                that.remove(s);
                delete e.spatial;
            }
        };

        return g;
    }

    addIcon() {
        var geometry = new THREE.BoxGeometry(20, 20, 20);
        var object = new THREE.Mesh(geometry,
            new THREE.MeshLambertMaterial({color: Math.random() * 0xffffff})
        );
        object.position.x = Math.random() * 1000 - 500;
        object.position.y = Math.random() * 600 - 300;
        object.position.z = Math.random() * 800 - 400;
        object.rotation.x = Math.random() * 2 * Math.PI;
        object.rotation.y = Math.random() * 2 * Math.PI;
        object.rotation.z = Math.random() * 2 * Math.PI;
        object.scale.x = Math.random() * 5 + 1;
        object.scale.y = Math.random() * 5 + 1;
        object.scale.z = Math.random() * 5 + 1;
        object.castShadow = false;
        object.receiveShadow = false;

        return this.add(object, {draggable: true});
    }

    /** https://github.com/mrdoob/three.js/blob/master/examples/css3d_youtube.html  */
    addIFrame(url, pw, ph) {
        //var ele = document.createElement('div');
        //ele.style.width = '100%';
        //ele.style.height = '100%';
        var iframe = document.createElement('iframe');
        iframe.style.border = '0px';
        iframe.src = url;
//            iframe.style.width = '100%';
//            iframe.style.height = '100%';
        //ele.appendChild(iframe);
        return this.addDOM(
            //($('<div>').append(iframe))[0],
            iframe,
            pw, ph);
    }

    addDOM(ele, pw, ph) {

        //const wrapper = ($('<div>').append(ele))[0];

        var planeMaterial   = new THREE.MeshBasicMaterial({
            opacity	: 0.2,
            color	: new THREE.Color('red'),
            blending: THREE.NoBlending,
            side	: THREE.DoubleSide,
        });


        const planeW = 1;
        //const planeH = 1;
        const planeH = (parseFloat(ph)/pw);

        var geometry	= new THREE.PlaneGeometry( planeW, planeH );
        const obj = new THREE.Mesh( geometry, planeMaterial );
        const opts = {
            planeW: planeW, planeH: planeH,
            object3d: obj,
            elementW: pw //pixels

        };


        const mixerPlane = new THREEx.HtmlMixer.Plane(this.DOMs, ele, opts);
        obj.scale.multiplyScalar(1000);


        this.focusSteal(obj, true);

        const d = this.add(obj, {draggable: false});

        //console.log( $(ele).parent(),$(ele).parent().width());
        //$(ele).css({'width': '', 'height': ''});
        //console.log( pw, ph, $(ele)[0] );

        // wrapper.style.width = pw + 'px';
        // wrapper.style.height = ph + 'px';
        //$(ele).addClass('max');

        return d;
    }

    stop() {
        //TODO
    }
}



/*
 {
 const group = new THREE.Group();
 scene.add(group);

 var segments = maxParticleCount * maxParticleCount;

 var POS = new Float32Array(segments * 3);
 var COLOR = new Float32Array(segments * 3);

 //        var pMaterial = new THREE.PointsMaterial({
 //            color: 0xFFFFFF,
 //            size: 3,
 //            blending: THREE.AdditiveBlending,
 //            transparent: true,
 //            sizeAttenuation: false
 //        });
 var material = new THREE.LineBasicMaterial({
 vertexColors: THREE.VertexColors,
 blending: THREE.AdditiveBlending,
 transparent: true
 });

 nodes = new THREE.BufferGeometry();
 nodePos = new Float32Array(maxParticleCount * 3);
 for (var i = 0; i < maxParticleCount; i++) {
 var x = Math.random() * r - r / 2;
 var y = Math.random() * r - r / 2;
 var z = Math.random() * r - r / 2;
 nodePos[i * 3] = x;
 nodePos[i * 3 + 1] = y;
 nodePos[i * 3 + 2] = z;
 // add it to the geometry
 nodeData.push({
 vel: new THREE.Vector3(-1 + Math.random() * 2, -1 + Math.random() * 2, -1 + Math.random() * 2),
 arity: 0
 });
 }
 nodes.setDrawRange(0, particleCount);
 nodes.addAttribute('position', new THREE.BufferAttribute(nodePos, 3).setDynamic(true));


 //groupGL.add(pointCloud = new THREE.Points(nodes, pMaterial));


 var geometry = new THREE.BufferGeometry();
 geometry.addAttribute('position', new THREE.BufferAttribute(POS, 3).setDynamic(true));
 geometry.addAttribute('color', new THREE.BufferAttribute(COLOR, 3).setDynamic(true));
 geometry.computeBoundingSphere();
 geometry.setDrawRange(0, 0);

 edgeMesh = new THREE.LineSegments(geometry, material);
 group.add(edgeMesh);

 }

 //    {
 //        var gui = new dat.GUI();
 //        gui.add(effectController, "showLines").onChange(function (value) {
 //            edgeMesh.visible = value;
 //        });
 //        gui.add(effectController, "minDistance", 10, 300);
 //        gui.add(effectController, "limitConnections");
 //        gui.add(effectController, "maxConnections", 0, 30, 1);
 //        gui.add(effectController, "particleCount", 0, maxParticleCount, 1).onChange(function (value) {
 //            particleCount = parseInt(value);
 //            nodes.setDrawRange(0, particleCount);
 //        });
 //    }


 //---------------------


 if (nodeData.length > 0) {
 var vertexpos = 0;
 var colorpos = 0;
 var numEdges = 0;

 for (var i = 0; i < particleCount; i++)
 nodeData[i].arity = 0;

 for (var i = 0; i < particleCount; i++) {

 // get the particle
 var A = nodeData[i];

 const iii = i * 3;

 nodePos[iii] += A.vel.x;
 nodePos[iii + 1] += A.vel.y;
 nodePos[iii + 2] += A.vel.z;

 if (nodePos[iii + 1] < -rHalf || nodePos[iii + 1] > rHalf)
 A.vel.y = -A.vel.y;

 if (nodePos[iii] < -rHalf || nodePos[iii] > rHalf)
 A.vel.x = -A.vel.x;

 if (nodePos[iii + 2] < -rHalf || nodePos[iii + 2] > rHalf)
 A.vel.z = -A.vel.z;

 if (effectController.limitConnections && A.arity >= effectController.maxConnections)
 continue;

 const minDistanceSq = effectController.minDistance * effectController.minDistance;

 // Check collision
 for (var j = i + 1; j < particleCount; j++) {

 var B = nodeData[j];
 if (effectController.limitConnections && B.arity >= effectController.maxConnections)
 continue;

 const jjj = j * 3;


 var distSq = 0;
 var dx = nodePos[iii] - nodePos[jjj];
 distSq += dx * dx;
 if (distSq > minDistanceSq) continue;
 var dy = nodePos[iii + 1] - nodePos[jjj + 1];
 distSq += dy * dy;
 if (distSq > minDistanceSq) continue;
 var dz = nodePos[iii + 2] - nodePos[jjj + 2];
 distSq += dz * dz;
 if (distSq > minDistanceSq) continue;


 A.arity++;
 B.arity++;

 var alpha = 1.0 - Math.sqrt(distSq) / effectController.minDistance;

 POS[vertexpos++] = nodePos[iii];
 POS[vertexpos++] = nodePos[iii + 1];
 POS[vertexpos++] = nodePos[iii + 2];

 POS[vertexpos++] = nodePos[jjj];
 POS[vertexpos++] = nodePos[jjj + 1];
 POS[vertexpos++] = nodePos[jjj + 2];

 COLOR[colorpos++] = alpha;
 COLOR[colorpos++] = alpha;
 COLOR[colorpos++] = alpha;

 COLOR[colorpos++] = alpha;
 COLOR[colorpos++] = alpha;
 COLOR[colorpos++] = alpha;

 numEdges++;

 }
 }


 edgeMesh.geometry.setDrawRange(0, numEdges * 2);
 edgeMesh.geometry.attributes.position.needsUpdate = true;
 edgeMesh.geometry.attributes.color.needsUpdate = true;

 //pointCloud.geometry.attributes.position.needsUpdate = true;
 }


 */

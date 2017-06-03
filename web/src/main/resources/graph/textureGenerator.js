function generatePositionTexture(inputArray, textureSize, size) {

    const bounds = size;
    const bounds_half = bounds / 2;

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (let i = 0; i < textureArray.length; i += 4) {

        if (i < inputArray.length * 4) {

            const x = Math.random() * bounds - bounds_half;
            const y = Math.random() * bounds - bounds_half;
            const z = Math.random() * bounds - bounds_half;

            textureArray[i] = x;
            textureArray[i + 1] = y;
            textureArray[i + 2] = z;
            textureArray[i + 3] = 1.0;

        } else {

            // fill the remaining pixels with -1
            textureArray[i] = -1.0;
            textureArray[i + 1] = -1.0;
            textureArray[i + 2] = -1.0;
            textureArray[i + 3] = -1.0;

        }

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('position', texture.image.data);
    return texture;

}



function generateIdMappings(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    let counter = 0;

    for (let i = 0; i < textureArray.length; i += 4) {

        if (i < inputArray.length * 4) {

            textureArray[i] = counter;
            textureArray[i + 1] = 0;
            textureArray[i + 2] = 0;
            textureArray[i + 3] = 0;

        } else {

            // fill the remaining pixels with -1
            textureArray[i] = -1.0;
            textureArray[i + 1] = -1.0;
            textureArray[i + 2] = -1.0;
            textureArray[i + 3] = -1.0;

        }

        counter++;

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('generateIdMappings', texture.image.data);
    return texture;

}


function generateCircularLayout(inputArray, textureSize) {

    const increase = Math.PI * 2 / inputArray.length;
    let angle = 0;
    const radius = inputArray.length * 4 * 2;

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (let i = 0; i < textureArray.length; i += 4) {

        if (i < inputArray.length * 4) {


            // modify to change the radius and position of a circle
            const x = radius * Math.cos(angle);
            const y = radius * Math.sin(angle);
            const z = 0;
            const w = 1.0;

            textureArray[i] = x;
            textureArray[i + 1] = y;
            textureArray[i + 2] = z;
            textureArray[i + 3] = w;

            angle += increase;

        } else {

            textureArray[i] = -1.0;
            textureArray[i + 1] = -1.0;
            textureArray[i + 2] = -1.0;
            textureArray[i + 3] = -1.0;

        }

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('position', texture.image.data);
    return texture;

}


function generateSphericalLayout(inputArray, textureSize) {

    //var increase = Math.PI * 2 / inputArray.length;
    //var angle = 0;
    const radius = inputArray.length * 4;

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (var i = 0, l = inputArray.length; i < l; i++) {

        const phi = Math.acos(-1 + ( 2 * i ) / l);
        const theta = Math.sqrt(l * Math.PI) * phi;


        // modify to change the radius and position of a circle
        const x = radius * Math.cos(theta) * Math.sin(phi);
        const y = radius * Math.sin(theta) * Math.sin(phi);
        const z = radius * Math.cos(phi);
        const w = 1.0;

        textureArray[i * 4] = z;
        textureArray[i * 4 + 1] = y;
        textureArray[i * 4 + 2] = x;
        textureArray[i * 4 + 3] = w;

    }

    for (var i = inputArray.length * 4; i < textureArray.length; i++) {

        // fill unused RGBA slots with -1
        textureArray[i] = -1;

    }


    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
//console.log('position', texture.image.data);
    return texture;

}


function generateHelixLayout(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (var i = 0, l = inputArray.length; i < l; i++) {

        const phi = i * 0.125 + Math.PI;


        // modify to change the radius and position of a circle
        const x = i * 15;
        const y = 500 * Math.sin(phi);
        const z = 500 * Math.cos(phi);
        const w = 1.0;

        textureArray[i * 4] = x;
        textureArray[i * 4 + 1] = y;
        textureArray[i * 4 + 2] = z;
        textureArray[i * 4 + 3] = w;

    }

    for (var i = inputArray.length * 4; i < textureArray.length; i++) {

        // fill unused RGBA slots with -1
        textureArray[i] = -1;

    }


    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
//console.log('position', texture.image.data);
    return texture;

}


function generateGridLayout(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (var i = 0; i < inputArray.length; i++) {

        // modify to change the radius and position of a circle
        const x = ( ( i % 5 ) * 500 ) - 1000;
        const y = ( -( Math.floor(i / 5) % 5 ) * 500 ) + 1000;
        const z = ( Math.floor(i / 25) ) * 500 - 1000;
        const w = 1.0;

        textureArray[i * 4] = x;
        textureArray[i * 4 + 1] = y;
        textureArray[i * 4 + 2] = z;
        textureArray[i * 4 + 3] = w;

    }

    for (var i = inputArray.length * 4; i < textureArray.length; i++) {

        // fill unused RGBA slots with -1
        textureArray[i] = -1;

    }


    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
//console.log('position', texture.image.data);
    return texture;

}


function generateZeroedPositionTexture(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (let i = 0; i < textureArray.length; i += 4) {

        if (i < inputArray.length * 4) {

            textureArray[i] = 0.0;
            textureArray[i + 1] = 0.0;
            textureArray[i + 2] = 0.0;
            textureArray[i + 3] = 0.0;

        } else {

            // fill the remaining pixels with -1
            textureArray[i] = -1.0;
            textureArray[i + 1] = -1.0;
            textureArray[i + 2] = -1.0;
            textureArray[i + 3] = -1.0;

        }

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('position', texture.image.data);
    return texture;

}


function generateVelocityTexture(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (let i = 0; i < textureArray.length; i += 4) {

        if (i < inputArray.length * 4) {

            textureArray[i] = 0.0;
            textureArray[i + 1] = 0.0;
            textureArray[i + 2] = 0.0;
            textureArray[i + 3] = 0.0;

        } else {

            // fill the remaining pixels with -1
            textureArray[i] = -1.0;
            textureArray[i + 1] = -1.0;
            textureArray[i + 2] = -1.0;
            textureArray[i + 3] = -1.0;

        }

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('velocities', texture.image.data);
    return texture;

}


function generateNodeAttribTexture(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    for (let i = 0; i < textureArray.length; i += 4) {

        // x = size
        // y = opacity
        // z = unused
        // w = unused

        if (i < inputArray.length * 4) {

            textureArray[i] = 200.0;
            textureArray[i + 1] = 0.2;
            textureArray[i + 2] = 0.0;
            textureArray[i + 3] = 0.0;

        } else {

            // fill the remaining pixels with -1
            textureArray[i] = -1.0;
            textureArray[i + 1] = -1.0;
            textureArray[i + 2] = -1.0;
            textureArray[i + 3] = -1.0;

        }

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('velocities', texture.image.data);
    return texture;

}


function generateIndiciesTexture(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);
    let currentPixel = 0;
    let currentCoord = 0;

    for (var i = 0; i < inputArray.length; i++) {

        //keep track of the beginning of the array for this node

        const startPixel = currentPixel;
        const startCoord = currentCoord;

        for (let j = 0; j < inputArray[i].length; j++) {

            // look inside each node array and see how many things it links to

            currentCoord++;

            if (currentCoord === 4) {

                // remainder is only 0-3.  If you hit 4, increment pixel and reset coord

                currentPixel++;
                currentCoord = 0;

            }

        }

        //write the two sets of texture indices out.  We'll fill up an entire pixel on each pass
        textureArray[i * 4] = startPixel;
        textureArray[i * 4 + 1] = startCoord;
        textureArray[i * 4 + 2] = currentPixel;
        textureArray[i * 4 + 3] = currentCoord;

    }

    for (var i = inputArray.length * 4; i < textureArray.length; i++) {

        // fill unused RGBA slots with -1
        textureArray[i] = -1;

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('indicies', texture.image.data);
    return texture;

}


function generateDataTexture(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);

    let currentIndex = 0;
    for (var i = 0; i < inputArray.length; i++) {

        for (let j = 0; j < inputArray[i].length; j++) {

            textureArray[currentIndex] = inputArray[i][j];
            currentIndex++;

        }
    }

    for (var i = currentIndex; i < textureArray.length; i++) {

        //fill unused RGBA slots with -1
        textureArray[i] = -1;

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('edge data texture:', texture.image.data);
    return texture;

}

function generateEpochDataTexture(inputArray, textureSize) {

    const textureArray = new Float32Array(textureSize * textureSize * 4);
    //console.log(textureArray);

    let currentIndex = 0;
    for (var i = 0; i < inputArray.length; i++) {
        //console.log('working on', i, j, inputArray[i]);
        for (let j = 0; j < inputArray[i].length; j++) {

            //console.log(currentIndex, inputArray[i][j]);
            textureArray[currentIndex] = inputArray[i][j] - epochOffset;
            currentIndex++;

        }
    }

    for (var i = currentIndex; i < textureArray.length; i++) {

        //fill unused RGBA slots with -1
        textureArray[i] = -1;

    }

    const texture = new THREE.DataTexture(textureArray, textureSize, textureSize, THREE.RGBAFormat, THREE.FloatType);
    texture.needsUpdate = true;
    //console.log('epoch data texture:', texture.image.data);
    return texture;

}


function indexTextureSize(num) {
    let power = 1;
    while (power * power < num) {
        power *= 2;
    }
    return power / 2 > 1 ? power : 2;

}


function dataTextureSize(num) {

    return indexTextureSize(Math.ceil(num / 4));
}




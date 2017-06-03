function GUIInterface() {

	function enableForceDirected() {

		console.log( 'force-directed' );
		simulator.velocityUniforms.layoutPositions.value = generateZeroedPositionTexture( nodesAndEdges, nodesWidth );
		temperature = 100;

	}

	function enableCircular() {

		console.log( 'circular' );
		simulator.velocityUniforms.layoutPositions.value = generateCircularLayout( nodesAndEdges, nodesWidth );
		temperature = 1000;

	}

	function enableSpherical() {

		console.log( 'spherical' );
		simulator.velocityUniforms.layoutPositions.value = generateSphericalLayout( nodesAndEdges, nodesWidth );
		temperature = 1000;

	}


	function enableHelix() {

		console.log( 'helix' );
		simulator.velocityUniforms.layoutPositions.value = generateHelixLayout( nodesAndEdges, nodesWidth );
		temperature = 1000;

	}

	function enableGrid() {

		console.log( 'grid' );
		simulator.velocityUniforms.layoutPositions.value = generateGridLayout( nodesAndEdges, nodesWidth );
		temperature = 1000;

	}


	this.init = function() {

		const container = document.createElement('div');
		container.id = 'layouts';

		const forceDirected = document.createElement('div');
		forceDirected.id = 'forceDirected';
		$( forceDirected ).append( "<img src='textures/forceDirected.png' height='32px' width='32px'>" );


		const circular = document.createElement('div');
		circular.id = 'circular';
		$( circular ).append( "<img src='textures/circle.png' height='32px' width='32px'>" );

		const spherical = document.createElement('div');
		spherical.id = 'spherical';
		$( spherical ).append( "<img src='textures/sphere.png' height='32px' width='32px'>" );

		const helix = document.createElement('div');
		helix.id = 'helix';
		$( helix ).append( "<img src='textures/spring.png' height='32px' width='32px'>" );

		const grid = document.createElement('div');
		grid.id = 'grid';
		$( grid ).append( "<img src='textures/square.png' height='32px' width='32px'>" );


		container.appendChild( forceDirected );
		container.appendChild( document.createElement( 'br' ) );
		container.appendChild( circular );
		container.appendChild( document.createElement( 'br' ) );
		container.appendChild( spherical );
		container.appendChild( document.createElement( 'br' ) );
		container.appendChild( helix );
		container.appendChild( document.createElement( 'br' ) );
		container.appendChild( grid );
		document.body.appendChild( container );


		$( '#forceDirected' ).on( 'click', enableForceDirected );
		$( '#circular' ).on( 'click', enableCircular );
		$( '#spherical' ).on( 'click', enableSpherical );
		$( '#helix' ).on( 'click', enableHelix );
		$( '#grid' ).on( 'click', enableGrid );
	};


}

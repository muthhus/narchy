/*
 * qfiles.java
 * Copyright (C) 2003
 *
 * $Id: qfiles.java,v 1.6 2005-05-07 23:40:49 cawe Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.qcommon;

import jake2.Defines;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * qfiles
 * 
 * @author cwei
 */
public class qfiles {
	//
	// qfiles.h: quake file formats
	// This file must be identical in the quake and utils directories
	//

	/*
	========================================================================
	
	The .pak files are just a linear collapse of a directory tree
	
	========================================================================
	*/

	/*
	========================================================================
	
	PCX files are used for as many images as possible
	
	========================================================================
	*/
	public static class pcx_t {

		// size of byte arrays
		static final int PALETTE_SIZE = 48;
		static final int FILLER_SIZE = 58;

		public final byte manufacturer;
		public final byte version;
		public final byte encoding;
		public final byte bits_per_pixel;
		public final int xmin;
		public final int ymin;
		public final int xmax;
		public final int ymax; // unsigned short
		public final int hres;
		public final int vres; // unsigned short
		public final byte[] palette; //unsigned byte; size 48
		public final byte reserved;
		public final byte color_planes;
		public final int bytes_per_line; // unsigned short
		public final int palette_type; // unsigned short
		public final byte[] filler; // size 58
		public final ByteBuffer data; //unbounded data

		public pcx_t(byte[] dataBytes) {
			this(ByteBuffer.wrap(dataBytes));
		}

		public pcx_t(ByteBuffer b) {
			// is stored as little endian
			b.order(ByteOrder.LITTLE_ENDIAN);

			// fill header
			manufacturer = b.get();
			version = b.get();
			encoding = b.get();
			bits_per_pixel = b.get();
			xmin = b.getShort() & 0xffff;
			ymin = b.getShort() & 0xffff;
			xmax = b.getShort() & 0xffff;
			ymax = b.getShort() & 0xffff;
			hres = b.getShort() & 0xffff;
			vres = b.getShort() & 0xffff;
			b.get(palette = new byte[PALETTE_SIZE]);
			reserved = b.get();
			color_planes = b.get();
			bytes_per_line = b.getShort() & 0xffff;
			palette_type = b.getShort() & 0xffff;
			b.get(filler = new byte[FILLER_SIZE]);

			// fill data
			data = b.slice();
		}
	}

	/*
	========================================================================
	
	TGA files are used for sky planes
	
	========================================================================
	*/
	public static class tga_t {
		
		// targa header
		public final int id_length;
		public final int colormap_type;
		public final int image_type; // unsigned char
		public final int colormap_index;
		public final int colormap_length; // unsigned short
		public final int colormap_size; // unsigned char
		public final int x_origin;
		public final int y_origin;
		public final int width;
		public final int height; // unsigned short
		public final int pixel_size;
		public final int attributes; // unsigned char

		public final ByteBuffer data; // (un)compressed data

		public tga_t(byte[] dataBytes) {
			this(ByteBuffer.wrap(dataBytes));
		}

		public tga_t(ByteBuffer b) {
			// is stored as little endian
			b.order(ByteOrder.LITTLE_ENDIAN);

			// fill header
			id_length = b.get() & 0xFF;
			colormap_type = b.get() & 0xFF;
			image_type = b.get() & 0xFF;
			colormap_index = b.getShort() & 0xFFFF;
			colormap_length = b.getShort() & 0xFFFF;
			colormap_size = b.get() & 0xFF;
			x_origin = b.getShort() & 0xFFFF;
			y_origin = b.getShort() & 0xFFFF;
			width = b.getShort() & 0xFFFF;
			height = b.getShort() & 0xFFFF;
			pixel_size = b.get() & 0xFF;
			attributes = b.get() & 0xFF;

			// fill data
			data = b.slice();
		}			

	}
	
	/*
	========================================================================
	
	.MD2 triangle model file format
	
	========================================================================
	*/
	
	public static final int IDALIASHEADER =	(('2'<<24)+('P'<<16)+('D'<<8)+'I');
	public static final int ALIAS_VERSION = 8;
	
	public static final int MAX_TRIANGLES = 4096;
	public static final int MAX_VERTS = 2048;
	public static final int MAX_FRAMES = 512;
	public static final int MAX_MD2SKINS = 32;
	public static final int MAX_SKINNAME = 64;
	
	public static class dstvert_t {
		public final short s;
		public final short t;
		
		public dstvert_t(ByteBuffer b) {
			s = b.getShort();
			t = b.getShort();
		}
	}

	public static class dtriangle_t {
		public final short[] index_xyz = { 0, 0, 0 };
		public final short[] index_st = { 0, 0, 0 };
		
		public dtriangle_t(ByteBuffer b) {
			index_xyz[0] = b.getShort();
			index_xyz[1] = b.getShort();
			index_xyz[2] = b.getShort();
			
			index_st[0] = b.getShort();
			index_st[1] = b.getShort();
			index_st[2] = b.getShort();
		}
	}

	public static final int DTRIVERTX_V0 =  0;
	public static final int DTRIVERTX_V1 = 1;
	public static final int DTRIVERTX_V2 = 2;
	public static final int DTRIVERTX_LNI = 3;
	public static final int DTRIVERTX_SIZE = 4;
	
	public static class  daliasframe_t {
		public final float[] scale = {0, 0, 0}; // multiply byte verts by this
		public final float[] translate = {0, 0, 0};	// then add this
		public final String name; // frame name from grabbing (size 16)
		public int[] verts;	// variable sized
		
		public daliasframe_t(ByteBuffer b) {
			scale[0] = b.getFloat();	scale[1] = b.getFloat();	scale[2] = b.getFloat();
			translate[0] = b.getFloat(); translate[1] = b.getFloat(); translate[2] = b.getFloat();
			byte[] nameBuf = new byte[16];
			b.get(nameBuf);
			name = new String(nameBuf).trim();
		}
	}
	
	//	   the glcmd format:
	//	   a positive integer starts a tristrip command, followed by that many
	//	   vertex structures.
	//	   a negative integer starts a trifan command, followed by -x vertexes
	//	   a zero indicates the end of the command list.
	//	   a vertex consists of a floating point s, a floating point t,
	//	   and an integer vertex index.
	
	public static class dmdl_t {
		public final int ident;
		public final int version;

		public final int skinwidth;
		public final int skinheight;
		public final int framesize; // byte size of each frame

		public final int num_skins;
		public final int num_xyz;
		public final int num_st; // greater than num_xyz for seams
		public final int num_tris;
		public final int num_glcmds; // dwords in strip/fan command list
		public final int num_frames;

		public final int ofs_skins; // each skin is a MAX_SKINNAME string
		public final int ofs_st; // byte offset from start for stverts
		public final int ofs_tris; // offset for dtriangles
		public final int ofs_frames; // offset for first frame
		public final int ofs_glcmds;
		public final int ofs_end; // end of file
		
		// wird extra gebraucht
		public String[] skinNames;
		public dstvert_t[] stVerts;
		public dtriangle_t[] triAngles;
		public int[] glCmds;
		public daliasframe_t[] aliasFrames;
		
		
		public dmdl_t(ByteBuffer b) {
			ident = b.getInt();
			version = b.getInt();

			skinwidth = b.getInt();
			skinheight = b.getInt();
			framesize = b.getInt(); // byte size of each frame

			num_skins = b.getInt();
			num_xyz = b.getInt();
			num_st = b.getInt(); // greater than num_xyz for seams
			num_tris = b.getInt();
			num_glcmds = b.getInt(); // dwords in strip/fan command list
			num_frames = b.getInt();

			ofs_skins = b.getInt(); // each skin is a MAX_SKINNAME string
			ofs_st = b.getInt(); // byte offset from start for stverts
			ofs_tris = b.getInt(); // offset for dtriangles
			ofs_frames = b.getInt(); // offset for first frame
			ofs_glcmds = b.getInt();
			ofs_end = b.getInt(); // end of file
		}

		/*
		 * new members for vertex array handling
		 */
		public FloatBuffer textureCoordBuf = null;
		public ShortBuffer vertexIndexBuf = null;
		public int[] counts = null;
		public ShortBuffer[] indexElements = null;
	}
	
	/*
	========================================================================
	
	.SP2 sprite file format
	
	========================================================================
	*/
	// little-endian "IDS2"
	public static final int IDSPRITEHEADER = (('2'<<24)+('S'<<16)+('D'<<8)+'I');
	public static final int SPRITE_VERSION = 2;

	public static class dsprframe_t {
		public final int width;
		public final int height;
		public final int origin_x;
		public final int origin_y; // raster coordinates inside pic
		public final String name; // name of pcx file (MAX_SKINNAME)
		
		public dsprframe_t(ByteBuffer b) {
			width = b.getInt();
			height = b.getInt();
			origin_x = b.getInt();
			origin_y = b.getInt();
			
			byte[] nameBuf = new byte[MAX_SKINNAME];
			b.get(nameBuf);
			name = new String(nameBuf).trim();
		}
	}

	public static class dsprite_t {
		public final int ident;
		public final int version;
		public final int numframes;
		public final dsprframe_t[] frames; // variable sized
		
		public dsprite_t(ByteBuffer b) {
			ident = b.getInt();
			version = b.getInt();
			numframes = b.getInt();
			
			frames = new dsprframe_t[numframes];
			for (int i=0; i < numframes; i++) {
				frames[i] = new dsprframe_t(b);	
			}
		}
	}
	
	/*
	==============================================================================
	
	  .WAL texture file format
	
	==============================================================================
	*/
	public static class miptex_t {

		static final int MIPLEVELS = 4;
		static final int NAME_SIZE = 32;

		public final String name; // char name[32];
		public final int width;
		public final int height;
		public final int[] offsets = new int[MIPLEVELS]; // 4 mip maps stored
		// next frame in animation chain
		public final String animname; //	char	animname[32];
		public final int flags;
		public final int contents;
		public final int value;

		public miptex_t(byte[] dataBytes) {
			this(ByteBuffer.wrap(dataBytes));
		}

		public miptex_t(ByteBuffer b) {
			// is stored as little endian
			b.order(ByteOrder.LITTLE_ENDIAN);

			byte[] nameBuf = new byte[NAME_SIZE];
			// fill header
			b.get(nameBuf);
			name = new String(nameBuf).trim();
			width = b.getInt();
			height = b.getInt();
			offsets[0] = b.getInt();
			offsets[1] = b.getInt();
			offsets[2] = b.getInt();
			offsets[3] = b.getInt();
			b.get(nameBuf);
			animname = new String(nameBuf).trim();
			flags = b.getInt();
			contents = b.getInt();
			value = b.getInt();
		}

	}
	
	/*
	==============================================================================
	
	  .BSP file format
	
	==============================================================================
	*/

	public static final int IDBSPHEADER = (('P'<<24)+('S'<<16)+('B'<<8)+'I');

	// =============================================================================

	public static class dheader_t {

		public dheader_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);
			this.ident = bb.getInt();
			this.version = bb.getInt();

			for (int n = 0; n < Defines.HEADER_LUMPS; n++)
				lumps[n] = new lump_t(bb.getInt(), bb.getInt());

		}

		public final int ident;
		public final int version;
		public final lump_t[] lumps = new lump_t[Defines.HEADER_LUMPS];
	}

	public static class dmodel_t {

		public dmodel_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);

			for (int j = 0; j < 3; j++)
				mins[j] = bb.getFloat();

			for (int j = 0; j < 3; j++)
				maxs[j] = bb.getFloat();

			for (int j = 0; j < 3; j++)
				origin[j] = bb.getFloat();

			headnode = bb.getInt();
			firstface = bb.getInt();
			numfaces = bb.getInt();
		}
		public final float[] mins = { 0, 0, 0 };
		public final float[] maxs = { 0, 0, 0 };
		public final float[] origin = { 0, 0, 0 }; // for sounds or lights
		public final int headnode;
		public final int firstface;
		public final int numfaces; // submodels just draw faces
		// without walking the bsp tree

		public static final int SIZE = 3 * 4 + 3 * 4 + 3 * 4 + 4 + 8;
	}
	
	public static class dvertex_t {
		
		public static final int SIZE = 3 * 4; // 3 mal 32 bit float 
		
		public final float[] point = { 0, 0, 0 };
		
		public dvertex_t(ByteBuffer b) {
			point[0] = b.getFloat();
			point[1] = b.getFloat();
			point[2] = b.getFloat();
		}
	}


	// planes (x&~1) and (x&~1)+1 are always opposites
	public static class dplane_t {

		public dplane_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);

			normal[0] = (bb.getFloat());
			normal[1] = (bb.getFloat());
			normal[2] = (bb.getFloat());

			dist = (bb.getFloat());
			type = (bb.getInt());
		}

		public final float[] normal = { 0, 0, 0 };
		public final float dist;
		public final int type; // PLANE_X - PLANE_ANYZ ?remove? trivial to regenerate

		public static final int SIZE = 3 * 4 + 4 + 4;
	}

	public static class dnode_t {

		public dnode_t(ByteBuffer bb) {

			bb.order(ByteOrder.LITTLE_ENDIAN);
			planenum = bb.getInt();

			children[0] = bb.getInt();
			children[1] = bb.getInt();

			short[] mins = this.mins;
			for (int j = 0; j < 3; j++) {
				mins[j] = bb.getShort();
			}

			short[] maxs = this.maxs;
			for (int j = 0; j < 3; j++)
				maxs[j] = bb.getShort();

			firstface = bb.getShort() & 0xffff;
			numfaces = bb.getShort() & 0xffff;

		}

		public final int planenum;
		public final int[] children = { 0, 0 };
		// negative numbers are -(leafs+1), not nodes
		public final short[] mins = { 0, 0, 0 }; // for frustom culling
		public final short[] maxs = { 0, 0, 0 };

		/*
		unsigned short	firstface;
		unsigned short	numfaces;	// counting both sides
		*/

		public final int firstface;
		public final int numfaces;

		public static final int SIZE = 4 + 8 + 6 + 6 + 2 + 2; // counting both sides
	}
	


	// note that edge 0 is never used, because negative edge nums are used for
	// counterclockwise use of the edge in a face
	
	public static class dedge_t {
		// unsigned short v[2];
		final int v[] = { 0, 0 };
	}
	
	public static class dface_t {
		
		public static final int SIZE =
				4 * Defines.SIZE_OF_SHORT
			+	2 * Defines.SIZE_OF_INT
			+	Defines.MAXLIGHTMAPS;

		//unsigned short	planenum;
		public final int planenum;
		public final short side;

		public final int firstedge; // we must support > 64k edges
		public final short numedges;
		public final short texinfo;

		// lighting info
		public final byte[] styles = new byte[Defines.MAXLIGHTMAPS];
		public final int lightofs; // start of [numstyles*surfsize] samples
		
		public dface_t(ByteBuffer b) {
			planenum = b.getShort() & 0xFFFF;
			side = b.getShort();
			firstedge = b.getInt();
			numedges = b.getShort();
			texinfo = b.getShort();
			b.get(styles);
			lightofs = b.getInt();
		}
		
	}

	public static class dleaf_t {

		public dleaf_t(byte[] cmod_base, int i, int j) {
			this(ByteBuffer.wrap(cmod_base, i, j).order(ByteOrder.LITTLE_ENDIAN));
		}

		public dleaf_t(ByteBuffer bb) {
			contents = bb.getInt();
			cluster = bb.getShort();
			area = bb.getShort();

			short[] mins = this.mins;
			mins[0] = bb.getShort();
			mins[1] = bb.getShort();
			mins[2] = bb.getShort();

			short[] maxs = this.maxs;
			maxs[0] = bb.getShort();
			maxs[1] = bb.getShort();
			maxs[2] = bb.getShort();

			firstleafface = bb.getShort() & 0xffff;
			numleaffaces = bb.getShort() & 0xffff;

			firstleafbrush = bb.getShort() & 0xffff;
			numleafbrushes = bb.getShort() & 0xffff;
		}

		public static final int SIZE = 4 + 8 * 2 + 4 * 2;

		public final int contents; // OR of all brushes (not needed?)

		public final short cluster;
		public final short area;

		public final short[] mins = { 0, 0, 0 }; // for frustum culling
		public final short[] maxs = { 0, 0, 0 };

		public final int firstleafface; // unsigned short
		public final int numleaffaces; // unsigned short

		public final int firstleafbrush; // unsigned short
		public final int numleafbrushes; // unsigned short
	}
	
	public static class dbrushside_t {

		public dbrushside_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);

			planenum = bb.getShort() & 0xffff;
			texinfo = bb.getShort();
		}

		//unsigned short planenum;
		final int planenum; // facing out of the leaf

		final short texinfo;

		public static final int SIZE = 4;
	}
	
	public static class dbrush_t {

		public dbrush_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);
			firstside = bb.getInt();
			numsides = bb.getInt();
			contents = bb.getInt();
		}

		public static final int SIZE = 3 * 4;

		final int firstside;
		final int numsides;
		final int contents;
	}

	//	#define	ANGLE_UP	-1
	//	#define	ANGLE_DOWN	-2

	// the visibility lump consists of a header with a count, then
	// byte offsets for the PVS and PHS of each cluster, then the raw
	// compressed bit vectors
	// #define	DVIS_PVS	0
	// #define	DVIS_PHS	1

	public static class dvis_t {

		public dvis_t(ByteBuffer bb) {
			final int numclusters = this.numclusters = bb.getInt();
			bitofs = new int[numclusters][2];

			int[][] b = this.bitofs;
			for (int i = 0; i < numclusters; i++) {
				int[] bi = b[i];
				bi[0] = bb.getInt();
				bi[1] = bb.getInt();
			}
		}

		public final int numclusters;
		public int bitofs[][] = new int[8][2]; // bitofs[numclusters][2]	
	}
	
	// each area has a list of portals that lead into other areas
	// when portals are closed, other areas may not be visible or
	// hearable even if the vis info says that it should be
	
	public static class dareaportal_t {

		public dareaportal_t() {
		}

		public dareaportal_t(ByteBuffer bb) {
			bb.order(ByteOrder.LITTLE_ENDIAN);
			portalnum = bb.getInt();
			otherarea = bb.getInt();
		}

		int portalnum;
		int otherarea;

		public static final int SIZE = 8;
	}

	public static class darea_t {

		public darea_t(ByteBuffer bb) {

			bb.order(ByteOrder.LITTLE_ENDIAN);

			numareaportals = bb.getInt();
			firstareaportal = bb.getInt();

		}
		final int numareaportals;
		final int firstareaportal;

		public static final int SIZE = 8;
	}

}
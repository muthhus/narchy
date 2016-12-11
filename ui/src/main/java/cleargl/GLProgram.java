package cleargl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLException;

import java.io.IOException;
import java.util.HashMap;

public class GLProgram implements GLInterface, GLCloseable {
	private GL mGL;
	private int mProgramId;
	private GLShader mVertexShader;
	private GLShader mFragmentShader;
	private HashMap<GLShaderType, GLShader> mShaders = new HashMap<>();
	private HashMap<String, String> parameters = new HashMap<>();
	private HashMap<String, GLUniform> uniforms = new HashMap<>();

	private boolean stale = false;

	public static GLProgram buildProgram(final GL pGL,
			final Class<?> pClass,
			final String pVertexShaderRessourcePath,
			final String pFragmentShaderRessourcePath) throws IOException {
		final GLShader lVertexShader = new GLShader(pGL,
				pClass,
				pVertexShaderRessourcePath,
				GLShaderType.VertexShader);
		System.out.println(lVertexShader.getShaderInfoLog());

		final GLShader lFragmentShader = new GLShader(pGL,
				pClass,
				pFragmentShaderRessourcePath,
				GLShaderType.FragmentShader);
		System.out.println(lFragmentShader.getShaderInfoLog());
		final GLProgram lGLProgram = new GLProgram(lVertexShader,
				lFragmentShader);

		return lGLProgram;
	}

	@Deprecated
	public static GLProgram buildProgram(final GL pGL,
			final String pVertexShaderSourceAsString,
			final String pFragmentShaderSourceAsString) throws IOException {
		final GLShader lVertexShader = new GLShader(pGL,
				pVertexShaderSourceAsString,
				GLShaderType.VertexShader);
		System.out.println(lVertexShader.getShaderInfoLog());

		final GLShader lFragmentShader = new GLShader(pGL,
				pFragmentShaderSourceAsString,
				GLShaderType.FragmentShader);
		System.out.println(lFragmentShader.getShaderInfoLog());
		final GLProgram lGLProgram = new GLProgram(lVertexShader,
				lFragmentShader);

		System.out.println(lGLProgram.getProgramInfoLog());
		return lGLProgram;
	}

	public static GLProgram buildProgram(final GL pGL,
			final Class<?> pClass,
			final String[] shaders) throws IOException {
		final GLProgram lGLProgram = new GLProgram(pGL,
				shaderPipelineFromFilenames(pGL,
						pClass,
						shaders));

		lGLProgram.printProgramInfoLog();

		return lGLProgram;
	}

	public static GLProgram buildProgram(final GL pGL,
			final Class<?> pClass,
			final String[] shaders,
			final HashMap<String, String> parameters) throws IOException {
		final GLProgram lGLProgram = new GLProgram(pGL,
				shaderPipelineFromFilenames(pGL,
						pClass,
						shaders, parameters),
				parameters);

		lGLProgram.printProgramInfoLog();

		return lGLProgram;
	}

	public void recompileProgram(final GL pGL) {

		final long start = System.nanoTime();
		pGL.getGL3().glDeleteProgram(mProgramId);

		mProgramId = mGL.getGL3().glCreateProgram();

		for (final GLShader shader : mShaders.values()) {
			shader.setParameters(parameters);
			shader.recompile(pGL);
			mGL.getGL3().glAttachShader(mProgramId, shader.getId());
		}

		mGL.getGL3().glLinkProgram(mProgramId);
		final long diff = System.nanoTime() - start;
		stale = false;
	}

	public void addParameter(final String name, final String value) {
		parameters.put(name, value);
		stale = true;
	}

	public void removeParameter(final String name) {
		parameters.remove(name);
		stale = true;
	}

	public void updateParameter(final String name, final String value) {
		parameters.replace(name, value);
		stale = true;
	}

	public boolean isStale() {
		return stale;
	}

	public void setStale(final boolean stale) {
		this.stale = stale;
	}

	private static String shaderFileForType(final GLShaderType type,
			final String[] shaders) {
		final HashMap<GLShaderType, String> glslFilenameMapping = new HashMap<>();

		glslFilenameMapping.put(GLShaderType.VertexShader, ".vert");
		glslFilenameMapping.put(GLShaderType.GeometryShader, ".geom");
		glslFilenameMapping.put(GLShaderType.TesselationControlShader, ".tesc");
		glslFilenameMapping.put(GLShaderType.TesselationEvaluationShader, ".tese");
		glslFilenameMapping.put(GLShaderType.FragmentShader, ".frag");
		glslFilenameMapping.put(GLShaderType.ComputeShader, ".comp");

		for (int i = 0; i < shaders.length; i++) {
			if (shaders[i].endsWith(glslFilenameMapping.get(type))) {
				return shaders[i];
			}
		}

		return null;
	}

	private static HashMap<GLShaderType, GLShader> shaderPipelineFromFilenames(final GL pGL,
			final Class<?> rootClass,
			final String[] shaders) throws IOException {
		final HashMap<GLShaderType, GLShader> pipeline = new HashMap<>();

		for (final GLShaderType type : GLShaderType.values()) {
			final String filename = shaderFileForType(type, shaders);
			if (filename != null) {
				final GLShader shader = new GLShader(pGL,
						rootClass,
						filename,
						type);
				// System.out.println(shader.getShaderInfoLog());
				pipeline.put(type, shader);
			}
		}

		return pipeline;
	}

	private static HashMap<GLShaderType, GLShader> shaderPipelineFromFilenames(final GL pGL,
			final Class<?> rootClass,
			final String[] shaders,
			final HashMap<String, String> params) throws IOException {
		final HashMap<GLShaderType, GLShader> pipeline = new HashMap<>();

		for (final GLShaderType type : GLShaderType.values()) {
			final String filename = shaderFileForType(type, shaders);
			if (filename != null) {
				final GLShader shader = new GLShader(pGL,
						rootClass,
						filename,
						type,
						params);

				System.out.println(shader.getShaderInfoLog());
				pipeline.put(type, shader);
			}
		}

		return pipeline;
	}

	public GLProgram(final GLShader pVerteShader, final GLShader pFragmentShader) {
		super();
		mVertexShader = pVerteShader;
		mFragmentShader = pFragmentShader;

		mShaders.put(GLShaderType.VertexShader, pVerteShader);
		mShaders.put(GLShaderType.FragmentShader, pFragmentShader);

		mGL = pVerteShader.getGL();

		final int lVertexShaderId = mVertexShader.getId();
		final int lFragmentShaderId = mFragmentShader.getId();

		mProgramId = mGL.getGL3().glCreateProgram();
		mGL.getGL3().glAttachShader(mProgramId, lVertexShaderId);
		mGL.getGL3().glAttachShader(mProgramId, lFragmentShaderId);
		mGL.getGL3().glLinkProgram(mProgramId);

		mGL.getGL3().glBindFragDataLocation(mProgramId, 0, "outColor");
	}

	public GLProgram(final GL pGL, final HashMap<GLShaderType, GLShader> pipeline) {
		super();

		mGL = pGL;

		mProgramId = mGL.getGL3().glCreateProgram();
		mShaders = pipeline;

		for (final GLShader shader : pipeline.values()) {
			mGL.getGL3().glAttachShader(mProgramId, shader.getId());
		}

		mGL.getGL3().glLinkProgram(mProgramId);
	}

	public GLProgram(final GL pGL, final HashMap<GLShaderType, GLShader> pipeline,
			final HashMap<String, String> parameters) {
		super();

		mGL = pGL;
		this.parameters = parameters;

		mProgramId = mGL.getGL3().glCreateProgram();
		mShaders = pipeline;

		for (final GLShader shader : pipeline.values()) {
			mGL.getGL3().glAttachShader(mProgramId, shader.getId());
		}

		mGL.getGL3().glLinkProgram(mProgramId);
	}

	@Override
	public void close() throws GLException {
		mGL.getGL3().glDeleteProgram(mProgramId);
	}

	public GLAttribute getAttribute(final String pAttributeName) {
		final int lAttributeId = mGL.getGL3()
				.glGetAttribLocation(mProgramId,
						pAttributeName);
		final GLAttribute lGLAttribute = new GLAttribute(this,
				lAttributeId);
		return lGLAttribute;
	}

	public GLUniform getUniform(String pUniformName) {
		if (uniforms.containsKey(pUniformName) && !isStale()) {
			return uniforms.get(pUniformName);
		} else {
			final int lUniformId = mGL.getGL3().glGetUniformLocation(mProgramId, pUniformName);
			final GLUniform lGLUniform = new GLUniform(this, lUniformId);

			uniforms.put(pUniformName, lGLUniform);
			return lGLUniform;
		}
	}

	public void bind() {
		if (stale) {
			recompileProgram(mGL);
		}

		mGL.getGL3().glUseProgram(mProgramId);
	}

	public void unbind() {
		mGL.getGL3().glUseProgram(0);
	}

	public void use(final GL pGL) {
		mGL = pGL;
		bind();
	}

	public void printProgramInfoLog() {
		final String log = getProgramInfoLog();

		if (log.length() >= 1) {
			System.err.println(log);
			for (final GLShader s : this.getShaderPipeline().values()) {
				System.err.println(s.getShaderInfoLog());
			}
		}
	}

	public String getProgramInfoLog() {
		final int status[] = new int[1];
		mGL.getGL3().glGetProgramiv(mProgramId, GL3.GL_LINK_STATUS, status, 0);
		final int lLogLength = getProgramParameter(GL2ES2.GL_INFO_LOG_LENGTH);

		if (status[0] == GL3.GL_TRUE)
			return "";

		final int[] lLength = new int[1];
		final byte[] lBytes = new byte[lLogLength + 1];
		mGL.getGL3().glGetProgramInfoLog(mProgramId,
				lLogLength,
				lLength,
				0,
				lBytes,
				0);
		final String logMessage = toString() + "\n" + new String(lBytes);

		return logMessage;
	}

	public int getProgramParameter(final int pParameterName) {
		final int lParameter[] = new int[1];
		mGL.getGL3().glGetProgramiv(mProgramId,
				pParameterName,
				lParameter,
				0);
		return lParameter[0];
	}

	public void setGL(final GL pGL) {
		mGL = pGL;
	}

	public HashMap<GLShaderType, GLShader> getShaderPipeline() {
		return mShaders;
	}

	@Override
	public GL getGL() {
		return mGL;
	}

	@Override
	public int getId() {
		return mProgramId;
	}

	@Override
	public String toString() {
		return "GLProgram [mGL=" + mGL
				+ ", mProgramId="
				+ mProgramId
				+ ", mVertexShader="
				+ mVertexShader
				+ ", mFragmentShader="
				+ mFragmentShader
				+ "]";
	}

}

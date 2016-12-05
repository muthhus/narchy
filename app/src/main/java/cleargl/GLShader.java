package cleargl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.Collectors;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLException;

public class GLShader implements GLInterface, GLCloseable {
	private final GL mGL;
	private int mShaderId;
	private final GLShaderType mShaderType;
	private final String mShaderSource;
	private final String mShaderSourcePath;
	private String mShaderBasePath;
	private final Class<?> mShaderSourceRootClass;
	private HashMap<String, String> mParameters;

	static final HashMap<GLShaderType, Integer> glShaderTypeMapping;

	static {
		glShaderTypeMapping = new HashMap<>();
		glShaderTypeMapping.put(GLShaderType.VertexShader,
				GL2ES2.GL_VERTEX_SHADER);
		glShaderTypeMapping.put(GLShaderType.GeometryShader,
				GL3.GL_GEOMETRY_SHADER);
		glShaderTypeMapping.put(GLShaderType.TesselationControlShader,
				GL3.GL_TESS_CONTROL_SHADER);
		glShaderTypeMapping.put(GLShaderType.TesselationEvaluationShader,
				GL3.GL_TESS_EVALUATION_SHADER);
		glShaderTypeMapping.put(GLShaderType.FragmentShader,
				GL2ES2.GL_FRAGMENT_SHADER);
	}

	public GLShader(final GL pGL,
			final Class<?> pRootClass,
			final String pResourceName,
			final GLShaderType pShaderType) throws IOException {
		super();
		mGL = pGL;
		final InputStream lResourceAsStream = pRootClass.getResourceAsStream(pResourceName);
		mShaderSource = new Scanner(lResourceAsStream, "UTF-8").useDelimiter("\\A").next();
		mShaderType = pShaderType;
		mShaderSourcePath = pResourceName;
		mShaderSourceRootClass = pRootClass;
		mParameters = new HashMap<>();
		mShaderBasePath = pRootClass.getResource(pResourceName).getPath().substring(0,
				pRootClass.getResource(pResourceName).getPath().lastIndexOf("/"));

		// preprocess shader
		final String shaderSourceProcessed = preprocessShader(mShaderSource);

		mShaderId = pGL.getGL3().glCreateShader(glShaderTypeMapping.get(pShaderType));
		mGL.getGL3().glShaderSource(mShaderId, 1, new String[]{shaderSourceProcessed}, null);
		mGL.getGL3().glCompileShader(mShaderId);

	}

	public GLShader(final GL pGL,
			final Class<?> pRootClass,
			final String pResourceName,
			final GLShaderType pShaderType,
			final HashMap<String, String> params) throws IOException {
		super();
		mGL = pGL;
		final InputStream lResourceAsStream = pRootClass.getResourceAsStream(pResourceName);
		mShaderSource = new Scanner(lResourceAsStream, "UTF-8").useDelimiter("\\A").next();
		mShaderType = pShaderType;
		mShaderSourcePath = pResourceName;
		mShaderSourceRootClass = pRootClass;
		mParameters = params;
		mShaderBasePath = pRootClass.getResource(pResourceName).getPath().substring(0,
				pRootClass.getResource(pResourceName).getPath().lastIndexOf(File.separator));

		// preprocess shader
		final String shaderSourceProcessed = preprocessShader(mShaderSource);

		mShaderId = pGL.getGL3().glCreateShader(glShaderTypeMapping.get(pShaderType));
		mGL.getGL3().glShaderSource(mShaderId, 1, new String[]{shaderSourceProcessed}, null);
		mGL.getGL3().glCompileShader(mShaderId);

	}

	public GLShader(final GL pGL,
			final String pShaderSourceAsString,
			final GLShaderType pShaderType) throws IOException {
		super();
		mGL = pGL;
		mShaderSource = pShaderSourceAsString;
		mShaderType = pShaderType;
		mShaderSourceRootClass = null;
		mShaderSourcePath = null;
		mParameters = new HashMap<>();
		mShaderBasePath = "";

		// preprocess shader
		final String shaderSourceProcessed = preprocessShader(mShaderSource);

		mShaderId = pGL.getGL3().glCreateShader(glShaderTypeMapping.get(pShaderType));
		mGL.getGL3().glShaderSource(mShaderId, 1, new String[]{shaderSourceProcessed}, null);
		mGL.getGL3().glCompileShader(mShaderId);

	}

	@Override
	public void close() throws GLException {
		mGL.getGL3().glDeleteShader(mShaderId);
	}

	public void setShaderBasePath(final String path) {
		mShaderBasePath = path;
	}

	public void recompile(final GL pGL) {
		close();

		// preprocess shader
		final String shaderSourceProcessed = preprocessShader(mShaderSource);

		mShaderId = pGL.getGL3().glCreateShader(glShaderTypeMapping.get(mShaderType));
		mGL.getGL3().glShaderSource(mShaderId, 1, new String[]{shaderSourceProcessed}, null);
		mGL.getGL3().glCompileShader(mShaderId);
	}

	public void setParameters(final HashMap<String, String> params) {
		mParameters = params;
	}

	public String preprocessShader(final String source) {
		String effectiveSource = source;
		int startPos = 0;
		int endPos = 0;

		// replace variables
		while ((startPos = effectiveSource.indexOf("%var(")) != -1) {
			endPos = effectiveSource.indexOf(")", startPos);
			final String varName = effectiveSource.substring(startPos + "%var(".length(), endPos);
			if (!mParameters.containsKey(varName)) {
				System.err.println("Warning: Variable '" + varName + "' does not exist in shader parameters!");
			}
			final String varContents = mParameters.getOrDefault(varName, "");

			effectiveSource = effectiveSource.substring(0, startPos) + varContents
					+ effectiveSource.substring(endPos + ")".length());
		}

		// find includes
		startPos = 0;
		endPos = 0;
		while ((startPos = effectiveSource.indexOf("%include <")) != -1) {
			endPos = effectiveSource.indexOf(">", startPos);
			final String includeFileName = effectiveSource.substring(startPos + "%include <".length(), endPos);
			String includeSource = "";

			try {
				includeSource = Files.lines(Paths.get(mShaderBasePath + File.separator + includeFileName))
						.parallel()
						.filter(line -> !line.startsWith("//"))
						.map(String::trim)
						.collect(Collectors.joining());
			} catch (final IOException e) {
				e.printStackTrace();
			}

			effectiveSource = effectiveSource.substring(0, startPos) + "\n// included from " + includeFileName + "\n"
					+ includeSource + "\n// end include\n" + effectiveSource.substring(endPos + ">".length());
		}

		return effectiveSource;
	}

	public String getShaderInfoLog() {
		final int logLen = getShaderParameter(GL2ES2.GL_INFO_LOG_LENGTH);
		if (logLen <= 0)
			return "";

		final int[] lLength = new int[1];
		final byte[] lBytes = new byte[logLen + 1];
		mGL.getGL3().glGetShaderInfoLog(mShaderId,
				logLen,
				lLength,
				0,
				lBytes,
				0);
		final String logMessage = toString() + ":\n" + new String(lBytes);
		return logMessage;
	}

	public int getShaderParameter(final int pParamName) {
		final int lParameter[] = new int[1];
		mGL.getGL3().glGetShaderiv(mShaderId, pParamName, lParameter, 0);
		return lParameter[0];
	}

	public String getSourcePath() {
		return mShaderSourcePath;
	}

	public Class<?> getShaderSourceRootClass() {
		return mShaderSourceRootClass;
	}

	@Override
	public int getId() {
		return mShaderId;
	}

	@Override
	public GL getGL() {
		return mGL;
	}

	@Override
	public String toString() {
		return "GLShader [mGL=" + mGL
				+ ", mShaderId="
				+ mShaderId
				+ ", mShaderType="
				+ mShaderType
				+ ", mShaderSource="
				+ mShaderSource
				+ "]";
	}

}

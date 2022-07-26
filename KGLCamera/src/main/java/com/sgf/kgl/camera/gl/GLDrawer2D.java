package com.sgf.kgl.camera.gl;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.IntRange;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
public class GLDrawer2D {
	private static final boolean DEBUG = false; // TODO set false on release
	private static final String TAG = "GLDrawer2D";

	private static final String vss
			= "uniform mat4 uMVPMatrix;\n"
			+ "uniform mat4 uTexMatrix;\n"
			+ "attribute highp vec4 aPosition;\n"
			+ "attribute highp vec4 aTextureCoord;\n"
			+ "varying highp vec2 vTextureCoord;\n"
			+ "\n"
			+ "void main() {\n"
			+ "	gl_Position = uMVPMatrix * aPosition;\n"
			+ "	vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
			+ "}\n";
	private static final String fss
			= "#extension GL_OES_EGL_image_external : require\n"
			+ "precision mediump float;\n"
			+ "uniform samplerExternalOES sTexture;\n"
			+ "varying highp vec2 vTextureCoord;\n"
			+ "uniform int mirrorType;"
			+ "void main() {\n"
			+ " vec4 rgba = texture2D(sTexture, vTextureCoord);\n"
			+ "	if(mirrorType == 1) {\n"
			+ "  	rgba = texture2D(sTexture,vec2(1.0-vTextureCoord.x,vTextureCoord.y));\n"
			+ "  }\n"
			+ "	else if(mirrorType == 2) {\n"
			+ "  	rgba = texture2D(sTexture,vec2(vTextureCoord.x,1.0-vTextureCoord.y));\n"
			+ "  }\n"
			+ "  gl_FragColor = rgba;\n"
			+ "}";


	/**
	 * a(-1,+1)              c(+1,+1)
	 *
	 * -----------------------------
	 *
	 *
	 * b(-1,-1)              d(+1,-1)
	 */
	private static final float[] VERTICES = {
			-1.0f, +1.0f,
			-1.0f, -1.0f,
			+1.0f, +1.0f,
			+1.0f, -1.0f,
	};

	/**
	 * a(0,1)              c(1,1)
	 *
	 *
	 *
	 * b(0,0)              d(1,0)
	 * ---------------------------
	 * 0  -> a, b, c, d
	 * 90 -> c, a, d, b
	 * 180-> d, c, b, a
	 * 270-> b, d, a, c
	 */
	//0
	private static final float[] TEXCOORD_0 = {
			0.0f, 1.0f,
			0.0f, 0.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,
	};

	//90
	private static final float[] TEXCOORD_90 = {
			1.0f, 1.0f,
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 0.0f,
	};

	//180
	private static final float[] TEXCOORD_180 = {
			1.0f, 0.0f,
			1.0f, 1.0f,
			0.0f, 0.0f,
			0.0f, 1.0f,
	};

	//270
	private static final float[] TEXCOORD_270 = {
			0.0f, 0.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
	};

	private static float[] TEXCOORD = TEXCOORD_0;


	private final FloatBuffer pVertex;
	private final FloatBuffer pTexCoord;
	private int hProgram;
	private int maPositionLoc;
	private int maTextureCoordLoc;
	private int muMVPMatrixLoc;
	private int muTexMatrixLoc;
	private int mirrorTypePtr;
	private final float[] mMvpMatrix = new float[16];

	private static final int FLOAT_SZ = Float.SIZE / 8;
	private static final int VERTEX_NUM = 4;
	private static final int VERTEX_SZ = VERTEX_NUM * 2;


	private static final int MIRROR_NONE = 0;
	private static final int MIRROR_LANDSCAPE = 1;
	private static final int MIRROR_PORTRAIT = 2;
	private static volatile int mMirrorType = MIRROR_NONE;
	private static volatile boolean isMirror = false;
	private static volatile int mRotation = 0;


	public GLDrawer2D() {
		this(mRotation);
	}

	/**
	 * Constructor
	 * this should be called in GL context
	 */
	public GLDrawer2D(int rotation) {
		mRotation = rotation;
		initTexCoord(rotation);
		pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pVertex.put(VERTICES);
		pVertex.flip();
		pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoord.put(TEXCOORD);
		pTexCoord.flip();

		hProgram = loadShader(vss, fss);
		GLES20.glUseProgram(hProgram);
		maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
		muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
		muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");
		mirrorTypePtr = GLES20.glGetUniformLocation(hProgram, "mirrorType");

		Matrix.setIdentityM(mMvpMatrix, 0);
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
		GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
	}

	private void initTexCoord(int rotation) {
		switch (rotation) {
			case 0:
				TEXCOORD = TEXCOORD_0;
				mMirrorType = MIRROR_PORTRAIT;
				break;
			case 90:
				TEXCOORD = TEXCOORD_90;
				mMirrorType = MIRROR_LANDSCAPE;
				break;
			case 180:
				TEXCOORD = TEXCOORD_180;
				mMirrorType = MIRROR_PORTRAIT;
				break;
			case 270:
				TEXCOORD = TEXCOORD_270;
				mMirrorType = MIRROR_LANDSCAPE;
				break;
		}
	}


	public void setMirror(boolean isMirror) {
		GLDrawer2D.isMirror = isMirror;
	}

	/**
	 * terminatinng, this should be called in GL context
	 */
	public void release() {
		if (hProgram >= 0)
			GLES20.glDeleteProgram(hProgram);
		hProgram = -1;
	}

	/**
	 * draw specific texture with specific texture matrix
	 *
	 * @param tex_id     texture ID
	 * @param tex_matrix texture matrix、if this is null, the last one use(we don't check size of this array and needs at least 16 of float)
	 */
	public void draw(final int tex_id, final float[] tex_matrix) {
		GLES20.glUseProgram(hProgram);
		if (GLDrawer2D.isMirror) {
			GLES20.glUniform1i(mirrorTypePtr, mMirrorType);
		} else {
			GLES20.glUniform1i(mirrorTypePtr, MIRROR_NONE);
		}
		if (tex_matrix != null)
			GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, tex_matrix, 0);
		GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex_id);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
		GLES20.glUseProgram(0);
	}

	public void setMatrix(final float[] matrix, final int offset) {
		if ((matrix != null) && (matrix.length >= offset + 16)) {
			System.arraycopy(matrix, offset, mMvpMatrix, 0, 16);
		} else {
			Matrix.setIdentityM(mMvpMatrix, 0);
		}
	}

	/**
	 * create external texture
	 *
	 * @return texture ID
	 */
	public static int initTex() {
		if (DEBUG) Log.v(TAG, "initTex:");
		final int[] tex = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		return tex[0];
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final int hTex) {
		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[]{hTex};
		GLES20.glDeleteTextures(1, tex, 0);
	}

	/**
	 * load, compile and link shader
	 *
	 * @param vss source of vertex shader
	 * @param fss source of fragment shader
	 * @return status
	 */
	public static int loadShader(final String vss, final String fss) {
		if (DEBUG) Log.v(TAG, "loadShader:");
		int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		GLES20.glShaderSource(vs, vss);
		GLES20.glCompileShader(vs);
		final int[] compiled = new int[1];
		GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			if (DEBUG) Log.e(TAG, "Failed to compile vertex shader:"
					+ GLES20.glGetShaderInfoLog(vs));
			GLES20.glDeleteShader(vs);
			vs = 0;
		}

		int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		GLES20.glShaderSource(fs, fss);
		GLES20.glCompileShader(fs);
		GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			if (DEBUG) Log.w(TAG, "Failed to compile fragment shader:"
					+ GLES20.glGetShaderInfoLog(fs));
			GLES20.glDeleteShader(fs);
			fs = 0;
		}

		final int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vs);
		GLES20.glAttachShader(program, fs);
		GLES20.glLinkProgram(program);

		return program;
	}
}

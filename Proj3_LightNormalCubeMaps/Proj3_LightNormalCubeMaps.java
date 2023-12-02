package Proj3_LightNormalCubeMaps;
import java.io.*;
import java.nio.*;
import java.lang.Math;
import javax.swing.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_CCW;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.texture.*;

import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Proj3_LightNormalCubeMaps extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram, renderingProgramCubeMap;
	private int vao[] = new int[1];
	private int vbo[] = new int[14];
	private float cameraX, cameraY, cameraZ; // Position of camera
	private float targetX, targetY, targetZ; // Where camera is looking at
	private Vector3f camVec, targetVec, f, r, u; // Vectors that define camera position, target, f, r and u
	private float Ddist = 1.0f, Dangle = 0.1f; // Distance and angle increments
	private int skyboxTexture;
	private int shuttleTexture;
	private int sphereTexture;
	private int pyramidTexture;
	private int numObjVertices;
	private boolean spaceBtn = false;
	
	private ImportedModel myModel;
	private Sphere mySphere;
	private int numSphereVerts;
	
	private Vector3f initialLightLoc = new Vector3f(0.0f, 1.0f, 0.0f);
	private Vector3f shuttleLoc = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f sphereLoc = new Vector3f(5.0f, 0.0f, 0.0f);
	private Vector3f pyramidLoc = new Vector3f(-2.0f, 0.0f, 3.0f);
	private Vector3f cameraLoc = new Vector3f(0.0f, 0.0f, 5.0f);
	
	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int vLoc, mvLoc, projLoc, nLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];

	// white light properties
	float[] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	float[] lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	float[] matAmb = Utils.goldAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();
	
	public Proj3_LightNormalCubeMaps() 
	{	setTitle("Project 3");
		setSize(800, 800);
		//Making sure we get a GL4 context for the canvas
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capabilities = new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
 		//end GL4 context
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this); // Listen for keystrokes
		this.add(myCanvas);
		this.setVisible(true);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public void display(GLAutoDrawable drawable) 
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		gl.glUseProgram(renderingProgram);
		//int ambientColorLoc = gl.glGetUniformLocation(renderingProgram, "ambientColor");
		//gl.glUniform3f(ambientColorLoc, 1.0f, 1.0f, 1.0f);
		
		vMat.identity();
		vMat.setTranslation(-cameraLoc.x(), -cameraLoc.y(), -cameraLoc.z);
		
		vMat.identity();
		vMat.lookAt(camVec, targetVec, new Vector3f(0.0f,1.0f,0.0f));
		
		// draw cube map
		
		gl.glUseProgram(renderingProgramCubeMap);
		
		vLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "v_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		
		projLoc = gl.glGetUniformLocation(renderingProgramCubeMap, "proj_matrix");
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
		
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
		
		// draw scene
		
		gl.glUseProgram(renderingProgram);
		
		// Set light properties
		//int lightPositionLoc = gl.glGetUniformLocation(renderingProgram, "lightPosition");
		//gl.glUniform3f(lightPositionLoc, 6.0f, 1.0f, 0.0f); 

		//int lightColorLoc = gl.glGetUniformLocation(renderingProgram, "lightColor");
		//gl.glUniform3f(lightColorLoc, 1.0f, 1.0f, 1.0f);

		// Set shininess
		//int shininessLoc = gl.glGetUniformLocation(renderingProgram, "shininess");
		//gl.glUniform1f(shininessLoc, 0.0f);
		
		//int objectColorLoc = gl.glGetUniformLocation(renderingProgram, "objectColor");
		//gl.glUniform3f(objectColorLoc, 1.0f, 1.0f, 1.0f);
		
		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		projLoc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");

		mMat.identity();
		mMat.translate(shuttleLoc.x(), shuttleLoc.y(), shuttleLoc.z());
		mMat.rotateX((float)Math.toRadians(20.0f));
		mMat.rotateY((float)Math.toRadians(130.0f));
		mMat.rotateZ((float)Math.toRadians(5.0f));
		
		currentLightPos.set(initialLightLoc);
		
		if (spaceBtn == true) {
			installLights(vMat);
		}
		
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shuttleTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
		
		// draw sphere
		
		mMat.translation(sphereLoc.x(), sphereLoc.y(), sphereLoc.z());
		
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(4);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(5);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, sphereTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
		
		// draw pyramid
		
		mMat.translation(pyramidLoc.x(), pyramidLoc.y(), pyramidLoc.z());
		
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(7);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(8);
		
		gl.glEnable(GL_DEPTH_TEST);
		gl.glFrontFace(GL_CCW);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 18);
		
		//gl.glUseProgram(renderingProgram);
		//gl.glUniform3f(objectColorLoc, 1.0f, 1.0f, 1.0f);
	}
	
	public void init(GLAutoDrawable drawable) 
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		myModel = new ImportedModel("Proj3_LightNormalCubeMaps_data/shuttle.obj");
		renderingProgram = Utils.createShaderProgram("Proj3_LightNormalCubeMaps_data/vertPhongShader.glsl", "Proj3_LightNormalCubeMaps_data/fragPhongShader.glsl");
		renderingProgramCubeMap = Utils.createShaderProgram("Proj3_LightNormalCubeMaps_data/vertCShader.glsl", "Proj3_LightNormalCubeMaps_data/fragCShader.glsl");

		aspect = (float) myCanvas.getWidth()/ (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float)Math.toRadians(90.0f), aspect, 0.1f, 1000.0f);
		
		setupVertices();
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
		targetX = 0.0f; targetY = 0.0f; targetZ = 0.0f;
		camVec = new Vector3f(cameraX, cameraY, cameraZ);
		targetVec = new Vector3f(targetX, targetY, targetZ);
		
		skyboxTexture = Utils.loadCubeMap("Proj3_LightNormalCubeMaps_data");
		shuttleTexture = Utils.loadTexture("Proj3_LightNormalCubeMaps_data/spstob_1.jpg");
		sphereTexture = Utils.loadTexture("Proj3_LightNormalCubeMaps_data/earth.jpg");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
	}
	
	private void installLights(Matrix4f vMatrix)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		currentLightPos.mulPosition(vMatrix);
		lightPos[0]=currentLightPos.x(); lightPos[1]=currentLightPos.y(); lightPos[2]=currentLightPos.z();
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}
	
	private void setupVertices() 
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		numObjVertices = myModel.getNumVertices();
		Vector3f[] vertices = myModel.getVertices();
		Vector2f[] texCoords = myModel.getTexCoords();
		Vector3f[] normals = myModel.getNormals();
	
		float[] pvalues = new float[numObjVertices*3];
		float[] tvalues = new float[numObjVertices*2];
		float[] nvalues = new float[numObjVertices*3];
	
		for (int i = 0; i < numObjVertices; i++)
		{	pvalues[i*3]   = (float) (vertices[i]).x();
			pvalues[i*3+1] = (float) (vertices[i]).y();
			pvalues[i*3+2] = (float) (vertices[i]).z();
			tvalues[i*2]   = (float) (texCoords[i]).x();
			tvalues[i*2+1] = (float) (texCoords[i]).y();
			nvalues[i*3]   = (float) (normals[i]).x();
			nvalues[i*3+1] = (float) (normals[i]).y();
			nvalues[i*3+2] = (float) (normals[i]).z();
		}
		
		mySphere = new Sphere(24);
		numSphereVerts = mySphere.getIndices().length;
		
		int[] indices = mySphere.getIndices();
		Vector3f[] vert = mySphere.getVertices();
		Vector2f[] tex = mySphere.getTexCoords();
		Vector3f[] norm = mySphere.getNormals();
		
		float[] pSvalues = new float[indices.length*3]; //vertex positions
		float[] tSvalues = new float[indices.length*2]; //texture coordinates
		float[] nSvalues = new float[indices.length*3]; //normal vectors
		
		for (int i = 0; i < indices.length; i++) 
		{	pSvalues[i * 3] = (float) (vert[indices[i]]).x;
			pSvalues[i * 3 + 1] = (float) (vert[indices[i]]).y;
			pSvalues[i * 3 + 2] = (float) (vert[indices[i]]).z;
			
			tSvalues[i * 2] = (float) (tex[indices[i]]).x;
			tSvalues[i * 2 + 1] = (float) (tex[indices[i]]).y;
			
			nSvalues[i * 3] = (float) (norm[indices[i]]).x;
			nSvalues[i * 3 + 1] = (float) (norm[indices[i]]).y;
			nSvalues[i * 3 + 2] = (float) (norm[indices[i]]).z;
		}
		
		// cube vertices
		float[] cubeVertexPositions =
		{	-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};
		
		// pyramid vertices
		float[] pyramidPositions =
		{	-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f,    //front
			1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f,    //right
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f,  //back
			-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f,  //left
			-1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, //LF
			1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f  //RR
		};
		
		float[] pyrTextureCoordinates =
		{	0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
		};
		
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cvertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cvertBuf.limit()*4, cvertBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer sVertBuf = Buffers.newDirectFloatBuffer(pSvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sVertBuf.limit()*4, sVertBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer sTexBuf = Buffers.newDirectFloatBuffer(tSvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sTexBuf.limit()*4, sTexBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer sNorBuf = Buffers.newDirectFloatBuffer(nSvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sNorBuf.limit()*4, sNorBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer pyrVertBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrVertBuf.limit()*4, pyrVertBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer pyrTexBuf = Buffers.newDirectFloatBuffer(pyrTextureCoordinates);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrTexBuf.limit()*4, pyrTexBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer pyrNorBuf = Buffers.newDirectFloatBuffer(pyrTextureCoordinates);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrNorBuf.limit()*4, pyrNorBuf, GL_STATIC_DRAW);
	}
	
	public static void main(String[] args) { new Proj3_LightNormalCubeMaps(); }
	public void dispose(GLAutoDrawable drawable) {}
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(90.0f), aspect, 0.1f, 1000.0f);
	}

	public void keyTyped(KeyEvent e) 
	{
		System.out.println("keyTyped event: " + e);
	}

	public void keyPressed(KeyEvent e) 
	{
		System.out.println("keyPressed event: "+e);
		// Calculate camera forward, left and up vector
		Vector3f f = new Vector3f();
		camVec.sub(targetVec, f);
		f.normalize();
		Vector3f r = new Vector3f();
		f.negate(r);
		r.cross(0, 1, 0);
		Vector3f u = new Vector3f();
		f.cross(r, u);
		System.out.println(e);
		
		if (e.getKeyChar()=='w')
		{ // Move camera forward by Ddist
			camVec.sub(f.mul(Ddist));
			myCanvas.display();
		}		
		if (e.getKeyChar()=='s')
		{ // Move camera backwards by Ddist
			camVec.add(f.mul(Ddist));
			System.out.println(e.getKeyChar() + "" + camVec);
			myCanvas.display();
		}		
		if (e.getKeyChar()=='d')
		{ // Right
			camVec.add(r.mul(Ddist));
			myCanvas.display();
		}		
		if (e.getKeyChar()=='a')
		{ // Left
			camVec.sub(r.mul(Ddist));
			myCanvas.display();
		}		
		if (e.getKeyCode()==38)
		{ // Up
			camVec.add(u.mul(Ddist));
			myCanvas.display();
		}		
		if (e.getKeyCode()==40)
		{ // Down
			camVec.sub(u.mul(Ddist));
			myCanvas.display();
		}
		if (e.getKeyCode() == 37)
		{   //camVec.rotateZYX(Dangle, 0.0f, 0.0f);
			myCanvas.display();
		}
		if (e.getKeyCode() == 39)
		{	//camVec.rotateZYX(-Dangle, 0.0f, 0.0f);
			myCanvas.display();
		}
		if (e.getKeyCode() == 38)
		{	//camVec.rotateZYX(0.0f, 0.0f, Dangle);
			myCanvas.display();
		}
		if (e.getKeyCode() == 40)
		{	//camVec.rotateX(0.0f, 0.0f, -Dangle);
			myCanvas.display();
		}
		if (e.getKeyCode() == 32) // Display positional light
		{	spaceBtn = true;
			myCanvas.display();
		}
		
	}

	public void keyReleased(KeyEvent e) 
	{
		System.out.println("keyReleased event: " + e);
		if (e.getKeyCode() == 32) // Destroy positional light
		{	spaceBtn = false;
			myCanvas.display();
		}
	}

	
}

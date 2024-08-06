package gregtech.common.render;

import static gregtech.api.enums.Mods.GregTech;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import li.cil.oc.util.RenderState;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import net.minecraft.util.IIcon;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;

import cpw.mods.fml.client.registry.ClientRegistry;
import gregtech.common.tileentities.render.TileWormhole;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import pers.gwyog.gtneioreplugin.plugin.block.ModBlocks;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GT_WormholeRenderer extends TileEntitySpecialRenderer {


    private static ShaderProgram wormholeProgram;

    private int uTranslateMatrix = -1;
    private int uTexture = -1;
    private int aTextCoord = -1;

    private int eboID = -1;
    private int vboID = -1, uvboID = -1;


    private boolean initialized = false;

    private FloatBuffer bufModelViewProjection = BufferUtils.createFloatBuffer(16);
    private Matrix4fStack modelProjection = new Matrix4fStack(2);




    public GT_WormholeRenderer() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileWormhole.class, this);
    }

    private static final double trimPercentage = .95;
    private static final double corePercentage = trimPercentage / Math.sqrt(3);


    private final float[] vertices = {
        // Positions
        -0.5f, -0.5f,  0.5f,  // Front Face
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,

        /*
        -0.5f, -0.5f, -0.5f,  // Back Face
        -0.5f,  0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,

        -0.5f, -0.5f,  0.5f,  // Front Face
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,

        -0.5f, -0.5f, -0.5f, // Back Face
        -0.5f,  0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,

        -0.5f, -0.5f,  0.5f, // Front Face
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,

        -0.5f, -0.5f, -0.5f, // Back Face
        -0.5f,  0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,

         */

    };

    private float[] uv = {
        // Positions          // Initial UVs
        0.0f, 0.0f, // Front face
        0.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
/*
        0.0f, 0.0f, // Back face
        0.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,

        0.0f, 0.0f, // Front face
        0.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,

        0.0f, 0.0f, // Back face
        0.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,

        0.0f, 0.0f, // Front face
        0.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,

        0.0f, 0.0f, // Back face
        0.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 0.0f

 */

    };

    private final int[] indices = {
        // Front face
        0, 1, 2, 2, 3, 0,
        /*

        // Back face
        4, 5, 6, 6, 7, 4,
        // Left face
        4, 0, 3, 3, 5, 4,
        // Right face
        1, 7, 6, 6, 2, 1,
        // Top face
        3, 2, 6, 6, 5, 3,
        // Bottom face
        4, 7, 1, 1, 0, 4

         */
    };



    private void updateTextures(Block block){
        IIcon texture = block.getIcon(0,0);
        float minu = texture.getMinU();
        float maxu = texture.getMaxU();

        float minv = texture.getMinV();
        float maxv = texture.getMaxV();
        float[] newUVs = {
            minu, minv, // Front face
            minu, maxv,
            maxu, maxv,
            maxu, minv,
        };

        FloatBuffer newUVBuff = BufferUtils.createFloatBuffer(uv.length);
        newUVBuff.put(newUVs).flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvboID);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, newUVBuff);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private boolean init(){
        wormholeProgram = new ShaderProgram(GregTech.resourceDomain,
            "shaders/wormhole.vert.glsl",
            "shaders/wormhole.frag.glsl");

        wormholeProgram.use();

        try{
            uTexture = wormholeProgram.getUniformLocation("u_Texture");
            uTranslateMatrix = wormholeProgram.getUniformLocation("u_TranslationMatrix");
            aTextCoord = wormholeProgram.getAttribLocation("a_TexCoord");
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
            ShaderProgram.clear();
            return false;
        }

        GL20.glUniform1i(uTexture, OpenGlHelper.defaultTexUnit - GL13.GL_TEXTURE0);

        vboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
        FloatBuffer vertBuff = BufferUtils.createFloatBuffer(vertices.length);
        vertBuff.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertBuff, GL15.GL_STATIC_DRAW);

        uvboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvboID);
        FloatBuffer uvBuff = BufferUtils.createFloatBuffer(uv.length);
        uvBuff.put(uv).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuff, GL15.GL_DYNAMIC_DRAW);

        eboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
        IntBuffer indexBuff = BufferUtils.createIntBuffer(indices.length);
        indexBuff.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuff, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        ShaderProgram.clear();
        return true;
    }

    private void renderWormhole(TileEntity tile, double x, double y, double z, float timeSinceLastTick){

        //if (!initialized) {
        if (!initialized) {
            boolean success = init();
            if (success)
                initialized = true;
            else
                return;
        }

        wormholeProgram.use();
        GL11.glDisable(GL11.GL_CULL_FACE);

        updateTextures(ModBlocks.getBlock("Ow"));

        modelProjection.identity();
        modelProjection.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);
        modelProjection.get(0, bufModelViewProjection);
        GL20.glUniformMatrix4(uTranslateMatrix, false, bufModelViewProjection);

        //Vertex VBO bindings
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vboID);
        GL11.glVertexPointer(3,GL11.GL_FLOAT,0,0);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

        //UV VBO bindings
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,uvboID);
        GL20.glVertexAttribPointer(aTextCoord,2, GL11.GL_FLOAT,false,0,0);
        GL20.glEnableVertexAttribArray(aTextCoord);

        //EBO bindings
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,eboID);

        GL11.glDrawElements(GL11.GL_TRIANGLES, indices.length, GL11.GL_UNSIGNED_INT, 0);

        // Unbind the VBOs and EBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL20.glDisableVertexAttribArray(uvboID);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

        GL11.glEnable(GL11.GL_CULL_FACE);

        System.out.println(uvboID);
        ShaderProgram.clear();
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float timeSinceLastTick) {

        if (!(tile instanceof TileWormhole)) return;

        renderWormhole(tile, x, y, z, timeSinceLastTick);

    }
}

package gregtech.common.render;

import static gregtech.api.enums.Mods.GregTech;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
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
import java.util.Arrays;

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


    private boolean init(RenderCubes cubes){
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
        FloatBuffer vertBuff = BufferUtils.createFloatBuffer(cubes.vboSize*3);
        vertBuff.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertBuff, GL15.GL_STATIC_DRAW);

        int uv_count = vertices.length/3*2;
        uvboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvboID);
        FloatBuffer uvBuff = BufferUtils.createFloatBuffer(cubes.vboSize*2);
        //uvBuff.put(uv).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuff, GL15.GL_DYNAMIC_DRAW);

        eboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
        IntBuffer indexBuff = BufferUtils.createIntBuffer(cubes.eboSize);
        indexBuff.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuff, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        ShaderProgram.clear();
        return true;
    }

    private void renderWormhole(TileEntity tile, double x, double y, double z, float timeSinceLastTick){
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        this.bindTexture(TextureMap.locationBlocksTexture);


        RenderCubes render = new RenderCubes(new Cube[]{
            new Cube(1,ModBlocks.getBlock("Ow")),
        });

        //if (!initialized) {
        if (!initialized) {
            boolean success = init(render);
            if (success)
                initialized = true;
            else
                return;
        }

        wormholeProgram.use();
        //GL11.glDisable(GL11.GL_CULL_FACE);

        modelProjection.identity();
        modelProjection.translate((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);
        modelProjection.get(0, bufModelViewProjection);
        GL20.glUniformMatrix4(uTranslateMatrix, false, bufModelViewProjection);


        //Set Texture
        render.setBlockAtIndex(0, ModBlocks.getBlock("Ow"));
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvboID);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, render.getTextCoordBuffer());
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

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


        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL20.glDisableVertexAttribArray(aTextCoord);

        // Unbind the VBOs and EBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        //GL11.glEnable(GL11.GL_CULL_FACE);

        ShaderProgram.clear();
    }



    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float timeSinceLastTick) {

        if (!(tile instanceof TileWormhole)) return;

        renderWormhole(tile, x, y, z, timeSinceLastTick);

    }


    private static final int[] indices = {
        0,  1,  2,  2,  3,  0, // Front face
        4,  5,  6,  6,  7,  4, // Back face
        8,  9,  10, 10, 11, 8, // Left face
        12, 13, 14, 14, 15, 12, // Right face
        16, 17, 18, 18, 19, 16, // Top face
        20, 21, 22, 22, 23, 20, // Bottom face
    };

    private class RenderCubes {
        private final Cube[] cubes;
        public final int vboSize;
        public final int eboSize;

        public RenderCubes(Cube[] cubes){
            this.cubes = cubes;
            vboSize = cubes.length*vertices.length / 3;
            eboSize = cubes.length*indices.length;
        }


        public IntBuffer getEBO(){
            IntBuffer buffer = BufferUtils.createIntBuffer(eboSize);
            for (int i = 0; i < cubes.length;i++){
                for (int index : indices) {
                    buffer.put(index + i * 24);
                }
            }
            return buffer;
        }

        public FloatBuffer getVertVBO(){
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vboSize*3);
            for (Cube cube : cubes) {
                buffer.put(vertices);
            }
            return buffer;
        }

        public FloatBuffer getScaleVBO(){
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vboSize);
            for (Cube cube : cubes) {
                buffer.put(cube.scaleData());
            }
            return buffer;
        }

        public FloatBuffer getTextCoordBuffer(){
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vboSize*2);
            for (Cube cube : cubes) {
                buffer.put(cube.uvData());
            }
            buffer.flip();
            return buffer;
        }

        public void setBlockAtIndex(int index, Block block){
            cubes[index].setBlock(block);
        }

    }


    private class Cube {
        private float scale;
        private Block block;

        public Cube(float scale, Block block){
            this.scale = scale;
            this.block = block;
        }


        public FloatBuffer scaleData(){
            float[] floats = new float[vertices.length / 3];
            Arrays.fill(floats,scale);
            FloatBuffer buff = FloatBuffer.allocate(floats.length);
            buff.put(floats).flip();
            return buff;
        }
        public Cube setScale(){
            scale = 1;
            return this;
        }


        private FloatBuffer uvData(){
            IIcon texture;
            float minu;
            float maxu;
            float minv;
            float maxv;
            FloatBuffer uvBuffer = BufferUtils.createFloatBuffer((vertices.length / 3 * 2));
            float[] face_uvs;

            // Front Face
            texture = block.getIcon(3,0);
            minu = texture.getMinU();
            maxu = texture.getMaxU();
            minv = texture.getMinV();
            maxv = texture.getMaxV();
            face_uvs = new float[]{
                minu, minv,
                minu, maxv,
                maxu, maxv,
                maxu, minv,
            };
            uvBuffer.put(face_uvs);

            // Back Face
            texture = block.getIcon(2,0);
            minu = texture.getMinU();
            maxu = texture.getMaxU();
            minv = texture.getMinV();
            maxv = texture.getMaxV();
            face_uvs = new float[]{
                maxu, maxv,
                maxu, minv,
                minu, minv,
                minu, maxv,
            };
            uvBuffer.put(face_uvs);

            // Left Face
            texture = block.getIcon(4,0);
            minu = texture.getMinU();
            maxu = texture.getMaxU();
            minv = texture.getMinV();
            maxv = texture.getMaxV();
            face_uvs = new float[]{
                minu, maxv,
                maxu, maxv,
                maxu, minv,
                minu, minv,

            };
            uvBuffer.put(face_uvs);


            // Right Face
            texture = block.getIcon(5,0);
            minu = texture.getMinU();
            maxu = texture.getMaxU();
            minv = texture.getMinV();
            maxv = texture.getMaxV();
            face_uvs = new float[]{
                maxu, maxv,
                maxu, minv,
                minu, minv,
                minu, maxv,
            };
            uvBuffer.put(face_uvs);


            // Top Face
            texture = block.getIcon(1,0);
            minu = texture.getMinU();
            maxu = texture.getMaxU();
            minv = texture.getMinV();
            maxv = texture.getMaxV();
            face_uvs = new float[]{
                minu, minv,
                minu, maxv,
                maxu, maxv,
                maxu, minv,

            };
            uvBuffer.put(face_uvs);
            // Bottom Face
            texture = block.getIcon(0,0);
            minu = texture.getMinU();
            maxu = texture.getMaxU();
            minv = texture.getMinV();
            maxv = texture.getMaxV();
            face_uvs = new float[]{
                minu, maxv,
                minu, minv,
                maxu, minv,
                maxu, maxv,
            };
            uvBuffer.put(face_uvs);
            uvBuffer.flip();
            return uvBuffer;
        }
        public void setBlock(Block block) {
            this.block = block;
        }
    }


    private static final float[] vertices = {
        // Front Face
        -0.5f,  0.5f,  0.5f,
        -0.5f, -0.5f,  0.5f,
        0.5f, -0.5f,  0.5f,
        0.5f,  0.5f,  0.5f,
        // Back Face
        -0.5f, -0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,
        0.5f,  0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        // Left face
        -0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f, -0.5f,
        // Right face
        0.5f, -0.5f, -0.5f,
        0.5f,  0.5f, -0.5f,
        0.5f,  0.5f,  0.5f,
        0.5f, -0.5f,  0.5f,
        // Top face
        -0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f,  0.5f,
        0.5f,  0.5f,  0.5f,
        0.5f,  0.5f, -0.5f,
        // Bottom face
        -0.5f, -0.5f,  0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, -0.5f,  0.5f,
    };






































}

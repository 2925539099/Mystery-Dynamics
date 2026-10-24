package com.mysterydynamics.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mysterydynamics.MysteryDynamics;
import com.mysterydynamics.block.InfernalBoilerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 用 bbmodel 数据绘制整栋 4×4×5 锅炉模型。 */
public class InfernalBoilerRenderer implements BlockEntityRenderer<InfernalBoilerBlockEntity> {

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Quad(Vertex[] vertices, ResourceLocation texture, float nx, float ny, float nz,
                        boolean bright) {
    }

    private record ModelData(List<Quad> quads) {
    }

    private static ModelData normal;
    private static ModelData overclock;
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITES = new HashMap<>();

    public InfernalBoilerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(InfernalBoilerBlockEntity boiler, float partialTick, PoseStack pose,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!boiler.isController() || boiler.boilerOrigin().equals(BlockPos.ZERO)) {
            return;
        }
        ModelData data = boiler.isOverclock() ? overclock() : normal();
        if (data == null || data.quads().isEmpty()) {
            return;
        }

        BlockPos pos = boiler.getBlockPos();
        BlockPos origin = boiler.boilerOrigin();
        pose.pushPose();
        pose.translate(origin.getX() - pos.getX(), origin.getY() - pos.getY(),
                origin.getZ() - pos.getZ());
        // 模型数据已平移为从 (0,0,0) 开始；绕 4×4 底座中心旋转。
        pose.translate(2.0D, 0.0D, 2.0D);
        pose.mulPose(Axis.YP.rotationDegrees(boiler.rotation() * 90.0F));
        pose.translate(-2.0D, 0.0D, -2.0D);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        Function<ResourceLocation, TextureAtlasSprite> atlas =
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for (Quad quad : data.quads()) {
            TextureAtlasSprite sprite = SPRITES.computeIfAbsent(quad.texture(), atlas);
            int light = quad.bright() ? LightTexture.FULL_BRIGHT : packedLight;
            for (Vertex vertex : quad.vertices()) {
                consumer.addVertex(pose.last(), vertex.x() / 16.0F, vertex.y() / 16.0F,
                                vertex.z() / 16.0F)
                        .setColor(255, 255, 255, 255)
                        .setUv(sprite.getU(vertex.u()), sprite.getV(vertex.v()))
                        .setLight(light)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setNormal(pose.last(), quad.nx(), quad.ny(), quad.nz());
            }
        }
        pose.popPose();
    }

    private static ModelData normal() {
        if (normal == null) {
            normal = load("infernal_boiler/formed");
        }
        return normal;
    }

    private static ModelData overclock() {
        if (overclock == null) {
            overclock = load("infernal_boiler/formed_overclock");
        }
        return overclock;
    }

    private static ModelData load(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MysteryDynamics.MODID, path);
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, ResourceLocation> textures = new HashMap<>();
            for (var entry : root.getAsJsonObject("textures").entrySet()) {
                textures.put(entry.getKey(),
                        ResourceLocation.parse(entry.getValue().getAsString()));
            }
            List<Quad> quads = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject object = element.getAsJsonObject();
                float[] from = readVec(object.getAsJsonArray("from"));
                float[] to = readVec(object.getAsJsonArray("to"));
                for (var faceEntry : object.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = faceEntry.getValue().getAsJsonObject();
                    String textureKey = face.get("texture").getAsString();
                    if (textureKey.startsWith("#")) {
                        textureKey = textureKey.substring(1);
                    }
                    ResourceLocation texture = textures.get(textureKey);
                    if (texture == null) {
                        continue;
                    }
                    float[] uv = face.has("uv") ? readVec(face.getAsJsonArray("uv"))
                            : new float[]{0, 0, 16, 16};
                    int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    quads.add(createQuad(Direction.byName(faceEntry.getKey()), from, to, uv,
                            rotation, texture, texture.getPath().contains("fire")
                                    || texture.getPath().contains("lava")
                                    || texture.getPath().contains("soul")));
                }
            }
            return new ModelData(List.copyOf(quads));
        } catch (Exception exception) {
            return new ModelData(List.of());
        }
    }

    private static float[] readVec(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    private static Quad createQuad(Direction direction, float[] from, float[] to, float[] uv,
                                   int rotation, ResourceLocation texture, boolean bright) {
        float x1 = from[0], y1 = from[1], z1 = from[2];
        float x2 = to[0], y2 = to[1], z2 = to[2];
        float[][] corners = switch (direction) {
            case NORTH -> new float[][]{
                    {x2, y2, z1}, {x2, y1, z1}, {x1, y1, z1}, {x1, y2, z1}};
            case SOUTH -> new float[][]{
                    {x1, y2, z2}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}};
            case WEST -> new float[][]{
                    {x1, y2, z1}, {x1, y1, z1}, {x1, y1, z2}, {x1, y2, z2}};
            case EAST -> new float[][]{
                    {x2, y2, z2}, {x2, y1, z2}, {x2, y1, z1}, {x2, y2, z1}};
            case UP -> new float[][]{
                    {x1, y2, z1}, {x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}};
            case DOWN -> new float[][]{
                    {x1, y1, z1}, {x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}};
            default -> new float[0][];
        };
        float[][] uvs = {
                {uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}
        };
        int steps = (rotation / 90) & 3;
        for (int step = 0; step < steps; step++) {
            float[] first = uvs[0];
            uvs[0] = uvs[1];
            uvs[1] = uvs[2];
            uvs[2] = uvs[3];
            uvs[3] = first;
        }
        Vertex[] vertices = new Vertex[4];
        for (int i = 0; i < 4; i++) {
            vertices[i] = new Vertex(corners[i][0], corners[i][1], corners[i][2],
                    uvs[i][0], uvs[i][1]);
        }
        float nx = direction.getStepX();
        float ny = direction.getStepY();
        float nz = direction.getStepZ();
        return new Quad(vertices, texture, nx, ny, nz, bright);
    }
}

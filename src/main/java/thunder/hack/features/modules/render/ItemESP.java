package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ItemESP extends Module {
    public ItemESP() {
        super("ItemESP", Category.RENDER);
    }
    private final Setting<ColorSetting> tcolor = new Setting<>("TextColor", new ColorSetting(new Color(-1).getRGB()));

    private final Setting<ESPMode> espMode = new Setting<>("Mode", ESPMode.Rect);

    private final Setting<Float> radius = new Setting<>("Radius", 1f, 0.1f, 5f, v -> espMode.getValue() == ESPMode.Circle);
    private final Setting<Boolean> useHudColor = new Setting<>("UseHudColor", true, v -> espMode.getValue() == ESPMode.Circle);
    private final Setting<Integer> cOffset = new Setting<>("ColorOffset", 2, 1, 50, v -> espMode.getValue() == ESPMode.Circle && useHudColor.getValue());
    private final Setting<ColorSetting> circleColor = new Setting<>("CircleColor", new ColorSetting(new Color(-1).getRGB()), v -> espMode.getValue() == ESPMode.Circle && !useHudColor.getValue());
    private final Setting<Integer> cPoints = new Setting<>("CirclePoints", 12, 3, 32, v -> espMode.getValue() == ESPMode.Circle);

    private final float groupRadius = 1.0f;

    public void onRender2D(DrawContext context) {
        List<ItemEntity> items = new ArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof ItemEntity) {
                items.add((ItemEntity) ent);
            }
        }

        renderGroupedItems(context, items);

        if (espMode.getValue() == ESPMode.Rect) {
            renderRectESP(context, items);
        }
    }

    private void renderGroupedItems(DrawContext context, List<ItemEntity> items) {
        List<ItemGroup> groups = groupNearbyItems(items);

        for (ItemGroup group : groups) {
            Vec3d[] vectors = getPoints(group.centerEntity);
            Vector4d position = calculateScreenPosition(vectors);

            if (position != null) {
                String displayText = createGroupDisplayText(group);
                renderItemText(context, displayText, position);
            }
        }
    }

    private void renderIndividualItems(DrawContext context, List<ItemEntity> items) {
        for (ItemEntity item : items) {
            Vec3d[] vectors = getPoints(item);
            Vector4d position = calculateScreenPosition(vectors);

            if (position != null) {
                String displayText = createItemDisplayText(item);
                renderItemText(context, displayText, position);
            }
        }
    }

    private List<ItemGroup> groupNearbyItems(List<ItemEntity> items) {
        List<ItemGroup> groups = new ArrayList<>();
        Set<ItemEntity> processed = new HashSet<>();

        for (ItemEntity item : items) {
            if (processed.contains(item)) continue;

            ItemGroup group = new ItemGroup();
            group.centerEntity = item;
            group.items = new ArrayList<>();
            group.items.add(item);
            processed.add(item);

            for (ItemEntity otherItem : items) {
                if (processed.contains(otherItem)) continue;

                double distance = item.getPos().distanceTo(otherItem.getPos());
                if (distance <= groupRadius) {
                    group.items.add(otherItem);
                    processed.add(otherItem);
                }
            }

            groups.add(group);
        }

        return groups;
    }

    private String createGroupDisplayText(ItemGroup group) {
        if (group.items.size() == 1) {
            return createItemDisplayText(group.items.get(0));
        }

        Map<String, Integer> itemCounts = new HashMap<>();
        for (ItemEntity item : group.items) {
            String itemName = item.getStack().getItem().getName().getString();
            int stackSize = item.getStack().getCount();
            itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + stackSize);
        }

        StringBuilder sb = new StringBuilder();
        int lineCount = 0;
        int totalItems = itemCounts.size();

        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if (lineCount > 0) sb.append("\n");
            sb.append(entry.getKey());
            if (entry.getValue() > 1) {
                sb.append(" x").append(entry.getValue());
            }
            lineCount++;
            if (lineCount >= 6 && totalItems > 6) {
                int remainingCount = totalItems - 6;
                sb.append("\n+").append(remainingCount).append(" more");
                break;
            }
        }

        return sb.toString();
    }

    private String createItemDisplayText(ItemEntity item) {
        String itemName = item.getStack().getItem().getName().getString();
        int count = item.getStack().getCount();
        return count > 1 ? itemName + " x" + count : itemName;
    }

    private Vector4d calculateScreenPosition(Vec3d[] vectors) {
        Vector4d position = null;
        for (Vec3d vector : vectors) {
            vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                if (position == null)
                    position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
                position.w = Math.max(vector.y, position.w);
            }
        }
        return position;
    }

    private void renderItemText(DrawContext context, String text, Vector4d position) {
        String[] lines = text.split("\n");
        float posX = (float) position.x;
        float posY = (float) position.y;
        float endPosX = (float) position.z;

        float diff = (endPosX - posX) / 2f;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            float textWidth = FontRenderers.sf_bold_mini.getStringWidth(line);
            float tagX = posX + diff - textWidth / 2f;
            float tagY = posY - 10 - (i * 10);

            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), line, tagX, tagY,
                    tcolor.getValue().getColor());
        }
    }

    private void renderRectESP(DrawContext context, List<ItemEntity> items) {
        List<ItemEntity> visibleItems = getVisibleGroupedItems(items);

        if (visibleItems.isEmpty()) return;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Render2DEngine.setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (ItemEntity item : visibleItems) {
            Vec3d[] vectors = getPoints(item);
            Vector4d position = calculateScreenPosition(vectors);

            if (position != null) {
                float posX = (float) position.x;
                float posY = (float) position.y;
                float endPosX = (float) position.z;
                float endPosY = (float) position.w;

                drawRect(bufferBuilder, matrix, posX, posY, endPosX, endPosY);
            }
        }
        Render2DEngine.endBuilding(bufferBuilder);
        Render2DEngine.endRender();
    }

    private List<ItemEntity> getVisibleGroupedItems(List<ItemEntity> items) {
        List<ItemGroup> groups = groupNearbyItems(items);
        List<ItemEntity> visibleItems = new ArrayList<>();

        for (ItemGroup group : groups) {
            visibleItems.add(group.centerEntity);
        }

        return visibleItems;
    }

    public void onRender3D(MatrixStack stack) {
        if (espMode.getValue() == ESPMode.Circle) {
            List<ItemEntity> items = new ArrayList<>();
            for (Entity ent : mc.world.getEntities()) {
                if (ent instanceof ItemEntity) {
                    items.add((ItemEntity) ent);
                }
            }

            List<ItemEntity> itemsToRender = getVisibleGroupedItems(items);

            for (ItemEntity item : itemsToRender) {
                Render3DEngine.drawCircle3D(stack, item, radius.getValue(),
                        circleColor.getValue().getColor(), cPoints.getValue(),
                        useHudColor.getValue(), cOffset.getValue());
            }
        }
    }

    private void drawRect(BufferBuilder bufferBuilder, Matrix4f stack, float posX, float posY, float endPosX, float endPosY) {
        Color black = Color.BLACK;
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1F, posY, (posX + 0.5f), endPosY + 0.5f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1F, (posY - 0.5f), endPosX + 0.5f, posY + 1f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, endPosX - 1f, posY, endPosX + 0.5f, endPosY + 0.5f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1, endPosY - 1f, endPosX + 0.5f, endPosY + 0.5f, black, black, black, black);
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 0.5f, posY, posX, endPosY, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270));
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX, endPosY - 0.5f, endPosX, endPosY, HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(180), HudEditor.getColor(0));
        Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 0.5f, posY, endPosX, (posY + 0.5f), HudEditor.getColor(180), HudEditor.getColor(90), HudEditor.getColor(90), HudEditor.getColor(180));
        Render2DEngine.setRectPoints(bufferBuilder, stack, endPosX - 0.5f, posY, endPosX, endPosY, HudEditor.getColor(90), HudEditor.getColor(270), HudEditor.getColor(270), HudEditor.getColor(90));
    }

    @NotNull
    private static Vec3d[] getPoints(Entity ent) {
        Box axisAlignedBB = getBox(ent);
        Vec3d[] vectors = new Vec3d[]{
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
        };
        return vectors;
    }

    @NotNull
    private static Box getBox(Entity ent) {
        double x = ent.prevX + (ent.getX() - ent.prevX) * Render3DEngine.getTickDelta();
        double y = ent.prevY + (ent.getY() - ent.prevY) * Render3DEngine.getTickDelta();
        double z = ent.prevZ + (ent.getZ() - ent.prevZ) * Render3DEngine.getTickDelta();
        Box axisAlignedBB2 = ent.getBoundingBox();
        Box axisAlignedBB = new Box(
                axisAlignedBB2.minX - ent.getX() + x - 0.05,
                axisAlignedBB2.minY - ent.getY() + y,
                axisAlignedBB2.minZ - ent.getZ() + z - 0.05,
                axisAlignedBB2.maxX - ent.getX() + x + 0.05,
                axisAlignedBB2.maxY - ent.getY() + y + 0.15,
                axisAlignedBB2.maxZ - ent.getZ() + z + 0.05
        );
        return axisAlignedBB;
    }

    private static class ItemGroup {
        ItemEntity centerEntity;
        List<ItemEntity> items;
    }

    private enum ESPMode {
        Rect, Circle, None
    }
}
package com.github.haoyiyu.create_headsupdisplay.screen;

import com.github.haoyiyu.create_headsupdisplay.CreateHeadsUpDisplay;
import com.github.haoyiyu.create_headsupdisplay.client.ClientOmniCoreData;
import com.github.haoyiyu.create_headsupdisplay.client.DynamicTextureCache;
import com.github.haoyiyu.create_headsupdisplay.network.*;
import com.github.haoyiyu.create_headsupdisplay.config.TranslationConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class OmniCoreScreen extends Screen {
    private final BlockPos corePos;
    private final List<SourceEntry> sources = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 25;
    private int tickCounter = 0;
    private long lastDataHash = 0;
    private boolean autoSort = true;

    public OmniCoreScreen(CompoundTag data) {
        super(Component.translatable("gui.create_headsupdisplay.omni_core.title"));
        this.corePos = BlockPos.of(data.getLong("CorePos"));
        loadSourcesFromTag(data);
    }

    private void loadSourcesFromTag(CompoundTag data) {
        sources.clear();
        ListTag sourcesTag = data.getList("Sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < sourcesTag.size(); i++) {
            CompoundTag srcTag = sourcesTag.getCompound(i);
            String name = srcTag.getString("name");
            ItemStack item1 = ItemStack.EMPTY;
            ItemStack item2 = ItemStack.EMPTY;
            try {
                if (srcTag.contains("item1")) {
                    item1 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("item1"));
                } else if (srcTag.contains("freqItem1")) {
                    item1 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("freqItem1"));
                }
                if (srcTag.contains("item2")) {
                    item2 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("item2"));
                } else if (srcTag.contains("freqItem2")) {
                    item2 = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), srcTag.getCompound("freqItem2"));
                }
            } catch (Exception ignored) {}
            int strength = srcTag.getInt("strength");
            String display = srcTag.contains("display") ? srcTag.getString("display") : String.valueOf(strength);
            String dlText = srcTag.contains("dlText") ? srcTag.getString("dlText") : null;
            BlockPos dlSourcePos = srcTag.contains("dlSourcePos") ? BlockPos.of(srcTag.getLong("dlSourcePos")) : null;
            SourceEntry entry = new SourceEntry(name, item1, item2, strength, display, dlText, dlSourcePos);
            if (srcTag.contains("type")) {
                entry.isImage = "IMAGE".equals(srcTag.getString("type"));
            }
            if (entry.isImage && srcTag.contains("ImageId")) {
                entry.imageId = srcTag.getUUID("ImageId");
                entry.imageFileName = srcTag.getString("ImageFileName");
                entry.imageData = srcTag.getByteArray("ImageData");
            }
            if (srcTag.contains("translation")) {
                entry.transConfig = TranslationConfig.deserialize(srcTag.getCompound("translation"));
                entry.trans = true;
            }
            sources.add(entry);
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.add_source"), b -> {
            PacketDistributor.sendToServer(new RequestOpenFrequencySelectionPayload(corePos));
        }).bounds(10, 10, 100, 20).build());

        // 添加图片按钮 → 打开子菜单
        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.display_terminal.add_image"), b -> {
            Minecraft.getInstance().setScreen(new ImageUploadScreen(this));
        }).bounds(10, 34, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.sort_on"), b -> {
            PacketDistributor.sendToServer(new ToggleAutoSortPayload(corePos));
            autoSort = !autoSort;
            b.setMessage(Component.translatable(autoSort ? "gui.create_headsupdisplay.sort_on" : "gui.create_headsupdisplay.sort_off"));
        }).bounds(10, 58, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.back"), b -> onClose())
                .bounds(10, height - 30, 60, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (tickCounter >= 3) {
            tickCounter = 0;
            PacketDistributor.sendToServer(new RequestSourcesDataPayload(corePos));
        }
        CompoundTag synced = ClientOmniCoreData.getSourcesData(corePos);
        if (synced != null) {
            autoSort = synced.getBoolean("AutoSort");
            loadSourcesFromTag(synced);
        }
    }

    /** 由 ImageUploadScreen 回调，开始上传（在 MC 线程） */
    void startImageUpload(byte[] fileBytes, String fileName) {
        UUID imageId = UUID.randomUUID();
        int chunkSize = 30000;
        int totalChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);

        PacketDistributor.sendToServer(new UploadImageInitPayload(imageId, fileName, totalChunks));

        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, fileBytes.length);
            PacketDistributor.sendToServer(new UploadImageChunkPayload(imageId, i, Arrays.copyOfRange(fileBytes, start, end)));
        }

        PacketDistributor.sendToServer(new UploadImageCompletePayload(corePos, imageId));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int startX = 160;
        int startY = 18;
        graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.signal_sources").getString(), startX, startY - 5, 0xFFFFFF);

        int listTop = startY;
        int listBottom = startY + sources.size() * ENTRY_HEIGHT;
        int listLeft = startX - 44;
        graphics.fill(listLeft, listTop, width, listBottom, 0xCC000000);

        for (int i = 0; i < sources.size(); i++) {
            int y = startY + i * ENTRY_HEIGHT - scrollOffset;
            if (y < 0 || y > height) continue;
            SourceEntry src = sources.get(i);

            int iconY = y + 3;
            int iconSize = 16;
            int icon1X = startX - 42;
            int icon2X = startX - 22;

            if (src.isImage) {
                int purpleLeft = icon1X - 1;
                int purpleRight = icon2X + iconSize + 1;
                graphics.fill(purpleLeft, iconY - 1, purpleRight, iconY + iconSize + 1, 0xAAFF00FF);
                int centerIconX = (purpleLeft + purpleRight) / 2 - 8;
                graphics.renderItem(new ItemStack(net.minecraft.world.item.Items.PAINTING), centerIconX, iconY);
            } else if (src.dlText != null) {
                int yellowLeft = icon1X - 1;
                int yellowRight = icon2X + iconSize + 1;
                graphics.fill(yellowLeft, iconY - 1, yellowRight, iconY + iconSize + 1, 0xAAFFAA00);
                ItemStack displayIcon = ItemStack.EMPTY;
                if (src.dlSourcePos != null && Minecraft.getInstance().level != null) {
                    var sourceState = Minecraft.getInstance().level.getBlockState(src.dlSourcePos);
                    displayIcon = new ItemStack(sourceState.getBlock().asItem());
                }
                if (displayIcon.isEmpty()) {
                    displayIcon = new ItemStack(net.minecraft.world.item.Items.REPEATER);
                }
                graphics.renderItem(displayIcon, (yellowLeft + yellowRight) / 2 - 8, iconY);
            } else {
                graphics.fill(icon1X - 1, iconY - 1, icon1X + iconSize + 1, iconY + iconSize + 1, 0xAAFF0000);
                graphics.fill(icon2X - 1, iconY - 1, icon2X + iconSize + 1, iconY + iconSize + 1, 0xAA0000FF);
                if (!src.freqItem1.isEmpty()) graphics.renderItem(src.freqItem1, icon1X, iconY);
                if (!src.freqItem2.isEmpty()) graphics.renderItem(src.freqItem2, icon2X, iconY);
            }

            String display;
            if (src.isImage) {
                display = "[IMG] " + (src.imageFileName != null ? src.imageFileName : src.name);
            } else if (src.dlText != null) {
                display = src.name + ": " + src.dlText;
            } else {
                display = src.name + ": " + src.display;
            }
            display = display.replaceAll("§[0-9a-fk-or]", "");
            // 超长文本滚动显示
            int maxTextWidth = (width - 120) - startX - 4;
            int textWidth = font.width(display);
            if (textWidth > maxTextWidth) {
                int scrollSpeed = 1;
                int scrollMax = textWidth - maxTextWidth + 20;
                int offset = (tickCounter * scrollSpeed) % scrollMax;
                graphics.enableScissor(startX, y, width - 120, y + ENTRY_HEIGHT);
                graphics.drawString(font, display, startX - offset, y + 5, 0xFFFFFF);
                graphics.disableScissor();
            } else {
                graphics.drawString(font, display, startX, y + 5, 0xFFFFFF);
            }

            if (src.isImage) {
                int detailBtnX = width - 120;
                graphics.fill(detailBtnX, y, detailBtnX + 30, y + 20, 0xAA4444AA);
                graphics.drawString(font, "D", detailBtnX + 11, y + 6, 0xFFFFFF);
            } else if (src.dlText == null) {
                int transBtnX = width - 120;
                graphics.fill(transBtnX, y, transBtnX + 30, y + 20, src.trans ? 0xAAFFA500 : 0xAA555555);
                graphics.drawString(font, "T", transBtnX + 11, y + 6, 0xFFFFFF);
            }

            int sendBtnX = width - 80;
            int delBtnX = width - 40;
            graphics.fill(sendBtnX, y, sendBtnX + 30, y + 20, 0xAA00AA00);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.send").getString(), sendBtnX + 5, y + 6, 0xFFFFFF);
            graphics.fill(delBtnX, y, delBtnX + 30, y + 20, 0xAAFF0000);
            graphics.drawString(font, Component.translatable("gui.create_headsupdisplay.delete").getString(), delBtnX + 5, y + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int startY = 20;
        for (int i = 0; i < sources.size(); i++) {
            int y = startY + i * ENTRY_HEIGHT - scrollOffset;
            if (y < 0 || y > height) continue;
            // IMAGE 详情按钮
            if (sources.get(i).isImage && mouseX >= width - 120 && mouseX <= width - 90 && mouseY >= y && mouseY <= y + 20) {
                final int idx = i;
                SourceEntry entry = sources.get(idx);
                Minecraft.getInstance().setScreen(new ImageDetailScreen(entry));
                return true;
            }
            if (!sources.get(i).isImage && sources.get(i).dlText == null && mouseX >= width - 120 && mouseX <= width - 90 && mouseY >= y && mouseY <= y + 20) {
                final int idx = i;
                SourceEntry entry = sources.get(idx);
                TranslationConfig tc = entry.transConfig != null ? entry.transConfig : new TranslationConfig();
                Minecraft.getInstance().setScreen(new TranslationScreen(tc, config -> {
                    entry.transConfig = config;
                    entry.trans = config.getMode() != TranslationConfig.Mode.NONE;
                    PacketDistributor.sendToServer(new UpdateTranslationPayload(corePos, idx, config.serialize()));
                }));
                return true;
            }
            if (mouseX >= width - 80 && mouseX <= width - 50 && mouseY >= y && mouseY <= y + 20) {
                PacketDistributor.sendToServer(new SendSourceToTerminalPayload(corePos, i));
                return true;
            }
            if (mouseX >= width - 40 && mouseX <= width - 10 && mouseY >= y && mouseY <= y + 20) {
                // IMAGE 源：删除本地图片文件
                SourceEntry entry = sources.get(i);
                if (entry.isImage && entry.imageFileName != null) {
                    try {
                        Path imgFile = getImageFolder().resolve(entry.imageFileName);
                        Files.deleteIfExists(imgFile);
                    } catch (Exception ignored) {}
                }
                PacketDistributor.sendToServer(new RemoveRedstoneSourcePayload(corePos, i));
                sources.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = sources.size() * ENTRY_HEIGHT - (height - 40);
        if (maxScroll > 0) {
            scrollOffset = (int) Math.clamp(scrollOffset - scrollY * 10, 0, maxScroll);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public void addSource(String name) {
        sources.add(new SourceEntry(name, ItemStack.EMPTY, ItemStack.EMPTY, 0, "0", null, null));
    }

    // ========== 图片上传子菜单 ==========

    /** 客户端图片文件夹 */
    static Path getImageFolder() {
        Path dir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("create_headsupdisplay_images");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir;
    }

    public class ImageUploadScreen extends Screen {
        private final OmniCoreScreen parent;
        private byte[] loadedBytes;
        private String loadedFileName;
        private UUID previewTexId;
        private ResourceLocation previewTex;

        // 文件列表
        private List<String> pngFiles = new ArrayList<>();
        private int fileScrollOffset = 0;
        private static final int FILE_ENTRY_H = 18;

        // 上传状态
        private boolean uploading = false;
        private int totalChunks;
        private int sentChunks;
        private int uploadTick = 0;

        protected ImageUploadScreen(OmniCoreScreen parent) {
            super(Component.translatable("gui.create_headsupdisplay.upload_image.title"));
            this.parent = parent;
        }

        private void refreshFileList() {
            pngFiles.clear();
            Path dir = getImageFolder();
            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(n -> {
                            String lo = n.toLowerCase();
                            return lo.endsWith(".png") || lo.endsWith(".jpg") || lo.endsWith(".jpeg");
                        })
                        .sorted()
                        .forEach(pngFiles::add);
            } catch (Exception ignored) {}
        }

        @Override
        protected void init() {
            super.init();
            refreshFileList();
            fileScrollOffset = 0;

            // 底部返回按钮
            addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.back"), b -> {
                if (uploading) return;
                cleanupPreview();
                Minecraft.getInstance().setScreen(parent);
            }).bounds(10, height - 30, 60, 20).build());
        }

        /** 从文件列表中选择一个文件并加载预览（JPG 自动缩放转 PNG） */
        private void selectFile(String fileName) {
            try {
                Path path = getImageFolder().resolve(fileName);
                byte[] raw = Files.readAllBytes(path);
                if (raw.length > 512 * 1024) return; // 原始文件也限制 512KB
                String lo = fileName.toLowerCase();

                byte[] pngBytes;
                if (lo.endsWith(".jpg") || lo.endsWith(".jpeg")) {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
                    if (image == null) return;
                    // 限制最大尺寸 512x512，防止 OOM
                    int maxDim = 512;
                    int w = image.getWidth();
                    int h = image.getHeight();
                    if (w > maxDim || h > maxDim) {
                        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                        w = (int) (w * ratio);
                        h = (int) (h * ratio);
                        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        scaled.createGraphics().drawImage(image, 0, 0, w, h, null);
                        image = scaled;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", bos);
                    pngBytes = bos.toByteArray();
                } else {
                    pngBytes = raw;
                }

                // 最终大小检查
                if (pngBytes.length > 2 * 1024 * 1024) {
                    CreateHeadsUpDisplay.LOGGER.warn("Converted image too large: {} bytes", pngBytes.length);
                    return;
                }

                cleanupPreview();
                this.loadedBytes = pngBytes;
                this.loadedFileName = fileName;
                this.previewTexId = UUID.randomUUID();
                this.previewTex = DynamicTextureCache.getOrCreate(previewTexId, pngBytes);
            } catch (Exception e) {
                CreateHeadsUpDisplay.LOGGER.error("Preview failed: {}", fileName, e);
            }
        }

        private void cleanupPreview() {
            if (previewTexId != null) {
                DynamicTextureCache.remove(previewTexId);
                previewTexId = null;
                previewTex = null;
            }
        }

        @Override
        public void tick() {
            super.tick();
            if (uploading) {
                uploadTick++;
                if (uploadTick >= 2 && sentChunks < totalChunks) {
                    uploadTick = 0;
                    int start = sentChunks * 30000;
                    int end = Math.min(start + 30000, loadedBytes.length);
                    byte[] chunk = Arrays.copyOfRange(loadedBytes, start, end);
                    PacketDistributor.sendToServer(new UploadImageChunkPayload(previewTexId, sentChunks, chunk));
                    sentChunks++;

                    if (sentChunks >= totalChunks) {
                        PacketDistributor.sendToServer(new UploadImageCompletePayload(parent.corePos, previewTexId));
                        uploading = false;
                        cleanupPreview();
                        Minecraft.getInstance().setScreen(parent);
                    }
                }
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);

            // 标题 + 刷新按钮
            graphics.drawCenteredString(font, "Upload Image", width / 2, 10, 0xFFFFFF);

            // ===== 左侧文件列表 =====
            int listLeft = 10;
            int listTop = 28;
            int listWidth = 150;
            int listBottom = height - 40;

            // 背景
            graphics.fill(listLeft, listTop, listLeft + listWidth, listBottom, 0xCC222222);
            graphics.drawString(font, "Images (" + pngFiles.size() + ")", listLeft + 4, listTop + 2, 0xAAAAAA);

            // 刷新按钮
            int refreshX = listLeft + listWidth - 42;
            int refreshY = listTop + 1;
            boolean refreshHovered = mouseX >= refreshX && mouseX <= refreshX + 40 && mouseY >= refreshY && mouseY <= refreshY + 14;
            graphics.fill(refreshX, refreshY, refreshX + 40, refreshY + 14, refreshHovered ? 0xFF556688 : 0xFF334455);
            graphics.drawString(font, "Refresh", refreshX + 2, refreshY + 3, 0xCCCCCC);

            graphics.fill(listLeft, listTop + 16, listLeft + listWidth, listTop + 17, 0xFF555555);

            // 文件条目
            int entryTop = listTop + 20;
            for (int i = 0; i < pngFiles.size(); i++) {
                int y = entryTop + i * FILE_ENTRY_H - fileScrollOffset;
                if (y < entryTop - 2 || y > listBottom) continue;

                String name = pngFiles.get(i);
                boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= y && mouseY <= y + FILE_ENTRY_H;
                boolean selected = name.equals(loadedFileName);

                if (selected) graphics.fill(listLeft, y, listLeft + listWidth, y + FILE_ENTRY_H, 0xAA4444AA);
                else if (hovered) graphics.fill(listLeft, y, listLeft + listWidth, y + FILE_ENTRY_H, 0xAA555555);

                graphics.drawString(font, truncate(name, listWidth - 8), listLeft + 4, y + 4, 0xCCCCCC);
            }

            // 文件列表滚动边界
            int maxFileScroll = Math.max(0, pngFiles.size() * FILE_ENTRY_H - (listBottom - entryTop));
            int scrollBarX = listLeft + listWidth - 6;
            if (maxFileScroll > 0) {
                graphics.fill(scrollBarX, entryTop, scrollBarX + 4, listBottom, 0xFF333333);
                int thumbH = Math.max(16, (listBottom - entryTop) * (listBottom - entryTop) / (pngFiles.size() * FILE_ENTRY_H));
                int thumbY = entryTop + (int)((listBottom - entryTop - thumbH) * ((double)fileScrollOffset / maxFileScroll));
                graphics.fill(scrollBarX, thumbY, scrollBarX + 4, Math.min(thumbY + thumbH, listBottom), 0xFF888888);
            }

            // ===== 中间预览区 =====
            int previewLeft = listLeft + listWidth + 12;
            int previewTop = 28;
            int previewRight = width - 92; // 给右侧按钮留空间
            int previewBottom = height - 55; // 给进度条留空间
            int previewW = previewRight - previewLeft;
            int previewH = previewBottom - previewTop;

            // 预览背景
            graphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0xCC333333);

            if (previewTex != null) {
                int iw = DynamicTextureCache.getWidth(previewTexId);
                int ih = DynamicTextureCache.getHeight(previewTexId);
                if (iw > 0 && ih > 0) {
                    float fitScale = Math.min((float) previewW / iw, (float) previewH / ih);
                    int dw = (int) (iw * fitScale);
                    int dh = (int) (ih * fitScale);
                    int dx = previewLeft + (previewW - dw) / 2;
                    int dy = previewTop + (previewH - dh) / 2;
                    RenderSystem.enableBlend();
                    graphics.blit(previewTex, dx, dy, 0, 0, dw, dh, dw, dh);
                    RenderSystem.disableBlend();
                }
                // 文件名
                int nameY = previewBottom + 2;
                graphics.drawCenteredString(font, loadedFileName,
                        previewLeft + previewW / 2, nameY, 0xAAAAAA);
            } else {
                graphics.drawCenteredString(font, "Select an image from the left",
                        previewLeft + previewW / 2, previewTop + previewH / 2 - 10, 0x888888);
            }

            // ===== 右侧上传按钮 =====
            int uploadBtnX = previewRight + 6;
            int uploadBtnY = previewTop + 20;
            int uploadBtnW = 80;
            if (!uploading) {
                boolean canUpload = loadedBytes != null;
                graphics.fill(uploadBtnX, uploadBtnY, uploadBtnX + uploadBtnW, uploadBtnY + 30,
                        canUpload ? 0xFF008800 : 0xFF444444);
                graphics.drawCenteredString(font, "Upload",
                        uploadBtnX + uploadBtnW / 2, uploadBtnY + 10, 0xFFFFFF);
            } else {
                graphics.fill(uploadBtnX, uploadBtnY, uploadBtnX + uploadBtnW, uploadBtnY + 30, 0xFFCC0000);
                graphics.drawCenteredString(font, "Stop",
                        uploadBtnX + uploadBtnW / 2, uploadBtnY + 10, 0xFFFFFF);
            }

            // ===== 进度条（预览区下方） =====
            int barY = previewBottom + 16;
            if (uploading && totalChunks > 0) {
                int barX = previewLeft;
                int barW = previewW;
                int barH = 12;
                graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF444444);
                int fillW = (int) (barW * ((float) sentChunks / totalChunks));
                if (fillW > 0) graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF00CC00);
                String pct = sentChunks + "/" + totalChunks + " (" + (int)(100f * sentChunks / totalChunks) + "%)";
                graphics.drawCenteredString(font, pct, previewLeft + previewW / 2, barY + 2, 0xFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) return true;

            int listLeft = 10;
            int listTop = 28;
            int listWidth = 150;

            // 刷新按钮
            int refreshX = listLeft + listWidth - 42;
            int refreshY = listTop + 1;
            if (mouseX >= refreshX && mouseX <= refreshX + 40 && mouseY >= refreshY && mouseY <= refreshY + 14) {
                refreshFileList();
                fileScrollOffset = 0;
                return true;
            }

            // 文件列表点击
            int entryTop = listTop + 20;
            int listBottom = height - 40;

            if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= entryTop && mouseY <= listBottom) {
                int idx = (int)(mouseY - entryTop + fileScrollOffset) / FILE_ENTRY_H;
                if (idx >= 0 && idx < pngFiles.size()) {
                    selectFile(pngFiles.get(idx));
                    return true;
                }
            }

            // 上传按钮
            int previewRight = width - 92;
            int uploadBtnX = previewRight + 6;
            int uploadBtnY = 28 + 20;
            int uploadBtnW = 80;
            if (mouseX >= uploadBtnX && mouseX <= uploadBtnX + uploadBtnW
                    && mouseY >= uploadBtnY && mouseY <= uploadBtnY + 30) {
                if (uploading) {
                    uploading = false;
                } else if (loadedBytes != null) {
                    uploading = true;
                    sentChunks = 0;
                    uploadTick = 0;
                    totalChunks = (int) Math.ceil((double) loadedBytes.length / 30000);
                    PacketDistributor.sendToServer(new UploadImageInitPayload(previewTexId, loadedFileName, totalChunks));
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            int listLeft = 10;
            int listTop = 28;
            int listWidth = 150;
            int entryTop = listTop + 20;
            int listBottom = height - 40;

            if (mouseX >= listLeft && mouseX <= listLeft + listWidth) {
                int maxScroll = Math.max(0, pngFiles.size() * FILE_ENTRY_H - (listBottom - entryTop));
                if (maxScroll > 0) {
                    fileScrollOffset = (int) Math.clamp(fileScrollOffset - scrollY * 10, 0, maxScroll);
                }
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public void onClose() {
            if (!uploading) {
                cleanupPreview();
                Minecraft.getInstance().setScreen(parent);
            }
        }

        private String truncate(String s, int maxPx) {
            if (font.width(s) <= maxPx) return s;
            while (font.width(s + "...") > maxPx && s.length() > 1) {
                s = s.substring(0, s.length() - 1);
            }
            return s + "...";
        }
    }

    // ========== 图片详情子菜单 ==========

    private class ImageDetailScreen extends Screen {
        private final SourceEntry entry;

        ImageDetailScreen(SourceEntry entry) {
            super(Component.literal(entry.imageFileName != null ? entry.imageFileName : "Image"));
            this.entry = entry;
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("gui.create_headsupdisplay.back"), b -> {
                Minecraft.getInstance().setScreen(OmniCoreScreen.this);
            }).bounds(10, height - 30, 60, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);

            int cx = width / 2;
            graphics.drawCenteredString(font, entry.imageFileName, cx, 10, 0xFFFFFF);

            ResourceLocation tex = DynamicTextureCache.getOrCreate(entry.imageId, entry.imageData);
            if (tex != null) {
                int iw = DynamicTextureCache.getWidth(entry.imageId);
                int ih = DynamicTextureCache.getHeight(entry.imageId);
                if (iw > 0 && ih > 0) {
                    int maxW = width - 40;
                    int maxH = height - 60;
                    float fit = Math.min((float) maxW / iw, (float) maxH / ih);
                    int dw = (int) (iw * fit);
                    int dh = (int) (ih * fit);
                    int dx = (width - dw) / 2;
                    int dy = 30 + (maxH - dh) / 2;
                    RenderSystem.enableBlend();
                    graphics.blit(tex, dx, dy, 0, 0, dw, dh, dw, dh);
                    RenderSystem.disableBlend();
                }
            }
        }
    }

    // ========== SourceEntry ==========

    private static class SourceEntry {
        String name;
        ItemStack freqItem1;
        ItemStack freqItem2;
        int strength;
        String display;
        String dlText;
        BlockPos dlSourcePos;
        boolean trans;
        TranslationConfig transConfig;
        boolean isImage;
        UUID imageId;
        String imageFileName;
        byte[] imageData;

        SourceEntry(String n, ItemStack i1, ItemStack i2, int s, String disp, String d, BlockPos dp) {
            name = n; freqItem1 = i1; freqItem2 = i2; strength = s; display = disp; dlText = d; dlSourcePos = dp;
        }
    }
}

package com.phat.rbmk.client;

import com.phat.rbmk.menu.ReactorControllerMenu;
import com.phat.rbmk.multiblock.ChannelType;
import com.phat.rbmk.network.ReactorStatusCache;
import com.phat.rbmk.network.ReactorStatusPayload;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import com.phat.rbmk.block.entity.FuelInputHandler;
import com.phat.rbmk.block.entity.SpentOutputHandler;

/** GUI bộ điều khiển: bản đồ lõi lục giác theo nhiệt độ + số liệu + nút điều khiển. */
public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {
    private static final int W = 344;
    private static final int H = 318;
    private static final int MAP_X = 8;
    private static final int MAP_Y = 22;
    private static final int MAP_SIZE = 176;
    private static final int PANEL_X = 194;

    private static final int C_BG = 0xF0141820;
    private static final int C_FRAME = 0xFF3C4452;
    private static final int C_PANEL = 0xFF1E2430;
    private static final int C_TEXT = 0xFFE6E6E6;
    private static final int C_DIM = 0xFF9AA3B2;
    private static final int C_WARN = 0xFFFF5555;
    private static final int C_OK = 0xFF55FF77;

    private Button minus;
    private Button plus;
    private Button mode;
    private Button az5;
    private Button reset;

    public ReactorControllerScreen(ReactorControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        int bx = leftPos + PANEL_X;
        int by = topPos + 158;
        minus = addRenderableWidget(Button.builder(Component.literal("-10%"), b -> click(ReactorControllerMenu.BTN_MINUS))
                .bounds(bx, by, 34, 18).build());
        plus = addRenderableWidget(Button.builder(Component.literal("+10%"), b -> click(ReactorControllerMenu.BTN_PLUS))
                .bounds(bx + 36, by, 34, 18).build());
        mode = addRenderableWidget(Button.builder(Component.translatable("gui.rbmk.mode_redstone"), b -> click(ReactorControllerMenu.BTN_MODE))
                .bounds(bx + 72, by, 70, 18).build());
        az5 = addRenderableWidget(Button.builder(Component.literal("AZ-5").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        b -> click(ReactorControllerMenu.BTN_AZ5))
                .bounds(bx, by + 22, 70, 20).build());
        reset = addRenderableWidget(Button.builder(Component.translatable("gui.rbmk.reset"), b -> click(ReactorControllerMenu.BTN_RESET))
                .bounds(bx + 72, by + 22, 70, 20).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Nullable
    private ReactorStatusPayload data() {
        ReactorStatusPayload p = ReactorStatusCache.get();
        return p != null && p.pos().equals(menu.getPos()) ? p : null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ReactorStatusPayload d = data();
        boolean manual = d != null && d.manual();
        mode.setMessage(Component.translatable(manual ? "gui.rbmk.mode_manual" : "gui.rbmk.mode_redstone"));
        boolean formed = d != null && d.formed();
        minus.active = formed;
        plus.active = formed;
        mode.active = formed;
        az5.active = formed && !d.az5();
        reset.active = formed && d.az5() && d.rod() >= 0.99f;
    }

    @Override
    public void removed() {
        super.removed();
        ReactorStatusCache.clear();
    }

    // ------------------------------------------------------------------ vẽ

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, C_FRAME);
        g.fill(x, y, x + W, y + H, C_BG);
        g.fill(x + MAP_X - 2, y + MAP_Y - 2, x + MAP_X + MAP_SIZE + 2, y + MAP_Y + MAP_SIZE + 2, C_PANEL);
        g.fill(x + PANEL_X - 4, y + 18, x + W - 4, y + 152, C_PANEL);
        // Vách ngăn phần kho đồ
        g.fill(x + 6, y + 223, x + W - 6, y + 224, C_FRAME);
        // Nền các ô
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            int border = slot.index < FuelInputHandler.SLOTS ? 0xFF2F6FD6
                    : slot.index < FuelInputHandler.SLOTS + SpentOutputHandler.SLOTS ? 0xFF9A5A2A : 0xFF4A5261;
            g.fill(sx, sy, sx + 18, sy + 18, border);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0F1218);
        }
        // Mũi tên: nạp vào lò -> ra ô đã cháy
        g.drawString(font, "→", x + 61, y + ReactorControllerMenu.INV_TOP + 22, C_DIM, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Toạ độ ở đây đã được dịch về góc GUI
        g.drawString(font, title, 8, 7, 0xFFFFAA00, false);
        int labelY = ReactorControllerMenu.INV_TOP - 11;
        g.drawString(font, Component.translatable("gui.rbmk.fuel_in"), ReactorControllerMenu.FUEL_X - 1, labelY, 0xFF6EA8FF, false);
        g.drawString(font, Component.translatable("gui.rbmk.fuel_out"), ReactorControllerMenu.SPENT_X - 1, labelY, 0xFFE0A060, false);
        g.drawString(font, playerInventoryTitle, ReactorControllerMenu.PLAYER_X - 1, labelY, C_DIM, false);
        ReactorStatusPayload d = data();
        if (d == null) {
            g.drawString(font, Component.translatable("gui.rbmk.waiting"), 8, MAP_Y + 4, C_DIM, false);
            return;
        }
        if (!d.formed()) {
            g.drawString(font, Component.translatable("status.rbmk.not_formed"), 8, MAP_Y + 4, C_WARN, false);
            g.drawWordWrap(font, d.message(), 8, MAP_Y + 18, MAP_SIZE, C_DIM);
        } else {
            drawMap(g, d, mouseX - leftPos, mouseY - topPos);
        }
        drawStats(g, d);
    }

    private void drawStats(GuiGraphics g, ReactorStatusPayload d) {
        int x = PANEL_X;
        int right = W - 8;
        int y = 22;
        String sub = d.formed()
                ? Component.translatable("gui.rbmk.core_size", d.radius(), d.height()).getString()
                : "-";
        g.drawString(font, sub, x, y, C_DIM, false);
        y += 12;

        y = row(g, x, right, y, "gui.rbmk.power",
                pct(d.power()) + "% → " + pct(d.setpoint()) + "%", d.power() > 1.6f ? C_WARN : C_TEXT);
        y = bar(g, x, right, y, d.power() / 1.5f, d.power() > 1.2f ? 0xFFFFAA00 : 0xFF3B82F6);

        float tFrac = d.maxTemp() / d.ruptureTemp();
        y = row(g, x, right, y, "gui.rbmk.temp",
                Math.round(d.maxTemp()) + " / " + Math.round(d.ruptureTemp()) + "°C", tFrac > 0.8f ? C_WARN : C_TEXT);
        y = bar(g, x, right, y, tFrac, tempColor(tFrac));

        boolean running = d.power() > 0.02f;
        boolean ormLow = running && d.rod() < d.ormWarning();
        y = row(g, x, right, y, "gui.rbmk.rods", pct(d.rod()) + "%", ormLow ? C_WARN : C_TEXT);
        y = row(g, x, right, y, "gui.rbmk.void_xenon", pct(d.voidFrac()) + "% | " + String.format("%.2f", d.xenon()), C_TEXT);
        y = row(g, x, right, y, "gui.rbmk.fuel", d.loadedFuel() + " / " + d.fuelSlots(), d.loadedFuel() == 0 ? C_WARN : C_TEXT);
        y = row(g, x, right, y, "gui.rbmk.water",
                Math.round(d.waterPerTick()) + " mB/t (" + pct(d.waterFrac()) + "%)", d.waterFrac() < 0.9f ? C_WARN : C_TEXT);
        y = row(g, x, right, y, "gui.rbmk.steam", Math.round(d.steamPerTick()) + " mB/t", C_TEXT);
        y = row(g, x, right, y, "gui.rbmk.fe", formatFe(d.fePerTick()) + " FE/t", d.fePerTick() > 0 ? C_OK : C_TEXT);

        // Cảnh báo (ưu tiên cái nguy hiểm nhất)
        Component warn = null;
        if (d.az5()) warn = Component.translatable("gui.rbmk.warn_az5");
        else if (d.formed() && d.coolantPorts() == 0) warn = Component.translatable("gui.rbmk.warn_no_coolant");
        else if (d.formed() && d.outputPorts() == 0) warn = Component.translatable("gui.rbmk.warn_no_output");
        else if (ormLow) warn = Component.translatable("gui.rbmk.warn_orm");
        else if (running && d.waterFrac() < 0.9f) warn = Component.translatable("gui.rbmk.warn_water");
        else if (d.backpressure() > 0.1f) warn = Component.translatable("gui.rbmk.warn_steam");
        else if (tFrac > 0.8f) warn = Component.translatable("gui.rbmk.warn_temp");
        if (warn != null) {
            g.drawWordWrap(font, warn, x, 136, right - x, C_WARN);
        } else {
            g.drawString(font, Component.translatable(d.manual() ? "gui.rbmk.hint_manual" : "gui.rbmk.hint_redstone"),
                    x, 138, C_DIM, false);
        }
    }

    private int row(GuiGraphics g, int x, int right, int y, String labelKey, String value, int color) {
        g.drawString(font, Component.translatable(labelKey), x, y, C_DIM, false);
        g.drawString(font, value, right - font.width(value), y, color, false);
        return y + 11;
    }

    private int bar(GuiGraphics g, int x, int right, int y, float frac, int color) {
        g.fill(x, y - 1, right, y + 2, 0xFF0B0E13);
        int w = Math.round((right - x) * Mth.clamp(frac, 0f, 1f));
        if (w > 0) g.fill(x, y - 1, x + w, y + 2, color);
        return y + 6;
    }

    private void drawMap(GuiGraphics g, ReactorStatusPayload d, int mx, int my) {
        int n = d.types().length;
        int span = 2 * d.radius() + 1;
        float step = Math.min(MAP_SIZE / (float) span, MAP_SIZE / (span * 0.87f));
        float cx = MAP_X + MAP_SIZE / 2f;
        float cy = MAP_Y + MAP_SIZE / 2f;
        int size = Math.max(3, (int) Math.floor(step) - 1);
        int hovered = -1;

        for (int i = 0; i < n; i++) {
            int q = d.qs()[i];
            int r = d.rs()[i];
            int px = Math.round(cx + (q + r / 2f) * step - size / 2f);
            int py = Math.round(cy + r * step * 0.87f - size / 2f);
            ChannelType type = ChannelType.byId(d.types()[i]);
            g.fill(px, py, px + size, py + size, cellColor(type, d, i));
            if (type == ChannelType.ROD) {
                int inner = Math.max(1, Math.round(size * 0.6f * d.rod()));
                int o = (size - inner) / 2;
                g.fill(px + o, py + o, px + o + inner, py + o + inner, 0xFFE8DCC0);
            }
            if (mx >= px && mx < px + size && my >= py && my < py + size) {
                hovered = i;
                g.renderOutline(px - 1, py - 1, size + 2, size + 2, 0xFFFFFFFF);
            }
        }
        if (hovered >= 0) {
            List<Component> tip = new ArrayList<>();
            ChannelType type = ChannelType.byId(d.types()[hovered]);
            tip.add(Component.translatable("gui.rbmk.cell_" + type.name().toLowerCase()).withStyle(ChatFormatting.GOLD));
            tip.add(Component.translatable("gui.rbmk.cell_pos", d.qs()[hovered], d.rs()[hovered]).withStyle(ChatFormatting.GRAY));
            if (type == ChannelType.FUEL) {
                tip.add(Component.translatable("gui.rbmk.cell_temp", Math.round(d.temps()[hovered])));
                tip.add(Component.translatable("gui.rbmk.cell_flux", pct(d.fluxes()[hovered])));
            }
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private static int cellColor(ChannelType type, ReactorStatusPayload d, int i) {
        return switch (type) {
            case FUEL -> tempColor(d.temps()[i] / d.ruptureTemp());
            case GRAPHITE -> 0xFF3A3A3E;
            case WATER -> 0xFF7FC8F0;
            case ROD -> 0xFF0E0E0E;
        };
    }

    /** Xanh dương (nguội) → xanh lá → vàng → đỏ (sắp vỡ kênh). */
    private static int tempColor(float frac) {
        float f = Mth.clamp(frac, 0f, 1f);
        float hue = (1f - f) * 0.62f;
        return 0xFF000000 | Mth.hsvToRgb(hue, 0.85f, 0.95f);
    }

    private static long pct(float v) {
        return Math.round(v * 100.0);
    }

    private static String formatFe(float fe) {
        if (fe >= 1_000_000f) return String.format("%.2fM", fe / 1_000_000f);
        if (fe >= 10_000f) return String.format("%.1fk", fe / 1000f);
        return String.valueOf(Math.round(fe));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}

package com.phat.rbmk.client;

import com.phat.rbmk.block.entity.FuelInputHandler;
import com.phat.rbmk.block.entity.SpentOutputHandler;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * GUI bộ điều khiển.
 * Trên: bản đồ lõi (trái) + số liệu và đồng hồ Nước / Hơi / FE (phải).
 * Giữa: hàng điều khiển. Dưới: kho nhiên liệu + túi đồ.
 */
public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {
    private static final int W = 372;
    private static final int H = 318;
    private static final int MAP_X = 8;
    private static final int MAP_Y = 20;
    private static final int MAP_SIZE = 176;
    private static final int PX = 194;          // cột số liệu
    private static final int PR = W - 8;        // mép phải số liệu
    private static final int CTRL_Y = 201;

    private static final int C_BG = 0xF0141820;
    private static final int C_FRAME = 0xFF3C4452;
    private static final int C_PANEL = 0xFF1E2430;
    private static final int C_TEXT = 0xFFE6E6E6;
    private static final int C_DIM = 0xFF9AA3B2;
    private static final int C_WARN = 0xFFFF5555;
    private static final int C_OK = 0xFF55FF77;
    private static final int C_WATER = 0xFF3B82F6;
    private static final int C_STEAM = 0xFFD8DEE9;
    private static final int C_FE = 0xFFFF3B3B;

    private Button minus;
    private Button plus;
    private Button mode;
    private Button az5;
    private Button reset;

    // vùng hover của đồng hồ để hiện tooltip số chính xác
    private final int[][] gaugeRects = new int[3][4];

    public ReactorControllerScreen(ReactorControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + CTRL_Y;
        int x = leftPos + 8;
        minus = addRenderableWidget(Button.builder(Component.literal("-"), b -> click(Screen.hasShiftDown()
                        ? ReactorControllerMenu.BTN_MINUS_SMALL : ReactorControllerMenu.BTN_MINUS))
                .bounds(x, y, 20, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.rbmk.tip_minus"))).build());
        plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> click(Screen.hasShiftDown()
                        ? ReactorControllerMenu.BTN_PLUS_SMALL : ReactorControllerMenu.BTN_PLUS))
                .bounds(x + 102, y, 20, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.rbmk.tip_plus"))).build());
        mode = addRenderableWidget(Button.builder(Component.translatable("gui.rbmk.mode_redstone"), b -> click(ReactorControllerMenu.BTN_MODE))
                .bounds(x + 128, y, 74, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.rbmk.tip_mode"))).build());
        az5 = addRenderableWidget(Button.builder(Component.literal("AZ-5").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        b -> click(ReactorControllerMenu.BTN_AZ5))
                .bounds(x + 208, y, 64, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.rbmk.tip_az5"))).build());
        reset = addRenderableWidget(Button.builder(Component.translatable("gui.rbmk.reset"), b -> click(ReactorControllerMenu.BTN_RESET))
                .bounds(x + 276, y, 80, 18).build());
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

    // ------------------------------------------------------------------ nền

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, C_FRAME);
        g.fill(x, y, x + W, y + H, C_BG);
        g.fill(x + MAP_X - 2, y + MAP_Y - 2, x + MAP_X + MAP_SIZE + 2, y + MAP_Y + MAP_SIZE + 2, C_PANEL);
        g.fill(x + PX - 4, y + MAP_Y - 2, x + W - 4, y + MAP_Y + MAP_SIZE + 2, C_PANEL);
        g.fill(x + 6, y + 223, x + W - 6, y + 224, C_FRAME);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            int border = slot.index < FuelInputHandler.SLOTS ? 0xFF2F6FD6
                    : slot.index < FuelInputHandler.SLOTS + SpentOutputHandler.SLOTS ? 0xFF9A5A2A : 0xFF4A5261;
            g.fill(sx, sy, sx + 18, sy + 18, border);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0F1218);
        }
        g.drawString(font, "→", x + 61, y + ReactorControllerMenu.INV_TOP + 22, C_DIM, false);
    }

    // ------------------------------------------------------------------ chữ + bản đồ

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFFFAA00, false);
        int labelY = ReactorControllerMenu.INV_TOP - 11;
        g.drawString(font, Component.translatable("gui.rbmk.fuel_in"), ReactorControllerMenu.FUEL_X - 1, labelY, 0xFF6EA8FF, false);
        g.drawString(font, Component.translatable("gui.rbmk.fuel_out"), ReactorControllerMenu.SPENT_X - 1, labelY, 0xFFE0A060, false);
        g.drawString(font, playerInventoryTitle, ReactorControllerMenu.PLAYER_X - 1, labelY, C_DIM, false);

        ReactorStatusPayload d = data();
        if (d == null) {
            g.drawString(font, Component.translatable("gui.rbmk.waiting"), MAP_X + 4, MAP_Y + 4, C_DIM, false);
            return;
        }

        // Đầu ra FE nổi bật ở góc phải
        String out = "⚡ " + fmt(d.fePerTick()) + " FE/t";
        g.drawString(font, out, PR - font.width(out), 6, d.fePerTick() > 0 ? C_OK : C_DIM, false);

        // Mục tiêu công suất giữa nút − và +
        String target = Component.translatable("gui.rbmk.target", pct(d.setpoint())).getString();
        int tx = 8 + 20 + (82 - font.width(target)) / 2;
        g.drawString(font, target, tx, CTRL_Y + 5, d.manual() ? 0xFFFFD166 : C_TEXT, false);

        if (!d.formed()) {
            g.drawString(font, Component.translatable("status.rbmk.not_formed"), MAP_X + 4, MAP_Y + 4, C_WARN, false);
            g.drawWordWrap(font, d.message(), MAP_X + 4, MAP_Y + 18, MAP_SIZE - 8, C_DIM);
        } else {
            drawMap(g, d, mouseX - leftPos, mouseY - topPos);
        }
        drawStats(g, d);
        drawGaugeTooltip(g, d, mouseX - leftPos, mouseY - topPos);
    }

    private void drawStats(GuiGraphics g, ReactorStatusPayload d) {
        int x = PX;
        int y = MAP_Y + 2;
        if (d.formed()) {
            g.drawString(font, Component.translatable("gui.rbmk.core_size", d.radius(), d.height()), x, y, C_DIM, false);
        }
        y += 12;

        y = row(g, y, "gui.rbmk.power", pct(d.power()) + "% → " + pct(d.setpoint()) + "%", d.power() > 1.6f ? C_WARN : C_TEXT);
        y = bar(g, y, d.power() / 1.5f, d.power() > 1.2f ? 0xFFFFAA00 : 0xFF3B82F6);

        float tFrac = d.maxTemp() / d.ruptureTemp();
        y = row(g, y, "gui.rbmk.temp", Math.round(d.maxTemp()) + " / " + Math.round(d.ruptureTemp()) + "°C", tFrac > 0.8f ? C_WARN : C_TEXT);
        y = bar(g, y, tFrac, tempColor(tFrac));

        boolean running = d.power() > 0.02f;
        boolean ormLow = running && d.rod() < d.ormWarning();
        y = row(g, y, "gui.rbmk.rods", pct(d.rod()) + "%   " + pct(d.voidFrac()) + "% | " + String.format("%.2f", d.xenon()),
                ormLow ? C_WARN : C_TEXT);
        y = row(g, y, "gui.rbmk.fuel", d.loadedFuel() + " / " + d.fuelSlots(), d.loadedFuel() == 0 ? C_WARN : C_TEXT);
        y += 3;

        // Đồng hồ Nước / Hơi / FE
        y = gauge(g, 0, y, "gui.rbmk.water", fmt(d.waterPerTick()) + " mB/t",
                frac(d.waterStored(), d.waterCapacity()), d.waterFrac() < 0.9f && running ? C_WARN : C_WATER);
        y = gauge(g, 1, y, "gui.rbmk.steam", fmt(d.steamPerTick()) + " mB/t",
                frac(d.steamStored(), d.steamCapacity()), C_STEAM);
        y = gauge(g, 2, y, "gui.rbmk.fe_buffer", fmt(d.feStored()) + " FE",
                frac(d.feStored(), d.feCapacity()), C_FE);

        // Một dòng cảnh báo / gợi ý
        Component warn = warning(d, running, ormLow, tFrac);
        int wy = MAP_Y + MAP_SIZE - 20;
        if (warn != null) {
            g.drawWordWrap(font, warn, x, wy, PR - x, C_WARN);
        } else if (d.formed() && d.loadedFuel() > 0 && d.setpoint() > 0 && d.maxTemp() < 280f) {
            g.drawWordWrap(font, Component.translatable("gui.rbmk.heating", Math.round(d.maxTemp())), x, wy, PR - x, 0xFFFFD166);
        } else {
            g.drawWordWrap(font, Component.translatable(d.manual() ? "gui.rbmk.hint_manual" : "gui.rbmk.hint_redstone"),
                    x, wy, PR - x, C_DIM);
        }
    }

    @Nullable
    private static Component warning(ReactorStatusPayload d, boolean running, boolean ormLow, float tFrac) {
        if (d.az5()) {
            return d.message().getString().isEmpty() ? Component.translatable("gui.rbmk.warn_az5")
                    : Component.translatable("gui.rbmk.warn_az5_auto", d.message());
        }
        if (d.formed() && d.coolantPorts() == 0) return Component.translatable("gui.rbmk.warn_no_coolant");
        if (d.formed() && d.outputPorts() == 0) return Component.translatable("gui.rbmk.warn_no_output");
        if (tFrac > 0.8f) return Component.translatable("gui.rbmk.warn_temp");
        if (running && d.waterFrac() < 0.9f) return Component.translatable("gui.rbmk.warn_water");
        if (d.backpressure() > 0.1f) return Component.translatable("gui.rbmk.warn_steam");
        if (d.backpressure() < -0.05f) return Component.translatable("gui.rbmk.warn_venting", pct(-d.backpressure()));
        if (ormLow && d.maxTemp() >= 280f) return Component.translatable("gui.rbmk.warn_orm");
        return null;
    }

    private int row(GuiGraphics g, int y, String labelKey, String value, int color) {
        g.drawString(font, Component.translatable(labelKey), PX, y, C_DIM, false);
        g.drawString(font, value, PR - font.width(value), y, color, false);
        return y + 11;
    }

    private int bar(GuiGraphics g, int y, float frac, int color) {
        g.fill(PX, y - 1, PR, y + 2, 0xFF0B0E13);
        int w = Math.round((PR - PX) * Mth.clamp(frac, 0f, 1f));
        if (w > 0) g.fill(PX, y - 1, PX + w, y + 2, color);
        return y + 6;
    }

    private int gauge(GuiGraphics g, int idx, int y, String labelKey, String value, float frac, int color) {
        g.drawString(font, Component.translatable(labelKey), PX, y, C_DIM, false);
        g.drawString(font, value, PR - font.width(value), y, C_TEXT, false);
        int by = y + 10;
        g.fill(PX, by, PR, by + 6, 0xFF0B0E13);
        int w = Math.round((PR - PX - 2) * Mth.clamp(frac, 0f, 1f));
        if (w > 0) g.fill(PX + 1, by + 1, PX + 1 + w, by + 5, color);
        gaugeRects[idx] = new int[]{PX, y, PR, by + 6};
        return by + 10;
    }

    private void drawGaugeTooltip(GuiGraphics g, ReactorStatusPayload d, int mx, int my) {
        for (int i = 0; i < 3; i++) {
            int[] r = gaugeRects[i];
            if (mx < r[0] || mx >= r[2] || my < r[1] || my >= r[3]) continue;
            List<Component> tip = new ArrayList<>();
            switch (i) {
                case 0 -> {
                    tip.add(Component.translatable("gui.rbmk.water").withStyle(ChatFormatting.AQUA));
                    tip.add(Component.literal(fmt(d.waterStored()) + " / " + fmt(d.waterCapacity()) + " mB"));
                    tip.add(Component.translatable("gui.rbmk.tip_water_use", fmt(d.waterPerTick()), pct(d.waterFrac()))
                            .withStyle(ChatFormatting.GRAY));
                    tip.add(Component.translatable("gui.rbmk.tip_water_how").withStyle(ChatFormatting.DARK_GRAY));
                }
                case 1 -> {
                    tip.add(Component.translatable("gui.rbmk.steam").withStyle(ChatFormatting.WHITE));
                    tip.add(Component.literal(fmt(d.steamStored()) + " / " + fmt(d.steamCapacity()) + " mB"));
                    tip.add(Component.translatable("gui.rbmk.tip_steam_how").withStyle(ChatFormatting.DARK_GRAY));
                }
                default -> {
                    tip.add(Component.translatable("gui.rbmk.fe_buffer").withStyle(ChatFormatting.RED));
                    tip.add(Component.literal(fmt(d.feStored()) + " / " + fmt(d.feCapacity()) + " FE"));
                    tip.add(Component.translatable("gui.rbmk.tip_fe_in", fmt(d.fePerTick())).withStyle(ChatFormatting.GREEN));
                    tip.add(Component.translatable("gui.rbmk.tip_fe_out", fmt(d.feOutPerTick())).withStyle(ChatFormatting.GOLD));
                    tip.add(Component.translatable("gui.rbmk.tip_fe_how").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            g.renderComponentTooltip(font, tip, mx, my);
            return;
        }
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

    private static int tempColor(float frac) {
        float f = Mth.clamp(frac, 0f, 1f);
        return 0xFF000000 | Mth.hsvToRgb((1f - f) * 0.62f, 0.85f, 0.95f);
    }

    private static float frac(long v, long cap) {
        return cap <= 0 ? 0f : (float) ((double) v / cap);
    }

    private static long pct(float v) {
        return Math.round(v * 100.0);
    }

    private static String fmt(double v) {
        double a = Math.abs(v);
        if (a >= 1e12) return String.format("%.2fT", v / 1e12);
        if (a >= 1e9) return String.format("%.2fB", v / 1e9);
        if (a >= 1e6) return String.format("%.2fM", v / 1e6);
        if (a >= 1e4) return String.format("%.1fk", v / 1e3);
        return String.valueOf(Math.round(v));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}

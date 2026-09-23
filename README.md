# RBMK Reactor — mod lò phản ứng cho ATM10

Lò phản ứng kiểu RBMK (Chernobyl) dạng multiblock lục giác cho **Minecraft 1.21.1 / NeoForge**.
End game generator: công suất cao, nhưng **setup sai là nổ**.

> Phiên bản: **0.1.0** (bản chạy thử đầu tiên)

---

## 1. Build file .jar

Mã nguồn này **chưa được compile thử** (môi trường tạo ra nó không truy cập được Maven của NeoForge
và máy chủ của Mojang). Lần build đầu tiên có thể báo vài lỗi nhỏ — dán log lỗi là sửa được nhanh.

Yêu cầu: **JDK 21** (ví dụ Eclipse Temurin 21) và có Internet.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Lần đầu Gradle sẽ tải Gradle 9.2.1, NeoForge 21.1.251 và Minecraft 1.21.1 (vài trăm MB).
File jar nằm ở: `build/libs/rbmk-0.1.0.jar` → chép vào thư mục `mods/` của instance ATM10.

Chạy thử trong môi trường dev (không cần ATM10):

```bash
gradlew runClient
```

Muốn thử chế độ ATM trong dev: thả jar của Mekanism, Mekanism Generators, Extreme Reactors,
Modern Industrialization, Oritech, Allthemodium vào `run/mods/` rồi chạy lại.

---

## 2. Cấu trúc lò

Controller đặt ở **tâm lớp nắp trên cùng**. Lò gồm 3 phần theo chiều dọc:

| Lớp | Nội dung |
|---|---|
| Nắp (trên cùng) | Tâm = **Reactor Controller**. Còn lại: **Upper Shield "Elena"**, **AZ-5 Panel**, hoặc **Port** |
| Lõi (2–10 lớp) | Vòng trong: các **cột** Graphite / Fuel Channel / Control Rod / Water Channel. Vòng ngoài cùng: **Shield Casing** hoặc **Port** |
| Đáy | **Shield Casing** hoặc **Port** |

Mỗi cột trong lõi phải **cùng một loại từ trên xuống dưới**.

### Hình lục giác trên lưới vuông

Một ô `(dx, dz)` so với controller thuộc lò nếu `max(|dx|, |dz|, |dx + dz|) ≤ R + 1`.
Vòng `R + 1` là tường. Hình ra là lục giác có 2 cạnh chéo. Ví dụ một lớp lõi bán kính **R = 4**:

```
      x: -5 -4 -3 -2 -1 +0 +1 +2 +3 +4 +5
z-5       .  .  .  .  .  S  S  S  S  S  S
z-4       .  .  .  .  S  F  F  G  F  F  S
z-3       .  .  .  S  F  G  F  F  R  F  S
z-2       .  .  S  G  F  F  G  F  F  G  S
z-1       .  S  F  F  R  F  F  G  F  F  S
z+0       S  F  G  F  F  W  F  F  G  F  S
z+1       S  F  F  G  F  F  R  F  F  S  .
z+2       S  G  F  F  G  F  F  G  S  .  .
z+3       S  F  R  F  F  G  F  S  .  .  .
z+4       S  F  F  G  F  F  S  .  .  .  .
z+5       S  S  S  S  S  S  .  .  .  .  .
```

`S` = Shield Casing (tường), `F` = Fuel Channel, `G` = Nuclear Graphite, `R` = Control Rod, `W` = Water Channel.
Lớp nắp và lớp đáy phủ kín toàn bộ hình (cả vòng `S`).

### Kích thước khuyến nghị

Mô phỏng cho thấy lò quá nhỏ **không đạt tới hạn** vì neutron thoát ra ngoài quá nhiều:

| Bán kính | Kết quả (layout ~65% nhiên liệu) |
|---|---|
| 2 | Không bao giờ tới hạn |
| 3 | Khởi động được khi nguội, **không giữ được 100%** khi nóng |
| **4–5** | **Khuyến nghị** — ổn định, dễ lái |
| 6–7 | Công suất lớn, dư phản ứng nhiều → **nguy hiểm hơn** nếu rút hết rod |

Nhiều graphite quá thì ít nhiên liệu → yếu; quá ít rod thì **không tắt được lò**.

---

## 3. Vận hành

| Thao tác | Tác dụng |
|---|---|
| Redstone vào Controller (0–15) | Ở chế độ Redstone: mức công suất mong muốn, 0 = tắt, 10 = 100%, 15 = 150%. Bấm ±10% trong GUI sẽ chuyển sang chế độ Thủ công |
| Chuột phải Controller | Mở **GUI**: bản đồ lõi tô màu theo nhiệt độ (rê chuột vào ô để xem chi tiết), số liệu, nút −10% / +10%, chuyển Redstone ↔ Thủ công, AZ-5, Reset AZ-5 |
| Shift + chuột phải Controller | Reset AZ-5 (chỉ khi rod đã cắm hết) |
| Comparator từ Controller | Nhiệt độ cao nhất / ngưỡng vỡ kênh × 15 |
| Chuột phải / xung redstone vào AZ-5 Panel | **SCRAM** — cắm toàn bộ rod |
| Bỏ Fuel Assembly vào ô **Nạp nhiên liệu** trong GUI Controller | Controller tự nạp vào mọi Fuel Channel còn trống (mỗi giây), kể cả khi lò đang chạy |
| Ô **Đã cháy** trong GUI | Controller tự rút bó đã cháy ra đây. Hết chỗ thì kênh đó giữ bó cũ và không được nạp mới |
| Ống / phễu vào Controller | Chỉ đẩy được nhiên liệu mới vào; rút ra chỉ lấy được bó đã cháy |

**Port:**
- **Coolant Port** — bơm nước vào (64 000 mB). Lò tiêu thụ khoảng **20 mB/tick cho mỗi khối nhiên liệu** ở 100%.
- **Steam Port** — xuất hơi `rbmk:steam` (tag `c:steam`), tự đẩy sang ống/turbine kề bên.
- **Energy Port** — đổi hơi thành FE trực tiếp (mặc định 20 FE/mB ≈ 400 FE/t mỗi khối nhiên liệu ở 100%), tự đẩy FE ra xung quanh.

Hơi được ưu tiên cho Energy Port, sau đó Steam Port. **Hơi không thoát được = áp suất ngược = lò mất làm mát.**

---

## 4. Những cách làm nổ lò

| Nguyên nhân | Chuyện gì xảy ra |
|---|---|
| **Thiếu nước** | Nhiệt tăng dần → một kênh vượt 950 °C trong 2 giây → **vỡ kênh**: cột thành corium, nổ nhỏ, nắp phía trên bật lên |
| **Hơi không thoát** | Như thiếu nước |
| **Void coefficient dương** | Nước bốc hơi → hấp thụ ít neutron hơn → công suất tăng → càng bốc hơi. Mạnh nhất ở vùng ~280–340 °C (công suất thấp) |
| **Hố xenon** | Giảm công suất đột ngột → xenon tích tụ → bộ điều chỉnh rút rod ra gần hết để bù |
| **AZ-5 khi rod rút hết** | Đầu graphite của rod vào trước, đẩy nước ra → **tăng phản ứng** vài giây. Rod càng gần 0% thì cú tăng càng mạnh: từ ~15% công suất tăng khoảng 4 lần, từ ~5% tăng hơn 30 lần → **nổ hơi** |

**Meltdown** (công suất > 5× danh định hoặc lõi > 1500 °C): nổ lớn, nắp "Elena" bay lên,
lõi thành **corium**, graphite bốc cháy, người chơi xung quanh bị nhiễm xạ.

Tái hiện Chernobyl: chạy 100% → hạ xuống 10% (hố xenon) → đòi lại 100% → rod bị rút hết → bấm AZ-5. 💥

---

## 5. Recipe

Hai chế độ, chỉ một bộ được load (condition `rbmk:pack_mode`):

- **ATM** — khi có đủ Mekanism, Mekanism Generators, Extreme Reactors, Modern Industrialization, Oritech, Allthemodium.
  Recipe base bị tắt. Nguyên liệu trung gian làm trong máy của mod khác (PRC của Mekanism; EBF, Compressor, Assembler của MI).
- **Standalone** — thiếu bất kỳ mod nào ở trên. Recipe vanilla nhưng đắt (nether star, heavy core, netherite, echo shard).

Ép chế độ trong `config/rbmk-common.toml`: `recipeMode = "auto" | "atm" | "standalone"`.

Mọi ID item của mod khác đã được đối chiếu với mã nguồn trên GitHub của từng mod (nhánh 1.21).
Recipe máy của mod khác mà sai format thì bị bỏ qua và ghi log, **không làm crash game**.

---

## 6. Config

`<world>/serverconfig/rbmk-server.toml`:

| Mục | Mặc định | Ý nghĩa |
|---|---|---|
| `hazards.explosionsEnabled` | true | Cho phép nổ |
| `hazards.blockDamage` | true | Nổ có phá block |
| `hazards.steamVent` | true | Hơi không thoát được thì xả bỏ (mất FE nhưng lò vẫn mát). false = nghẽn hơi, lò nóng lên |
| `hazards.autoProtection` | true | Tự bấm AZ-5 khi thiếu nước / nghẽn hơi 3 giây, hoặc kênh > 85% ngưỡng vỡ |
| `hazards.autoScramOnUnload` | true | Chunk unload → tắt lò an toàn |
| `hazards.maxExplosionPower` | 14 | Sức nổ tối đa (TNT = 4) |
| `hazards.ruptureTemp` | 950 | °C vỡ kênh |
| `hazards.meltdownTemp` | 1500 | °C meltdown |
| `hazards.criticalPowerMultiplier` | 5 | Công suất > N × danh định → nổ |
| `structure.maxRadius` / `maxHeight` | 7 / 10 | Kích thước tối đa |
| `structure.simInterval` | 5 | Tick giữa mỗi bước mô phỏng |
| `balance.heatPerMb` | 2.5 | Nhiệt cho 1 mB hơi |
| `balance.fePerMbSteam` | 20 | FE mỗi mB hơi ở Energy Port |
| `balance.fuelBurnSeconds` | 7200 | Giây chạy 100% để cạn 1 bó nhiên liệu |

---

## 7. Chưa có trong 0.1.0

Peripheral CC: Tweaked, xuất hơi dạng chemical / phóng xạ của Mekanism,
Refueling Machine, Reprocessing Port, nhóm rod riêng.

Thư mục `tools/` chứa script Python dùng để chỉnh hằng số vật lý (`ReactorSim.java` là bản port 1:1).

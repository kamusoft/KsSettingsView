# UI Brief: fix-cell-icon-size-parity

## 画面と状態

この change は新しい画面を作らない。対象は共通行レイアウトの icon 領域で、確認に使う既存のデモ画面は次の 2 つ:

- **Section 装飾デモ (style 切替)** (`samples/ios` / `samples/android` の `SectionDecorationDemo*`): Modern・既定プリセットで 1 番目の Section が icon 付き Cell 4 行 (機内モード / Wi-Fi / Bluetooth / バッテリー)。`SampleTheme.sectionDecorationDemo` が `cellIconSize` / `cellIconRadius` を両 OS に同値で渡す。**Android の A/B の主舞台**
- **共通フィールドデモ** (`UnifyCellCommonFieldsDemo*`): iOS が `KsImage.systemName` (SF Symbols) を複数行に渡す画面。字形ごとに intrinsic 幅が違う。**iOS の A/B の主舞台** (title の開始位置が行間で揃うか)

- **基本 Cell デモ / 入力 Cell デモ** (`BasicCellsDemo*` / `InputCellsDemo*`): 長い valueText を持つ行 (SSID 相当) と EntryCell の配分を、両 OS のフォントスケール最大で確認する補助画面。修正後は両 OS とも title が残り valueText が省略される (core/ADR-0026)。Android は修正前 (title が潰れる) との A/B を撮る

状態は通常表示と、フォントスケール最大 (iOS: Dynamic Type アクセシビリティサイズ / Android: 表示サイズ・フォントサイズ最大) での狭幅表示の 2 つ (loading / empty / error は対象外)。

## リファレンス注釈

- `references/ios-modern-standard-before.png` — iOS の現行描画 (implement-modern-style の証跡から転載)。**採用**: icon 領域が Theme の指定どおりの正方形枠で角丸になっていること、4 行の icon 幅と title 開始位置が揃っていること。**対象外**: Section の箱・余白・separator・switch・chevron (implement-modern-style で照合済み)
- 共通フィールドデモ (SF Symbols) の修正前画像は実装フェーズで撮影し、title 開始位置の基準線を注釈した比較画像を `verification/ios-common-fields-compare-annotated.png` として残す (承認モックの代わりに「行間で基準線が一致する」を照合条件にする)
- `references/android-modern-standard-before.png` — Android の**修正前**描画 (同上から転載)。**見るべき差**: icon が iOS より小さく、角丸がかかっていない。それ以外 (箱・余白・separator) は本 change の対象外

## デザイントークン参照

- icon size / radius の解決順と既定: [concepts/core/styling/style-resolution.md](../../../concepts/core/styling/style-resolution.md) (特殊な解決規則)
- 標準 icon 枠: [concepts/core/styling/cell-row-layout.md](../../../concepts/core/styling/cell-row-layout.md) (platform 別の行寸法)
- 角丸の適用先 (正方形枠) の契約: [core/ADR-0025](../../../decisions/core/0025-cell-icon-radius-applies-to-square-frame.md)
- デモ Theme の値は `samples/*/SampleTheme` と `SampleIconBadge` が持つ (本 brief に生値は書かない)

## 承認モック

`mock/approved.png` を採用 — 新規 HTML モックは作らず、iOS の現行描画 (`references/ios-modern-standard-before.png` と同一) を「見た目の正」とする。Android はこれに icon 領域の寸法・角丸を揃え、iOS は SF Symbols 指定時にもこの描画と同じく icon 列幅が揃うことを正とする (提案時に合意: 2026-08-22)。

## 視覚照合の結果

照合日: 2026-08-23 / 結果: **合格** (両 OS とも承認モックの条件を満たす)

### Android (Pixel 6a 実機 / API 36 / density 2.625x)

- `verification/android-modern-standard-after.png` — icon 枠 76px = 28.95dp の正方形 (Theme 指定 29)、4 行とも同寸法。角丸は枠にかかる。title 左端 77.7〜78.5dp
- `approved.png` (iOS 実測 29.0pt / title 左端 77.3〜78.7pt) と一致。修正前は 24dp・角丸なし・title 左端 72.8dp で iOS と 5dp ずれていた
- `verification/android-overflow-long-value-before.png` / `verification/android-overflow-long-value.png` (font_scale 2.0 = 端末最大) — 修正前は Wi-Fi 行の title が幅 0 で完全消失していたが、修正後は title 全文 + valueText 末尾省略。icon 枠はフォント最大でも 76px のまま、chevron の幅も不変 (core/ADR-0026 の契約どおり)

### iOS (iPhone 17 / iOS 26.0 Simulator @3x)

- `verification/ios-common-fields-compare-annotated.png` — SF Symbols 9 行の title 開始位置のばらつきが **19px (6.3pt) → 6px (2.0pt)**。基準線 x = 56.7pt (左マージン 16 + icon 枠 24 + spacing 16) に収束
- `verification/ios-modern-standard-after.png` — `approved.png` と全画素比較して差分はステータスバーの時刻と Wi-Fi 行の valueText 文言のみ。**退行ゼロ**
- `verification/ios-overflow-long-value.png` (Dynamic Type AX5) — title 全文 + valueText 末尾省略、icon 枠 4 行とも 29.0pt、chevron 健在。Android の同条件証跡と並置して同じ配分であることを確認
- `verification/ios-test-constraints.log` — テスト実行中および Simulator system log に `Unable to simultaneously satisfy constraints` は 0 件

### 証跡が対応する実装範囲

`verification/` の PNG 8 点は 2026-08-23 のレビュー前実装で撮影した。以後に入った修正は (1) Android の icon 角丸を `IconFrameOutlineProvider` のインスタンス差し替え方式へ変えたこと、(2) iOS の icon 表示 API を `showIcon(size:)` / `hideIcon()` の 2 入口へ分けたこと、(3) テストコードとコメントの是正のみで、いずれも描画結果を変えないため再撮影していない (レビュー側も同判定。`review-002.md` を参照)。`verification/ios-test-constraints.log` のみ修正反映後に再実行し、対応する実装ソースの SHA-256 を併記している。

### 合意済み妥協

- **角丸半径の実測差 (iOS 7.25pt / Android 7.63dp、実画素 1px 強)**: CoreAnimation の `cornerRadius` と Android の `ViewOutlineProvider` clip でラスタライザとアンチエイリアスが異なることによる差 (別の計測法では大小が逆転する)。実装差ではないため許容する
- **iOS の title 開始位置の残差 2.0pt**: 先頭文字のサイドベアリング差。先頭字が同じ 3 行では修正後 1px の差もなく一致することを実測で確認済み。レイアウトのずれではないため許容する
- **狭幅時に iOS の機内モード行だけ title が上限省略される** (Android は同条件で全文): AX5 の iOS フォントが Android の最大スケールより大きいため。「title は残り幅を使い、超えたら上限で省略」という同一契約の範囲内
- **サンプルの Bluetooth 行のアイコン字形差**・**`approved.png` と現行サンプルの valueText 文言差**: [deviation.md](../deviation.md) の「合意済みの見送り」を参照

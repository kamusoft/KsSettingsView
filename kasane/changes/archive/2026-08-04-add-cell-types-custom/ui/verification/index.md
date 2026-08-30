# 視覚照合・挙動検証の証跡インデックス

`ui/mock/approved.png`（Plan A）と実装スクリーンショットの照合、および実装者が「テストでは実証できていない」と
申告した項目の実機確認の記録。撮影日 2026-08-03（`01`〜`07`）/ 2026-08-04（`08`〜`15`、`compare-*`）。

「照合結果 / トークン候補 / 合意済み妥協」は `ui/brief.md` に記録済み。本ファイルは証跡画像の
キャプション（何を撮ったか / 何が読み取れるか）を担当する。

## 使用環境

| 環境 | 種別 | OS | 用途 |
|---|---|---|---|
| Pixel 6a | Android 実機 | Android 16 | 主。全項目 |
| Pixel 4a | Android 実機 | Android 13 | 副。描画と動的高さ |
| iPhone 17 シミュレータ | iOS simulator | iOS 26.5 | iOS 全項目 |
| pixie4 (iPhone 11) | iOS 実機 | iOS 16.6.1 | アプリ導入のみ（後述の制約で未検証） |
| pixie5 (iPhone 15) | iOS 実機 | iOS 26.5 | アプリ導入のみ（後述の制約で未検証） |

**iOS 実機が未検証な理由**: `mcp__mobile__*` の screenshot / input は実機 iOS ではオンデバイスエージェント
（`mobilecli agent install --provisioning-profile`）の導入が前提で、未導入。ユーザー所有端末への
エージェント追加インストールは承認事項と判断し実施していない。アプリ本体は両実機に導入済みで、
`jp.kamusoft.kssettingsview.samples.ios` を起動すれば目視確認できる状態。

## 証跡一覧

### Android — Pixel 6a (Android 16)

| ファイル | 何が読み取れるか |
|---|---|
| `android-pixel6a-01-top.png` | ①インライン（同期ステータス / 静的キャプション）②SliderCell 3 行 ③動的高さ 2 行 の描画。mock のセクション構成・文言・配色と一致 |
| `android-pixel6a-02-dynamic-height-expanded.png` | 「利用規約」を content 内タップで展開。行高さが伸び、**後続行（プライバシーポリシー行・footer）が押し下がる**（検証項目4） |
| `android-pixel6a-03-slider-enabled-dragged-disabled-blocked.png` | 有効スライダーは 70→23 に追従、`isEnabled=false` の「無効」行は 60 のまま（ドラッグ・タップとも遮断。検証項目5） |
| `android-pixel6a-04-chevron-vs-commandcell.png` | CustomCell(showArrow=true) と CommandCell を隣接配置 |
| `android-pixel6a-04b-chevron-zoom.png` | 上記 chevron 部の 4 倍拡大。bbox 18×30px / x 1014–1031 / 右端余白 48px(=16dp) が両者一致、最大画素差 10・平均 0.269（検証項目2） |
| `android-pixel6a-05-row-tap-count-3.png` | 行本体タップ 3 回でピルが「3 回」（mock と同一表示） |
| `android-pixel6a-06-child-tap-does-not-fire-row-ontap.png` | content 内のピル（子要素）タップで「0 回」にリセット。行 onTap が発火していれば「1 回」になるはずで、**二重発火なし**（検証項目1 相当） |
| `android-pixel6a-07-scroll-recycle-bottom-tap.png` | 40 行を往復フリック後の #29–#40。アクセント 6 色循環・番号・ピルすべて整合。リサイクル後にタップした #35 / #40 にだけ ✓ が付く（**listener の取り違えなし**。検証項目3） |
| `android-pixel6a-08-press-feedback.png` | CustomCell 行の押下中。背景が白 (255,255,255) → 選択色 (252,239,194) に変化し、隣接行は白のまま（検証項目6） |
| `android-pixel6a-08b-press-zoom.png` | chevron 拡大（04b の元データ） |

### Android — Pixel 4a (Android 13)

| ファイル | 何が読み取れるか |
|---|---|
| `android-pixel4a-01-top.png` | Android 13 でも同一の描画 |
| `android-pixel4a-02-dynamic-height-expanded.png` | Android 13 でも展開で行高さと後続行が追従 |

### iOS — iPhone 17 シミュレータ (iOS 26.5)

| ファイル | 何が読み取れるか |
|---|---|
| `ios-sim-iphone17-ios265-01-top.png` | ①②③ の描画。Android と同一の文言・構成・配色（sample-parity） |
| `ios-sim-iphone17-ios265-02-dynamic-height-expanded.png` | 「利用規約」展開で行高さが伸び、後続行が押し下がる |
| `ios-sim-iphone17-ios265-03-chevron-vs-commandcell.png` | CustomCell(showArrow=true) と CommandCell の隣接配置 |
| `ios-sim-iphone17-ios265-03b-chevron-zoom.png` | 上記の 3 倍拡大。bbox 20×35px / x 1136–1155 / 右端余白 50px(=16.7pt) が両者一致、最大画素差 6・平均 0.067（検証項目2） |
| `ios-sim-iphone17-ios265-04-row-tap-count-3.png` | 行本体タップ 3 回で「3 回」 |
| `ios-sim-iphone17-ios265-05-child-tap-does-not-fire-row-ontap.png` | content 内 Button タップで「0 回」。行 onTap は発火せず（検証項目1） |
| `ios-sim-iphone17-ios265-06-scroll-recycle-tap.png` | 40 行往復スクロール後の #13–#26。表示整合、タップした #17 / #22 にだけ ✓ |
| `ios-sim-iphone17-ios265-07-slider-enabled-dragged-disabled-blocked.png` | 有効スライダー 70→31、`isEnabled=false` 行は 60 のまま |

### iOS — 動的高さの展開アニメーション（修正前 / 修正後のフレーム列）

オーナーが実機で指摘した「展開時に content が一度上へ飛び出してから落ちてくる」動きの検証。
「動的高さ」の**利用規約**行を content 内タップで展開し、その瞬間を画面録画（`simctl io recordVideo`、
60fps）してフレームを切り出したもの。画像は行の周辺だけを切り出し、横 700px に縮小してある。
**両者は同じ端末・同じ操作**で、`CustomCellHostedContent` の配置方式だけが異なる。

読み取り方: 「▼ 利用規約（タップで展開）」の**見出しの縦位置**を追う。

| ファイル | 何が読み取れるか |
|---|---|
| `ios-sim-iphone17-ios265-08-expand-before-f0-collapsed.png` | 修正前・タップ直前（折りたたみ）。見出しは行の縦中央 |
| `ios-sim-iphone17-ios265-08-expand-before-f1-overshoot.png` | 修正前・遷移 1 フレーム目。**見出しが f0 より約 32px 上へ飛び出し**、展開本文の末尾が行の下端で切れている |
| `ios-sim-iphone17-ios265-08-expand-before-f2.png` | 修正前・遷移中。見出しが少しずつ下へ戻る |
| `ios-sim-iphone17-ios265-08-expand-before-f3.png` | 修正前・遷移中（同上） |
| `ios-sim-iphone17-ios265-08-expand-before-f4-settled.png` | 修正前・収束。見出しは f1 より約 14px 下。**f0 → 上 → 下と 2 段階に動いている**（＝オーナー指摘の症状） |
| `ios-sim-iphone17-ios265-09-expand-after-f0-collapsed.png` | 修正後・タップ直前（折りたたみ）。修正前 f0 と同一 |
| `ios-sim-iphone17-ios265-09-expand-after-f1.png` | 修正後・遷移 1 フレーム目。見出しは**すでに収束位置**にあり、上への飛び出しがない |
| `ios-sim-iphone17-ios265-09-expand-after-f2.png` | 修正後・遷移中。見出しは動かず、行が下へ伸びて後続行が押し下がるだけ |
| `ios-sim-iphone17-ios265-09-expand-after-f3.png` | 修正後・遷移中（同上） |
| `ios-sim-iphone17-ios265-09-expand-after-f4-settled.png` | 修正後・収束。f1〜f4 で見出しの位置は不変 |

f0 → f1 の見出しの移動（折りたたみ時の縦中央から、展開後の上寄せ位置へ約 18px）は修正後にも残るが、
これは「最低行高さの中で縦中央」と「content が行を満たす」の差であり、1 段階の落ち着きで戻り運動がない。

### iOS — 修正後の静止状態（退行確認）

| ファイル | 何が読み取れるか |
|---|---|
| `ios-sim-iphone17-ios265-10-post-fix-dynamic-height-expanded.png` | 修正後の画面全体。①②③ の描画は `01` / `02` と同一で、配置方式の変更による静止時の見た目の変化がない |
| `ios-sim-iphone17-ios265-11-post-fix-chevron-vs-commandcell.png` | 修正後の CustomCell(showArrow=true) と CommandCell の隣接比較。両者の chevron を実測すると **bbox 20×35px / x 1136–1155 / 右端余白 50px(=16.7pt) が一致**し、`03b` の修正前実測値とも同一。配置方式の変更が chevron に影響していない |

## mock との差異（照合結果）

いずれも「構造・トークン・意図」の一致は保たれている。ピクセル一致は基準にしていない。

1. **Section ヘッダの大文字表記**: mock HTML は iOS 標準リストの自動大文字化を模して
   「インライン CUSTOMCELL」「SHOWARROW / ONTAP」等と書いているが、本ライブラリのヘッダは文字列をそのまま描画する。
   既存デモ（`CommandCell` 等）と揃えて自然な大小混在で実装した。
2. **スライダーの見た目**: mock は丸ノブ + 4px トラック。iOS は SwiftUI `Slider`、Android は Material 3 `Slider` の
   標準描画をそのまま使うため形状が異なる。brief.md が「mock はプラットフォーム中立」と明示しており照合基準外。
3. **無効スライダーの見た目差**: iOS は `.disabled(true)` により SwiftUI が自動的に淡色化するが、
   Android は pointer 消費方式のため淡色化しなかった。**→ 両プラットフォームで content 全体を淡色化して解消済み**
   （下記「Android — 2 巡目」および「両プラットフォーム — 3 巡目」を参照）。
4. **行タップカウンタの初期値**: mock は「3 回」だが、これは実行中の状態。実装の初期値は 0 で、
   3 回タップした状態が mock と一致することを `*-row-tap-count-3.png` で確認した。

## mock に対する意図的な追加（3 点）

いずれも検証を可能にするための追加で、iOS / Android 両方に同一構成で入れてある（sample-parity 準拠）。

1. **「無効」スライダー行（②の 3 行目）** — spec Scenario「無効時は content 内の操作も抑止される」の実地確認用。
2. **「詳細設定（CommandCell）」行（④の 2 行目）** — chevron を既存 Cell と隣接比較するための基準行。
3. **ダミー行のタップで ✓ が付く挙動（⑤）** — リサイクル後の listener の取り違えを可視化するため。
   ピル文言は `#01` / `#01 ✓` の 2 状態のみで、mock の見た目は変えていない。

## SampleTheme に追加した色（生値の二重管理を避けるため一元化）

`demoAccentPalette`（既存 6 色の循環リスト）/ `demoPillBackground` #FAF3D9 /
`demoExpandBackground` #FAF7EE / `demoExpandText` #777777。iOS・Android で同一 RGBA。

## Android — 2 巡目（レビュー指摘修正後の実機確認）

端末: **Pixel 6a / Android 16**（主）、**Pixel 4a / Android 13**（副）。両機とも同一 APK。

### 無効時の淡色化と Slider の見た目

| ファイル | 何が読み取れるか |
|---|---|
| `android-pixel6a-09-disabled-dimmed-m3-slider.png` | ②の「無効」行が、ラベル・スライダー・数値ともに淡色で描画される（有効な「明るさ」「音量」と濃度が明確に異なる）。Slider の色指定を外したため thumb / track が Material 3 の既定色になっている |
| `android-pixel4a-03-disabled-dimmed-m3-slider.png` | Android 13 でも同一（淡色化・M3 既定色ともに再現） |

淡色化の対象は content だけで、行の背景と Disclosure Indicator には掛けていない。
なお、この巡で Slider の色指定を外したことにより Android が M3 既定の紫・iOS がアンバーとなり色がずれた。
次の巡（下記）で両プラットフォームとも `SampleTheme.mauiAccent` に揃えて解消している。

### 押下 feedback（clickable flag）

| ファイル | 何が読み取れるか |
|---|---|
| `android-pixel6a-10-press-feedback-no-ontap.png` | `onTap` を持たない①「同期ステータス」行を押下した瞬間。行全体に `Theme.selectedColor` の feedback が出る（共通行の LabelCell と同じ扱い） |
| `android-pixel6a-11-press-feedback-disabled-none.png` | `isEnabled = false` の「無効」行を押下した瞬間。feedback は出ない |

### 無効行の accessibility 遮断（TalkBack が見るツリー）

`uiautomator dump` で端末上の accessibility ツリーを取得したもの。TalkBack / Switch Access が
操作対象として見るのはこのツリーであり、ポインタ経路とは独立している。

| ファイル | 何が読み取れるか |
|---|---|
| `android-pixel6a-14-a11y-tree-disabled-row.xml` | 有効な 2 行は `android.widget.SeekBar` としてツリーに現れるのに対し、無効行（bounds y 1188–1366）の中身は **SeekBar もテキストも 1 つも現れない**。行の枠だけが `enabled=false clickable=false focusable=false` で残る |
| `android-pixel4a-04-a11y-tree-disabled-row.xml` | Android 13 でも同一（無効行の SeekBar / テキストがツリーに存在しない） |

### 展開 / 折りたたみの遷移（iOS で問題になった飛び出しの有無）

`screenrecord`（60fps）を撮り、`AVAssetImageGenerator` で 16.6ms 刻みに切り出した実フレーム。
元動画は `android-pixel6a-12-expand-collapse-transition.mp4`。

| ファイル | 何が読み取れるか |
|---|---|
| `android-pixel6a-12-expand-f0-1683ms-collapsed.png` | タップ直前。折りたたみ |
| `android-pixel6a-12-expand-f1-1699ms-collapsed.png` | 1 フレーム後。まだ折りたたみ（レイアウトは f0 と同一） |
| `android-pixel6a-12-expand-f2-1716ms-expanded.png` | その次のフレーム。**すでに収束後の展開状態**。中間の高さも、上への飛び出しもない |
| `android-pixel6a-12-expand-f3-1749ms-settled.png` | 以降レイアウトは不変（差分は ripple の減衰のみ） |
| `android-pixel6a-13-collapse-f0-2799ms-expanded.png` | 折りたたみ操作の直前 |
| `android-pixel6a-13-collapse-f1-2832ms-expanded.png` | 1 フレーム前。まだ展開 |
| `android-pixel6a-13-collapse-f2-2849ms-collapsed.png` | その次のフレーム。**すでに収束後の折りたたみ状態** |

結論: Android の展開/折りたたみは**補間アニメーションを持たず 1 フレームで確定する**ため、
iOS で問題になった「上に飛び出してから落ちてくる」中間状態は原理的に発生しない。
`animator_duration_scale = 10`（10 倍遅延）でも中間フレームが 1 枚も観測されないことを別途確認した。

なお content が行高さに収まらない場合の縦位置は、iOS の `CustomCellRowPlacement` と同じ
「収まるときは縦中央 / 収まらないときは上端揃え」に揃えた（`CenterOrTopVertically`）。

## 両プラットフォーム — 3 巡目（無効時の淡色化を iOS へ適用 / Slider のアクセント色を統一）

撮影日 2026-08-04。環境: **iPhone 17 シミュレータ / iOS 26.5**、**Pixel 6a / Android 16**（主）、
**Pixel 4a / Android 13**（副）。Android は両機とも同一 APK。

この巡で入れた変更は 2 点。

- **無効時の淡色化を iOS 側にも適用**（`CustomCellHostedContent`）。`.disabled(true)` は環境値を読む標準
  コントロールしか淡色化しないため、`Text` / `Image` が Android だけ薄く iOS は素の色という非対称が残っていた。
  Android の `Modifier.alpha(0.38f)` と同じ値・同じ適用範囲（content のみ。行背景と Disclosure Indicator は対象外）で
  `opacity` を掛けて揃えた
- **Sample の Slider のアクセント色を統一**。Android の `thumbColor` / `activeTrackColor` を
  `SampleTheme.mauiAccent` に指定し、**active track の色**を iOS の `.tint(SampleTheme.mauiAccent)` と
  一致させた。M3 の標準形状は保っている（thumb をセル背景色に塗って自前描画に見せることはしていない）。

  **thumb と inactive track の色は一致していない**（画像もそう写っている）: SwiftUI の `.tint` は
  active track にしか効かないため iOS の thumb は標準の白い capsule のままで、Android はアンバーの縦バーになる。
  inactive track も未指定のため iOS = システムのグレー / Android = Material 3 の淡紫。いずれも
  OS 標準コントロールの描画差であり、sample-parity 規約が許容する範囲（明示的に渡していないパラメータの既定値差）。
  なお `compare-01` の焼き込みキャプションは「active track / thumb とも mauiAccent」と読める表現になっているが、
  正確には上記のとおり active track のみ一致である

### 証跡

| ファイル | 何が読み取れるか |
|---|---|
| `compare-01-disabled-dimming-and-slider-accent-ios-vs-android.png` | 「再利用（SliderCell ラップ関数）」セクション全体の左右比較。3 行とも active track / thumb が同じアンバーで、「無効」行だけがラベル・スライダー・数値の**すべて**淡色になっている |
| `compare-02-disabled-row-zoom-ios-vs-android.png` | 無効行だけの拡大比較（iOS 上 / Android 下）。ラベル「無効」と数値「60」の濃度が両プラットフォームで一致していることが目視できる |
| `ios-sim-iphone17-ios265-12-disabled-dimmed-content.png` | iOS の画面全体。①②③④ の描画は `01` / `10` と同一で、opacity 追加による他行・他セクションへの影響がない |
| `android-pixel6a-15-disabled-dimmed-accent-slider.png` | Pixel 6a の画面全体。Slider がアンバーになったこと以外は `01` と同一 |
| `android-pixel4a-05-disabled-dimmed-accent-slider.png` | Pixel 4a（Android 13）でも同一（アンバーの Slider・無効行の淡色化ともに再現） |

### 実測値

スクリーンショットの画素を直接サンプルした値。Pixel 6a の画面キャプチャには広色域→sRGB の変換が
掛かるため、Android の絶対値は同一画像内の他のアクセント色（「同期済み」バッジ）と突き合わせて判定した。

| 測定対象 | iOS | Android (Pixel 6a) | 判定 |
|---|---|---|---|
| 有効行のラベル（`mauiDeepText` #555555） | (85,85,85) | (85,85,85) | 一致 |
| **無効行のラベル** | **(190,190,190)** | **(191,191,191)** | **一致**（#555555 を alpha 0.38 で白に載せた計算値 190.4 と合致） |
| 有効行の数値（`mauiFooterText` #999999） | (153,153,153) | (153,153,153) | 一致 |
| **無効行の数値** | **(216,216,216)** | **(217,217,217)** | **一致**（計算値 216.2 と合致） |
| 有効 Slider の active track | (255,191,0) = `mauiAccent` そのもの | 同一画像内の「同期済み」バッジと同色 | 一致 |
| 無効 Slider の active track の実効 alpha | **約 0.19** | **約 0.38** | **iOS が 2 倍薄い**（下記） |

### iOS の標準コントロールが二重に薄くなる度合い（合意済みの副作用）

無効行の Slider の active track は、iOS で (255,243,206)。これを白背景上のアクセント色
(255,191,0) の合成として解くと**実効 alpha ≒ 0.19**。Android の同じ箇所は実効 alpha ≒ 0.38。
つまり **iOS の標準コントロールだけがちょうど 2 倍薄い**（SwiftUI の `.disabled` による淡色化が
約 0.5 を掛け、そこに opacity 0.38 が重なるため）。

見え方としては、iOS の無効 Slider は thumb が白背景にほぼ溶け込み、track の色も「薄いクリーム色」まで
落ちる。Android は thumb と track がまだアンバーだと分かる濃さを保つ。ただし**淡色化の主目的である
「有効行と並べたときに無効だと分かる」ことは両プラットフォームとも達成**しており、テキストの濃度は
実測で一致している。`deviation.md` に記録済みの合意済み差分。

### inactive track の色差（未解消・意図的）

Slider の inactive track は iOS がシステムのグレー、Android が Material 3 の淡紫のままで、
**どちらの Sample も明示指定していない**。sample-parity 規約は「Sample が明示的に渡していない
パラメータの、本体既定値の platform 差」を許容される差異としており、片側だけ明示指定すると
「標準コントロールを素のまま置いた例」という意図が崩れるため、既定のままとした。

## iOS — 4 巡目（折りたたみアニメーションの修正）

オーナー指摘「折りたたみ時にタイトルが一度消え、下から上がってくるように見える」の修正検証。
`CustomCellRowPlacement` の縦中央揃えの基準を「現在の行の高さ」から「定常状態の行の高さ
（`max(content の自然高, 実効行高さ)`、bounds を上限）」に変更した後、iPhone 17 シミュレータ
(iOS 26.5) で「動的高さ」の**利用規約**行を展開→折りたたみし、`simctl io screenshot` の連写
(約 3.5 fps) で遷移フレームを取得したもの。

読み取り方: 「利用規約」の**見出しの縦位置**を追う。遷移中フレームで見出しが行の縦中央
（従来の症状の開始位置）に落ちていないことが確認点。

| ファイル | 何が読み取れるか |
|---|---|
| `ios-sim-iphone17-ios265-13-collapse-postfix-f0-expanded.png` | タップ直前（展開）。見出しは行上端 |
| `ios-sim-iphone17-ios265-13-collapse-postfix-f1-transition-title-anchored.png` | 遷移中。**見出しは行上端の定常位置に留まり**（reconfigure の crossfade で薄くなるのみ）、本文が消えて後続行が上へ詰まっていく。従来の「まだ高い行の縦中央に置かれて下から上がる」動きが無い |
| `ios-sim-iphone17-ios265-13-collapse-postfix-f2-collapsed.png` | 収束（折りたたみ）。見出し位置は遷移中と同一 |
| `ios-sim-iphone17-ios265-14-expand-postfix-f0-collapsed.png` | 展開方向・タップ直前（退行確認） |
| `ios-sim-iphone17-ios265-14-expand-postfix-f1-transition-title-anchored.png` | 展開方向・遷移中。見出しは動かず本文が行の下端でクリップされながら現れる（3 巡目までの修正が保たれている） |
| `ios-sim-iphone17-ios265-14-expand-postfix-f2-expanded.png` | 展開方向・収束 |

証跡の射程: 3 巡目と異なり修正前フレームは撮っていない。また連写は約 3.5 fps のため、
このフレーム列単体では「症状が消えた」と「中間フレームを撮り逃した」を区別できない。
修正前後の差別化はユニットテストが担保する — 配置基準を修正前の「現在の行の高さ」に戻すと
`test_行が実効行高さより高い間は定常高さ基準の縦位置を維持する` だけが失敗することを
レビュー時に A/B 実測済み (review-004)。修正後の配置は行の高さを参照しないため、
中間フレームで縦位置が動かないことは実装から演繹的に決まる。

静止状態の退行が無いことはユニットテストで担保する（固定高行の縦中央 2 件・self-sizing 行の
定常縦中央 1 件・遷移中の上端/縦中央維持 2 件、`CustomCellTests`）。

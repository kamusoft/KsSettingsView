# Exploration: add-cell-types-custom

openspec 時代の未実施 change `openspec/changes/add-cell-types-custom`（review-result_001 で CHANGES_REQUESTED のまま停止）を、現行コードベースに合わせて kasane change として再起票するための探索。

## 課題 / 動機

- 旧 AiForms.Maui.SettingsView の `CustomCell`（任意ビューをセル化）に相当する機能が KsSettingsView に未実装。
- 旧 openspec proposal は現行コードと乖離しており、そのままでは使えない。

## 現状調査の要点（2026-08-03、ksn-scout 2本）

### 現行コードベース側
- CustomCell の実体は完全未着手。ただし Android `ComposeCellViewHolder` は CustomCell を名指しで見越した基底クラスとして既存。`VisibilityAware` も非準拠 Cell 対応済み。
- 旧 proposal の前提3変更（add-declarative-dsl / purify-core-extract-style-to-ui-layer / refactor-accessory-and-root-hf）はすべて実装・アーカイブ済みで前提として有効。
- `KsCellRegistry` は iOS / Android とも単一 `register` API のみ。旧 proposal の 2 系統登録 API は存在しない。
- `KsAnyView` は H/F 装飾領域で完全実装済み（`UIHostingConfiguration` / ComposeView キャッシュ + `DisposeOnDetachedFromWindow` 実運用）。ただし Decision 3 により等価性非参加。
- MAUI 層は実体ゼロ（slnx の殻のみ）。openspec 上に add-maui-bridge / add-maui-core / add-maui-cells の3分割案が未実施のまま残る。

### AiForms.Maui.SettingsView 側
- `CustomCell : CommandCell`。固有プロパティ: `ShowArrowIndicator` / `Content` / `IsSelectable`(既定 true) / `IsMeasureOnce` / `UseFullSize` / `LongCommand`。
- Content 差し込み時は Title/Description/Hint 領域を取り除き中央領域のみ差し替え。`UseFullSize` でアイコン領域も潰す。
- 「Content の NativeView をセル数分保持し仮想化されていない」既知のパフォーマンス課題が TODO 明記 → 引き継がず設計で回避すべき失敗パターン。
- MAUI ハンドラ差し替え・`Parent` ワークアラウンド・ハイライト時背景色退避などは MAUI/UITableViewCell 固有事情で移植不要。

## 検討した選択肢 (却下案と理由を含む)

論点1（コンテンツの持ち方）:
- **A 案（旧 proposal 型）**: Content 型を Registry に事前登録する 2 系統登録 API。→ 却下（実装リスク高・事前登録が冗長・ADR-0013 の既存拡張経路と重複）
- **B 案**: `KsAnyView` を Cell に直接持たせる。→ 却下（等価性非参加により再バインド無駄打ちが構造的に発生、uiKit/AndroidView backing はバインドごとネイティブ View 再生成で AiForms の失敗パターンに近づく）
- **新 C 案（採用）**: content 値 + ビルダクロージャ、等価性は content のみ。静的コンテンツ向けに content 省略の糖衣を提供。

詳細は ADR-0014 参照。

## 決定事項

- 論点1: 新 C 案で確定（2026-08-03 ユーザー判断）。→ core/ADR-0014 (**accepted**、2026-08-03 昇格)
- 論点2a: レイアウト参加形態は**全面差し替え (full-bleed) 一本**で確定（2026-08-03 ユーザー判断）。中央スロット差し替え・AiForms 式両対応 (`UseFullSize`) は不採用。行内レイアウトは利用者がビルダ内で SwiftUI / Compose の合成により組む。これにより `UseFullSize` / `ShowArrowIndicator` / アイコン連携等の条件付きプロパティ群は不要。既存プリセットと見た目を揃えたいニーズは、将来 ADR-0011 の共通行レイアウト部品を public 開放する別 change で拾える（追加的で可逆なため ADR は起票しない）。
- 論点2b: タップ・アクション（2026-08-03 ユーザー判断）:
  - `onTap` クロージャ（nil 可）を持ち、**nil = 行タップ非対応**。AiForms の `IsSelectable` はフラグとしては引き継がず onTap の有無に吸収。**既定は nil（非選択）** — AiForms（既定選択可）と逆。full-bleed content 内の操作系ジェスチャとの競合を避けるための opt-in 方針。
  - クロージャは equality から除外（CommandCell の Decision 2 の家風、ADR-0014 のビルダ除外と同じ理屈）。
  - `isEnabled` / `isVisible` は家風どおり搭載。`isEnabled=false` は onTap のゲートに徹し、content のグレーアウト表現は利用者責務。
  - **`showArrow: Bool = false`** を持ち、`true` で CommandCell と同一の Disclosure Indicator を同一位置（trailing accessory 領域）にライブラリ側 accessory 機構で表示。素材・位置合わせを利用者に負わせない（ユーザー指摘: 同じ素材は用意できず位置合わせも大変）。`onTap` とは独立。Bool は equality に参加。2a の「全面」は「accessory 領域を除いた全面」と精密化。命名の最終確定は propose 段階。
  - `KeepSelectedUntilBack` 相当は引き継がない（既存 Cell 群にない概念。入れるなら全 Cell 横断の別 change）。
- 論点2c: `LongCommand` / `IsMeasureOnce` は**どちらも引き継がない**（2026-08-03 ユーザー判断。LongCommand は「実際出番がなかった」との実地知見あり）。長押しは既存 Cell 群に概念がなく、必要なら全 Cell 横断の別 change。利用者は builder 内の `combinedClickable` / `onLongPressGesture` で自力対応も可能。`IsMeasureOnce` は MAUI の Measure コスト由来の防衛スイッチで、self-sizing / Compose レイアウトパスの現行構造には対応概念自体が存在しない。
- 論点3a: builder の受け口は**宣言 UI (SwiftUI / Compose) 一本**で確定（2026-08-03 ユーザー判断）。旧 proposal の2レーン構成（+UIView / +Android View）は不採用。ネイティブ View は `UIViewRepresentable` / `AndroidView { }` の公式 interop で builder 内に埋め込む。2レーン構成の View レーンは「バインドごとネイティブ View 再生成」問題（B案却下理由と同根）を再び設計する羽目になるため。**MAUI 対応もこの経路で成立**: 将来の MAUI ブリッジは `ToPlatform()` で得たネイティブ View を interop ラッパで builder に入れる「ネイティブ View を持ち込む利用者の一種」であり（ADR-0004 の構図と整合）、iOS/Android 側に特別なレーンは不要。MAUI View インスタンスの生存管理（リサイクル時の二重親付け防止）はブリッジの責務で、詳細設計は将来の add-maui-* 群に委ねる。interop オーバーヘッドが実測で問題化した場合のネイティブ View レーン追加は非破壊で可能（可逆なため ADR 不起票）。
- 論点3b: 高さ決定は**self-sizing 全面委任、専用機構なし**で確定（2026-08-03 ユーザー判断）。iOS は `UIHostingConfiguration` の自動サイズ無効化（H/F の KsAnyView 描画で実績あり）、Android は ComposeView の recompose → `requestLayout` → RecyclerView 再測定の通常パスに任せる。AiForms 相当の Measure 呼び出し・デバウンス・高さキャッシュ機構は実装しない。固定高さは既存 DSL の `.cellHeight()` 系 modifier に乗る（iOS 側の正確な形は propose 段階で実物確認）。「content の状態変化で高さが動的に変わるサンプル」をサンプル+テストに含め、追従の実効性を受け入れ条件とする。
- 論点4: Sample 専用 Cell の置換は**対象なし**（2026-08-03 確認: 現 Sample に registry 登録の独自 Cell は存在せず、KsCellRegistry への言及は strictMode 切替のみ）。代わりに両 Sample へ CustomCell デモ画面（`CustomCellDemoScreen.kt` / `CustomCellDemoView.swift`、既存デモ画面と同列）を追加する。デモ内容: ①インライン利用、②ラップ関数再利用（AiForms オマージュの **SliderCell** 例）、動的高さ（展開/折りたたみ、3b の受け入れ条件を兼ねる）、`showArrow` + `onTap`。**ユーザー条件: デモ画面は基本 Cell 7種・入力 5種のデモと同じテーマ（SampleTheme.kt / SampleTheme.swift）を引き継ぐこと**（2026-08-03 ユーザー判断）。
- 論点5: MAUI への設計配慮は**最小限**で決着（論点3a の議論内で確定）。MAUI 対応は interop 経由で成立する見通しを確認済み。iOS/Android 側に MAUI 向けの特別な仕掛けは作らず、詳細設計は将来の add-maui-* 群に委ねる。
- 用語整理（2026-08-03 ユーザー判断）: カスタムセルは3層で呼び分ける。① **CustomCell**（インライン利用 = 新 C 案そのもの）、② **CustomCell**（ラップ関数による再利用。①から自動的に導出、登録不要）、③ **UserDefinedCell**（利用者定義 Cell。ADR-0013 で確立済みの自前 Cell 型 + Renderer + `register` 経路）。AiForms の「CustomCell 直置き」=①、「CustomCell 継承サブクラス」=②に対応。詳細は ADR-0014 の用語定義節。

## ADR 候補

- 作成済み: core/ADR-0014（CustomCell は content 値 + builder クロージャで表現、accepted。2026-08-03 に等価性文言を「content + 表示スカラー、関数値除外」へ改訂）
- 作成済み: core/ADR-0015（CustomCell は共通行レイアウト統一 ADR-0011 の適用除外、accepted。propose 段階の second-opinion-001 指摘 #2 を受けて起票）

## propose 段階での決定改訂（second-opinion-001 対応、2026-08-03）

- 論点2b の「content のグレーアウトは利用者責務」は、既存契約 cell-visual-states（無効 Cell は内包 control の操作も抑止）との整合のため「**操作の抑止はライブラリが行い、見た目の描き分けのみ利用者責務**」に改訂（ユーザー承認）。
- cellHeight の意味は既存の高さ解決契約に従う（hasUnevenRows=true では最低高）ことを明確化。

## 未決の論点

なし（論点1〜5 + 用語整理まですべて決着済み。2026-08-03）

## UI 素材

なし（現時点で画像素材は貼られていない）

## 変更級: L（確定）

2026-08-03 ユーザー判断で **L 級に確定**（エージェント当初推奨は M だったが、ユーザーが「iOS と Android 両方あるしどう考えても L 級」と判断。直近の M 級 change との規模バランスからも妥当）。

判定材料:
- 新規 capability 1個（cell-types-custom）を **iOS / Android の2プラットフォームに同時実装**
- 公開 API は追加のみ（CustomCell 型 + DSL 拡張。既存 API の破壊変更なし）
- 触る範囲: iOS UI モジュール + Android UI モジュール + Android Compose モジュール（DSL）+ 両 Sample + テスト
- UI あり（CustomCell の描画 + デモ画面2枚。デモは SampleTheme 引き継ぎ条件つき）
- 旧 proposal の「リスク: 高」要因は H/F 実運用実績・ComposeCellViewHolder 基盤・Registry 拡張不要化で低減済みだが、リサイクル×宣言 UI ホスティングの本丸実装であることに変わりはない

# Proposal: cell-style-dark-appearance-parity

## Why

fix-default-colors-dark-appearance ([core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md)) で Theme と CellStyle の未指定色は現在の外観のライブラリ既定へ解決されるようになったが、単一 Cell の色まわりに 3 つの穴が残っている ([exploration.md](exploration.md))。

1. Android の Cell 固有の色引数 12 本 (`EntryCell.placeholderColor` / `accentColor`、`ButtonCell.titleColor`、選択系・入力系 8 Cell の `accentColor`、`DatePickerCell.androidButtonColor`) が `Color?` (`null` = 未指定) のままで、Theme / CellStyle の `Color.Unspecified` と未指定の表現が 2 流儀に割れている。ADR-0030 の Alternatives が却下した「2 流儀混在」と同じ状態が Cell 側に残っている
2. 利用者が明示した CellStyle 色 (と Cell 固有色) を両外観で切り替える手段が、契約にもテストにも無い。経路自体は iOS (dynamic `UIColor`)・Android Compose DSL (再 composition → Cell 差分 → 行の再 bind) で通っており、Android Store / View は `replaceCell` で差し替える形になる。MAUI は Cell 単位プロパティ → snapshot → bridge → native の経路がコード上あるが、前回の spike が実証したのは Theme 側だけで Cell 側は未検証
3. bridge の `KsBridgeCellStyle` (iOS / Android) と MAUI の `KsCellStyleSnapshot` は doc コメントで「native の CellStyle と 1 対 1」と書いているが、`placeholderColor` は両 DTO に無く、MAUI facade の CellStyle 段は 7 色中 5 色 (accent / placeholder は Cell 固有プロパティに畳まれている) で、記述と実態が食い違っている

## What Changes

- **Android の Cell 固有色 12 本を `Color = Color.Unspecified` に揃える** (Cell の data class、対応する Compose DSL 関数の引数、`PickerCellItemProjection` の引数、`EffectiveStyle` の Cell 固有段を受ける解決関数、ViewHolder の `?.toArgb() ?:` 読み出し、bridge の DTO → Cell 変換)。解決順 (Cell 固有値 → CellStyle → Theme → 外観既定) と描画結果は変えない
- **明示した CellStyle 色 / Cell 固有色の外観に対する契約を 3 platform で明文化し、テストで固定する**: ライブラリは明示色を置き換えない。両外観で変えたいときの手段は各 platform の慣用 (iOS: dynamic `UIColor` / Android Compose: 構築時分岐 / Android Store: 外観変更時の `replaceCell` / MAUI: 外観変更 (`RequestedThemeChanged`) を購読して Cell の色プロパティへ再代入)。ライブラリに新しい型・API・コールバックは足さない (ADR-0030 Decision 3 の延長)
- **MAUI の Cell 単位プロパティの変更が native の Cell 置換まで届くことを spike で実証する** (実装の先頭、承認ゲート。tasks 0.1 で実施済み)。結果: プロパティへの直接代入は iOS / Android とも表示中の行に届いた。一方 Cell プロパティに書いた `AppThemeBinding` は外観変更で再評価されず (facade の Section / Cell が MAUI の element ツリーに属さないため)、当初の手段「Cell プロパティの `AppThemeBinding`」は成立しなかった。2026-09-06 オーナー裁定: MAUI の手段を「外観変更を購読して再代入」に改めて続行する。net10 のユニットテストでプロパティ変更 → snapshot の style 更新の配信を固定する
- **bridge / snapshot の DTO 記述を実態に合わせる**: `KsBridgeCellStyle` (iOS / Android) と `KsCellStyleSnapshot` の「1 対 1」の doc コメントを、輸送している項目の実態 (CellStyle 段の placeholder は輸送せず、MAUI の `EntryCell.PlaceholderColor` は Cell 固有段で届く) に書き換える。wire 形式は変えない (下記 Non-Goals)
- **tests**: Android (Cell 固有色の `Unspecified` 解決、明示 CellStyle 色は uiMode 変更で不変・未指定は追随、Compose DSL の style 差分が行の再 bind まで届く)、iOS (CellStyle の dynamic `UIColor` は dark 値へ解決、固定色は不変)、MAUI (Cell プロパティ変更が style 更新として配信される)、bridge (Android の Cell 固有色 DTO null → `Unspecified`)
- **付随修正**: 型変更で触る Android の Cell ファイル (`ButtonCell.kt` / `RadioCell.kt` 等) の公開 doc コメントに残る ADR 参照の除去 (前回 change の deviation.md が「別途起票判断待ち」とした据え置き分のうち、本 change が触るファイルの範囲)
- 影響能力: settings-view-android-ui / settings-view-ios-ui (契約とテストのみ、実装変更なし) / maui-bridge

## Non-Goals

- **Cell 粒度で明示色を表示中の外観変更に自動追随させる機構 (対の型・Cell へ外観を渡すコールバック)** — 探索で却下 (exploration 論点 2 の B)。ADR-0030 Decision 3 を覆す設計判断であり、Store 経路の利用者が `replaceCell` を書く手間を省くためのコストとして見合わない
- **bridge DTO / MAUI snapshot への `placeholderColor` (CellStyle 段) の追加** — MAUI facade に CellStyle 段の placeholder プロパティが無く、追加しても書き手のいない wire になる (CellStyle 段の accent も同じく DTO にあるが MAUI から設定されない)。MAUI から placeholder を指定する手段は `EntryCell.PlaceholderColor` (Cell 固有段) で足りている。DTO は doc コメントの訂正にとどめる
- **MAUI facade に CellStyle 段の accent / placeholder プロパティを足す** — MAUI は CellStyle 段と Cell 固有段を区別しない設計 (maui/ADR-0004、`CellBase.TitleColor` が ButtonCell では Cell 固有段に写る) で、区別を持ち込むのは別の設計判断
- **Sample 3 面の変更** — 探索時に「MAUI サンプルの固定 `x:Static` 色が非追随」を穴として挙げたが、提案作成時の照合で誤りと判明した。共通フィールド統合デモ・入力 Cell デモの accent / placeholder は iOS / Android サンプルでも固定値 (`SampleTheme.demoAccentXxx` / `demoPlaceholderOrange`) で、handbook cross/sample-parity.md の「dark mode 追随のような platform らしさより一致を優先する」どおり 3 面一致している。唯一外観分岐している基本 Cell デモの ButtonCell title は MAUI も code-behind (`SampleThemeFollower`) で追随済み。MAUI の spike は前回と同じく一時改変で行い、Sample には残さない
- **色以外の nullable フィールドの `Unspecified` 化** — 前回 change と同じ理由 (Compose 自身も `TextStyle` は既定値式で扱う)
- **`skills/` (利用者向け Agent Skills) と README の追従** — 契約と利用コード例は蒸留で concepts (`core/styling/style-resolution.md` と platform 別 api 文書) に書き、docs-refresh スキルの既存経路 (`skills/.manifest.json` の `targets`) でユーザーの明示依頼により別途追従する
- **MAUI facade の Section / Cell を MAUI の element ツリーへ繋ぎ、Cell プロパティの `AppThemeBinding` を外観変更で再評価させる改修** — tasks 0.1 の spike で `AppThemeBinding` が Cell では再評価されないと判明した (2026-09-06)。親子付けは BindingContext の継承など他の挙動へ波及する設計判断で、本 change の主題 (型統一と契約固定) と切り離す。`maui-appthemebinding-coverage` (簡易起票済み) に発見を合流させ、別途探索する
- **前回 deviation.md の据え置き分のうち本 change が触らないファイル (`CellBaseLayout.kt` 等) の doc コメント掃除** — 触るファイルの範囲を超える。別途判断のまま

## Impact

- **破壊的変更 (Android)**: Cell 固有色 12 本と対応する DSL 関数の引数の型が `Color?` → `Color` (既定 `Color.Unspecified`) になる。`null` を明示的に渡す呼び出しと、値を `?:` / `?.let` で読むコード (ViewHolder・bridge 変換・tests) に追随作業が発生する
- **配信状況と互換方針**: Android は `0.1.0-beta.1` として 2026-09-04 に Maven Central へ配信済みで、ADR-0030 と前回 change が置いた「未配信」の前提は当時から古かった (相方レビュー second-opinion-spec-001 Major 1)。**beta の prerelease 期間中は破壊的変更を受容し、互換経路 (旧 `Color?` の overload / deprecated constructor) は作らない** (2026-09-06 オーナー裁定: 案 A)。理由: prerelease は互換を約束しない段階で beta.1 は配信直後、旧シグネチャを残す互換案は ADR-0030 が却下した「未指定の 2 流儀混在」を公開面に作る。利用者の移行は機械的 (`null` を渡している引数は省略、`?:` / `?.let` で読んでいる箇所は `takeOrElse` / `isSpecified` へ)。次の prerelease のリリースノートに Android の breaking change として記載する。ADR-0030 の前提「未配信」は「beta 期間中は破壊的変更を受容」へ訂正する (蒸留で amend)
- **等価性**: Cell の `equals` / `hashCode` は `Color.Unspecified` 同士を等価として扱うため、`null` 時代と同じ判定結果になる (対象 10 Cell はすべて手書きの `hashCode` を持ち `null` 前提で書かれているため、全部の追随が要る)
- **iOS**: 公開型・実装の変更なし。テストと bridge DTO の doc コメントのみ
- **MAUI**: facade の公開面・wire 形式の変更なし。Android bridge の DTO → Cell 変換が `Unspecified` へ写る形に追随する
- **利用者可視の変更**: なし (描画結果は 3 platform とも変えない)。Android の利用者が Cell 固有色に `null` を渡していた場合はコンパイルエラーになる
- **リスク**: Android の `Color?` 読み出し箇所の追随漏れが `Unspecified.toArgb()` (透明な黒) として現れる。前回 change の付随修正で同型の漏れ (`EntryCellViewHolder` の placeholder) が実際に起きているため、tasks で消費者 (ViewHolder 6 箇所・`EffectiveStyle` 3 関数・DatePicker の sheet 色) を列挙し、`Unspecified` が ARGB へ落ちないことをテストで固定する
- **ゲート**: tasks 0.1 の spike で MAUI の Cell 単位プロパティの直接代入が native の Cell 置換まで届くことは実証済み (iOS / Android)。`AppThemeBinding` は届かず、契約を「購読して再代入」に改めた (上記 Non-Goals)。ゲートは通過扱い (2026-09-06)
- 長命層: ADR-0030 の Context / Revisit When の前提訂正 (上記)。`core/styling/style-resolution.md` (未指定表現の記述を Cell 固有色まで正確に、明示色の追随手段を 4 経路で、`KsCheckBoxView` の CGColor 再解決の追記)、`android/api/android-native-host.md` / `android-compose.md` (Cell 固有色の型と Store 経路の差し替え例)、`ios/api/ios-native-host.md` (CellStyle の dynamic 色の例)、`maui/api/maui-styling.md` (Cell プロパティの外観追随は `RequestedThemeChanged` 購読 + 再代入、`AppThemeBinding` は Cell では効かない旨)、`maui/api/native-bridge.md` (CellStyle DTO の輸送項目)。ADR-0030 Decision 3 に CellStyle への適用を一文追記 (蒸留で追随)

## 級: M

複数能力横断 (Android UI / bridge / 3 面のテスト) で Android の公開 API の破壊的変更を含むが、新しい契約・既定値・ADR は増えず UI 承認ゲートもない (2026-09-06 オーナー確定)。

domain: core

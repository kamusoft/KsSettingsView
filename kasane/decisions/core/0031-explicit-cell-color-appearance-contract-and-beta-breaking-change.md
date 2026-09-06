---
id: 0031
title: 未指定色の Unspecified 統一と明示色の外観契約を Cell 固有色まで広げ、beta 配信中は互換経路なしの破壊的変更を受容する
status: accepted
date: 2026-09-06
amends: 0030
---

## Context

core/ADR-0030 で Theme と CellStyle の未指定色は現在の外観のライブラリ既定へ解決されるようになったが、単一 Cell の色まわりに 3 つの穴が残っていた (kasane/changes/archive/2026-09-06-cell-style-dark-appearance-parity/exploration.md)。

1. Android の Cell 固有の色引数 12 本 (`EntryCell.placeholderColor` / `accentColor`、`ButtonCell.titleColor`、選択系・入力系 8 Cell の `accentColor`、`DatePickerCell.androidButtonColor`) が `Color?` (`null` = 未指定) のままで、Theme / CellStyle の `Color.Unspecified` と未指定の表現が 2 流儀に割れていた。ADR-0030 の Alternatives が却下した「2 流儀混在」と同じ状態が Cell 側に残っていた
2. 利用者が明示した CellStyle 色と Cell 固有色を両外観で切り替える手段が、契約にもテストにも無かった。経路自体は iOS (dynamic `UIColor`)・Android Compose DSL (再 composition → Cell 差分 → 行の再 bind) で通っており、Android Store / View は `replaceCell` で差し替える形になる。MAUI は Cell 単位プロパティ → snapshot → bridge → native の経路がコード上あるが、実証は Theme 側だけだった
3. bridge の `KsBridgeCellStyle` (iOS / Android) と MAUI の `KsCellStyleSnapshot` は「native の CellStyle と 1 対 1」と説明していたが、`placeholderColor` は両 DTO に無く、MAUI facade の CellStyle 段は 7 色中 5 色で、記述と実態が食い違っていた

前提の変化: ADR-0030 は「ライブラリは未配信で外部利用者がいない」ことを破壊的変更の根拠にしていたが、Android は `0.1.0-beta.1` として 2026-09-04 に Maven Central へ配信済みで、この前提は ADR-0030 の accepted 時点 (2026-09-05) で既に古かった (相方 spec レビューの指摘)。

実証で判明した制約: MAUI の Cell プロパティに書いた `AppThemeBinding` は外観変更で再評価されない。facade の `Section` / `CellBase` は MAUI の element ツリーに属さず (`Parent` が `null`)、ツリー外の `Element` では binding が外観変更を受け取らない (実測に基づく見立て。MAUI 本体の binding 実装は未読)。同じプロパティへの直接代入は同一 Activity / 同一 View のまま native の行に届く (iOS / Android で実測。同 tasks.md 0.1)。

本決定の前提: ライブラリは prerelease (beta) 配信中で、正式版 (`0.1.0`) は未配信。MAUI facade は CellStyle 段と Cell 固有段を区別しない設計 (maui/ADR-0004)。

## Decision

ADR-0030 の決定のうち、Context の前提「ライブラリは未配信」と Revisit When の「配信後に互換問題が分かったとき」、および Decision 3 の MAUI 節 (`AppThemeBinding` を手段とする範囲) を本決定で置き換える。他の決定 (既定色の外観追随・未指定色の描画時解決・Theme の型不変・iOS / Android の実現方法・Cell title / description の扱い・MAUI facade 不変) は維持する。

1. **Android の Cell 固有色は Theme / CellStyle と同じ 1 流儀にする。** Cell 種別が意味上の固有値として持つ色引数 12 本、対応する Compose DSL 関数の引数、選択系 Cell の projection の引数はすべて非 nullable の `Color` 型で、未指定は `Color.Unspecified` (既定値) で表す。`null` を未指定として受ける色引数は置かない。解決順 (Cell 固有値 → CellStyle → Theme → 外観既定または platform 既定) と描画結果は変えない。`Unspecified` を ARGB へ落とす消費点は 1 つのヘルパ (`toArgbOrElse`) に集約し、透明な黒への変換を残さない
2. **明示した CellStyle 色と Cell 固有色もライブラリは置き換えない。** 表示中に外観が変わっても明示色は変わらず、同じ行の未指定色だけが現在の外観の既定へ再解決される。両外観で異なる色を使う手段は各 platform の慣用に乗せ、ライブラリは Cell へ外観を渡す型やコールバックを公開しない:
   - iOS: dynamic な `UIColor` を渡す。`CellStyle` / Cell 固有値を CGColor へ落とす箇所 (`KsCheckBoxView` の塗りと枠、Section 装飾の Border) はライブラリが trait 変更で再解決する
   - Android Compose DSL: 構築時に `isSystemInDarkTheme()` で色を選ぶ。CellStyle / Cell 固有色だけが変わった再 composition は内容更新 (`replaceCells` 経路) として表示中の行へ届き、行は作り直されない
   - Android Store / View: 外観の変更を受けて `replaceCell` / `replaceCells` で style または Cell 固有色を差し替えた Cell に置き換える
   - MAUI: `SettingsView` 単位の Theme プロパティは XAML の `AppThemeBinding` で書ける (ADR-0030 の実証どおり)。Cell の色プロパティは `AppThemeBinding` が再評価されないため、`Application.RequestedThemeChanged` を購読して現在の外観の値を再代入する。再代入は facade → snapshot → bridge → native の Cell 置換として表示中の行に届く
3. **beta (prerelease) 配信中は公開 API の破壊的変更を受容し、互換経路 (旧 `Color?` の overload・deprecated constructor) を作らない。** 変更は次の prerelease のリリースノートに breaking change として記載する。移行は機械的 (`null` を渡す引数は省略、`?:` / `?.let` で読む箇所は `takeOrElse` / `isSpecified` へ)
4. **bridge / snapshot の DTO には CellStyle 段の `placeholderColor` を足さない。** MAUI からの placeholder 指定は `EntryCell.PlaceholderColor` (Cell 固有段、per-type の Cell DTO で輸送) で足りており、CellStyle 段の accent / placeholder は MAUI から設定する手段を持たない。DTO の wire 形式は変えず、記述だけを輸送項目の実態に合わせる

## Alternatives Considered

明示色の外観追随の手段:

- **Cell 粒度の追随機構 (light / dark の対の型、Cell へ外観を渡すコールバック) を足す**: 却下。ADR-0030 が却下したコールバックを Cell 粒度で復活させることになり、Cell 抽象 (core/ADR-0009 / ADR-0013) か Store に外観の概念が入る L 級の改修になる。省けるのは Store 経路の利用者が `replaceCell` を 1 回書く手間だけで、覆すコストに見合わない
- **MAUI の Cell プロパティに `AppThemeBinding` を書く (当初の手段)**: 実証で不成立。`Section` / `CellBase` が element ツリー外にあり再評価されない。`Section` / `Cell` をツリーへ繋ぐ改修は BindingContext の継承など他の挙動へ波及する別の設計判断のため、本決定から切り離して別途探索する (`maui-appthemebinding-coverage`)

Android の未指定の印:

- **`Color?` のまま残し、concepts に Theme / CellStyle との使い分けを一文足す**: 却下。利用者に 2 流儀を覚えさせ、ADR-0030 の Alternatives で却下した「2 流儀混在」と同型の状態を Cell 側に残す

配信済み API の互換方針:

- **互換経路を設計し旧 API から段階的に移行する (旧 `Color?` の overload / deprecated constructor)**: 却下。prerelease は互換を約束しない段階で、beta.1 は配信直後 (2 日)。旧シグネチャを残す互換案は「未指定の 2 流儀混在」を公開面に作る。オーナー裁定 2026-09-06

bridge / MAUI facade:

- **bridge DTO と MAUI snapshot に CellStyle 段の `placeholderColor` を追加する**: 却下。MAUI facade に CellStyle 段の placeholder プロパティが無く、追加しても書き手のいない wire になる (CellStyle 段の accent も同じく DTO にあるが MAUI から設定されない)
- **MAUI facade に CellStyle 段の accent / placeholder プロパティを足す**: 却下。MAUI は CellStyle 段と Cell 固有段を区別しない設計 (maui/ADR-0004。`CellBase.TitleColor` が ButtonCell では Cell 固有段に写る) で、区別を持ち込むのは別の設計判断

## Consequences

- 正: Android の未指定の表現が Theme / CellStyle / Cell 固有色で 1 流儀 (`Color.Unspecified`) に揃い、Compose の慣習 (`takeOrElse` の解決チェーン) にも一致する
- 正: 明示色の外観契約が 3 platform で明文化され、テストで固定された (Android: uiMode 変更で明示 CellStyle 色・Cell 固有 accent が不変、DSL / Store の style 差分が内容更新として行へ届き行が作り直されない。iOS: CellStyle の dynamic 色が dark 値へ解決され固定色は不変、`KsCheckBoxView` の accent が trait 変更で再解決。MAUI: Cell プロパティ変更が Cell 置換として配信され、`RequestedThemeChanged` 購読 + 再代入が MauiHost の既定シナリオとして回帰資産になった)
- 正: 描画結果は 3 platform とも変わらない (Android Native Sample 6 画面 × 2 外観の実装前後の画素比較で差分ゼロ。出典: 実装結果)
- 正: 消費点を `toArgbOrElse` 1 本に集約したことで、`Unspecified` を素の `toArgb()` へ戻す退行は Android 本体の 25 件以上のテストで検出される (出典: 実装結果、review-001 のミューテーション probe)
- 負: Android の Cell 固有色 12 本と対応する DSL 関数の引数の型が `Color?` → `Color` になる破壊的変更。beta.1 の利用者で `null` を渡す呼び出しはコンパイルエラーになり、`?:` / `?.let` で読むコードに追随作業が発生する
- 負: 対象 10 Cell は手書きの `hashCode` を持ち、`null` 前提の実装をすべて追随させる必要があった。`Unspecified` 同士は等価で、`null` 時代と同じ等価性判定になる (出典: 実装結果)
- 負: MAUI の Cell プロパティは `AppThemeBinding` で外観追随せず、利用者が購読と再代入を書く。`SettingsView` の Theme プロパティ (`AppThemeBinding` が効く) と Cell の色プロパティで手段が分かれる非対称が残る
- 負: Android Store / View の利用者は外観変更時に `replaceCell` / `replaceCells` を自分で書く (Activity が再生成される構成では onCreate で組み直すだけでよい)
- 負: bridge DTO と MAUI facade の CellStyle 段は native の 7 色中 5 色のままで、CellStyle 段の accent / placeholder を MAUI から指定する手段は持たない (Cell 固有段で足りている前提)
- 負: MAUI iOS で表示中に外観をダークへ切り替えると行の背景がライトのまま残る既存不具合が MauiHost の確認で見つかった (本決定の変更前から再現。`fix-maui-ios-dark-appearance-row-background` として別途起票。出典: 実装結果)

## Revisit When

- `AppThemeBinding` を Cell で効かせる改修 (`Section` / `Cell` を MAUI の element ツリーへ繋ぐ) が採用されたとき — Decision 2 の MAUI 節の手段を見直す
- 正式版 (`0.1.0`) を配信したとき — Decision 3 の「beta 配信中は互換経路なし」は prerelease 期間の方針であり、正式版以後の互換方針は別途決める
- MAUI facade に CellStyle 段の accent / placeholder を公開する要望が出たとき — Decision 4 の DTO の輸送項目を見直す
- Android で明示指定した色まで表示中の外観変更に自動追随させる要望が出たとき (ADR-0030 の Revisit When と同じ)

出典: kasane/changes/archive/2026-09-06-cell-style-dark-appearance-parity/exploration.md (論点 1 / 論点 2 の比較表と決定事項) / 同 proposal.md (What Changes・Non-Goals・Impact の配信状況と互換方針) / 同 tasks.md (0.1 spike の結果と裁定、8.2 / 8.4 の実行結果) / 同 deviation.md (MAUI の手段の変更) / 同 second-opinion-spec-001.md (Major 1: 未配信前提の失効) / 同 review-001.md (Minor 3: ADR-0030 Decision 3 の MAUI 節の限定、Suggestion: Android の内容更新経路)

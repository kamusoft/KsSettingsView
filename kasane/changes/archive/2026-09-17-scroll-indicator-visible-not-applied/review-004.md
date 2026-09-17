# レビュー結果: scroll-indicator-visible-not-applied (004 回目)

**日付**: 2026-09-17
**判定**: APPROVED

## サマリー

deviation.md 1 項目め (オーナー指示による Sample への恒久追加) の実装を、`samples/` 配下の未コミット差分だけを対象にレビューした。候補 47 件の PickerCell 行「都道府県」が iOS / Android / MAUI の入力 Cell デモの同じ Section・同じ位置に、表示文言・デモデータ・パラメータすべて一致した状態で追加されている。差分は 4 ファイル 84 行の追加のみで削除・変更が 1 行も無く、既存の行・文言・デモデータ (「テーマ」の 3 候補、`SampleTheme`) は手つかず、Header / Footer 背景色のデモも追加されていない。Critical / Major なし。コード整理上の小さな不揃いを 1 件 Suggestion として挙げる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/sample-parity.md` (きっかけ: `samples/` のデモ画面・文言・デモデータを追加・変更するとき)
- `kasane/lessons/code-review.md` の重点観点 (L-001 はミューテーション実測を要する争点が無いため非適用。本レビューはビルド・テスト非実行の指示下)

`cross/test-execution.md` は今回の差分がテストを含まず、実行も指示で禁じられているため非適用。`cross/public-identifiers.md` は配布座標・build ファイルを触らないため非適用。

## 確認した観点と結果

**合意スコープとの一致 (deviation.md 1 項目め)**

- 3 platform すべてに追加されている: `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:192-200`、`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:198-207`、`samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:60-63`
- 位置: 3 面とも Section「PickerCell（単一選択）」の中の「テーマ」行の直後。Section 数・並び順は変わっていない
- 文言: `title` = 「都道府県」、`pageTitle` = 「都道府県を選択」が 3 面で一致 (コードポイント単位で確認。異体字・全半角のゆれなし)
- デモデータ: 候補 47 件を 3 ファイルから抽出して文字列列として比較し、**要素・順序ともに完全一致**。重複なし
- 初期選択: `12` (= 「東京都」) が 3 面一致 (Swift `@State private var prefectureIndex: Int? = 12` / Kotlin `mutableStateOf<Int?>(12)` / C# `_prefectureIndex = 12`)
- 未選択時の表示: 3 面とも `(未選択)`。MAUI は `FormatPrefecture` が範囲外も `(未選択)` に落とし、iOS は `prefectures.indices.contains`、Android は `getOrNull` で同じ振る舞い
- 候補数が「画面に収まらない」条件を満たし、初期選択が中ほど (12/47) にあるため、選択面を開いた時点でスクロール可能な状態になる — 本 change の目的 (候補リストのスクロールインジケータを Sample で確認できること) に適う

**既存資産への非侵襲**

- `git diff HEAD -- samples/` は 4 ファイル・84 行の追加のみで、削除行・変更行がゼロ。「テーマ」の候補は 3 件のまま、`SampleTheme` は 3 platform とも無変更
- Header / Footer 背景色のデモは追加されていない (deviation.md 2 項目めと整合)。tasks 4.4 の確認結果としての sample 側の色明示も、追加・変更なしで従前のまま
- 生成物 (`samples/maui/KsSettingsView.Sample.Maui/bin/` 配下の XML doc) に語が現れるが、追跡対象外のビルド成果物であり差分に含まれない

**各面の既存の流儀**

- 変更追跡: iOS は `tracked($prefectureIndex, title:display:)`、Android は `prefectureIndex.tracked(title, display, onEvent = record)`、MAUI は setter 内の `LastEvent = $"都道府県 → {FormatPrefecture(value)}"` と、いずれも同ファイル内の「テーマ」行と同じ経路。イベント文字列の形式も `都道府県 → 東京都` で 3 面一致
- デモデータの置き方: 文字列候補は画面 / ViewModel 内にインライン (既存の `themes` / `notifTypes` / `Themes` / `NotificationTypes` と同じ)。専用ファイル (`SampleMember`) は object 候補のための置き場なので、ここでインライン化するのは既存の使い分けに沿う
- MAUI VM の並び: バッキングフィールドを `_themeIndex` の直後に、候補リスト→index の対で `ThemeIndex` の直後に、`FormatPrefecture` を `FormatTheme` の直後に置いており、既存の配置規則を踏襲している
- 末尾カンマ・コレクション式・`remember` の使い方は各言語の既存記述と同型

**コメント規約 (`cross/comment-policy.md`)**

- 追加コメントに作業文書のパス・change 識別子・ローカル通番・デルタスペック構文キーワード・履歴記述のいずれも無い
- MAUI の `Prefectures` / `PrefectureIndex` は Sample アプリ内の public プロパティで、doc コメントは内部用語を含まず単体で意味が通る
- `scripts/comment-policy-lint.py --advisory` と `scripts/local-path-lint.py` を対象 4 ファイルについて確認し、報告なし

**指示に従い未実施**: ビルド・テスト・ミューテーション (オーケストレーターが同じツリーで実機向けビルドを実行中のため)。本レビューは静的読解のみで、3 面のビルド成功は実装側の報告に依る。

## 指摘事項

### [🔵 Suggestion] Android だけ「テーマ」と別の MARK グループに分かれている

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:65`

**問題点**: iOS 側は「都道府県」の状態を既存の `// MARK: - PickerCell（単一）TwoWay binding 用` グループの中に置いているのに対し、Android 側は `// MARK: - PickerCell（単一）候補が画面に収まらない選択面用` という新しい MARK グループを作っている。「都道府県」も TwoWay binding 経路の状態であり、この MARK が経路の違いを表しているわけではない。Android 側ファイルの KDoc は「iOS Sample の `InputCellsDemoView.swift` と Section 構成・表示文言・デモデータを一致させる」と宣言しており、MARK グループの対応関係も iOS に揃えるのが同ファイルの流儀に見える。表示文言には出ないため `cross/sample-parity.md` の違反ではなく、可読性上の不揃い。

**推奨修正**: Android の新規 MARK 行を落とし、既存の `// MARK: - PickerCell（単一）TwoWay binding 用` グループ内に `prefectures` / `prefectureIndex` を置く。用途の説明が要るなら、iOS / MAUI と同様に状態そのものへの doc コメント (`/** 単一選択：都道府県（候補が画面に収まらない長さの選択面のデモ） */`) で添える。

## アクションプラン

1. 対応任意: 上記 Suggestion (Android の MARK グループ) — 修正しなくても合意スコープ・規約ともに満たしている

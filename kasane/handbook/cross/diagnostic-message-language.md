---
kind: rule
applies-when:
  always: false
  paths: ["ios/Sources/**", "android/*/src/main/**", "maui/KsSettingsView.Maui/**", "maui/macios/KsSettingsView.Binding.iOS/**", "maui/android/KsSettingsView.Binding.Android/**"]
  tasks: [例外・ログ・assert・コンパイル時警告など開発者向け文字列の追加・変更]
title: 例外・診断メッセージの言語
description: 本体コードが実行時・コンパイル時に開発者へ向けて出す文字列 (例外の message・ログ・assert / precondition・deprecated 警告文) は英語固定にする規約。UI 文言 (OS リソース由来) とソースコメント (日本語) は対象外
timestamp: 2026-09-07
---

# 例外・診断メッセージの言語

この文書は、ライブラリ本体のコードが**開発者に向けて出す文字列**の言語を定める。読むと、どの文字列が英語固定の対象で、どの文字列が対象外か、そして既存メッセージに揃える書き方が分かる。

## 規則

本体コード (利用者が参照するパッケージに含まれる製品コード。iOS の `ios/Sources/`、Android の各モジュールの `src/main/`、MAUI の facade と binding) が出す次の文字列は**英語で書く**:

- 例外・エラーの message (`throw` する例外の文字列、Swift の `Error` の description、Kotlin の `require` / `check` / `error()`、C# の `ArgumentException` 等)
- ログ・診断出力 (`print` / `os.Logger` / `Log.*` / `Debug.WriteLine` 等に渡す文字列)
- assert / precondition / `assertionFailure` / `fatalError` の文字列
- コンパイル時に開発者へ出る文字列 (`@available(message:)`、`@Deprecated(message)`、`[Obsolete]`、MSBuild の Error / Warning テキスト)

理由: ライブラリの利用者は言語圏を限定できず、例外 message と診断ログは利用者のクラッシュレポート・ログ・IDE の警告にそのまま現れる。本体の他のメッセージは既に英語で統一されており、日本語のメッセージが混じると英語圏の利用者が読めない箇所が点在する。

メッセージのローカライズ機構 (リソース化して端末 Locale で切り替える) は採らない。開発者向け診断は利用者のアプリ画面に出る文言ではなく、文字列を機構に載せる費用に見合う利益がない。

## 対象外

- **UI 文言** (ボタンラベル・既定タイトル・アクセシビリティラベル等の利用者の画面に出る文字列): 利用者向けの文字列であり本規約の対象ではない
- **ソースコメント・doc コメント**: 日本語で書く (AGENTS.md の全体ルール、[ソースコメント規約](comment-policy.md))
- **テストコードのアサーションメッセージ・テスト名**、**samples / verification のデモ用文言**: 対象外。ただし samples 内の例外メッセージも本体と同じ英語で書くと一貫する

UI 文言のうち選択面の文言は、自前の翻訳文字列を持たず OS の公開リソース・端末 Locale 由来にする契約が別にある ([Picker の選択面](../../concepts/core/cells/picker-selection-surface.md) ほか各選択面の記述)。それ以外の UI 文言 (アクセシビリティラベル等) の言語方針は本規約では定めない。

## 書き方

既存メッセージの流儀に揃える:

- 文脈を先頭に置き、説明を続ける: `assertionFailure("KsCellRegistry: no renderer registered for \(type(of: cell))")`、`require(...) { "Section: header and headerContent cannot both be specified" }`
- 句点の有無はメッセージの形ごとの既存慣行に従う (下表)
- 利用者が渡した値・位置・実際に採った既定値など、原因の特定に要る情報を補間で載せる (下の例)

| メッセージの形 | 句点 | 例 |
|---|---|---|
| Swift / Kotlin の 1 文で終わる短句 (例外・ログ・assert・deprecated 警告) | 付けない | `IllegalStateException("Cell type ${cell::class} is not registered in KsCellRegistry")` |
| Swift / Kotlin / MSBuild の複数の文を連ねた診断 | 各文を句点で区切る | `"DatePickerCell(id=$id) has $reason. Calendar dialog will not be shown."` |
| C# の例外 message | 1 文でも句点で終える | `InvalidOperationException("A null Section cannot be placed in SettingsView.Root.")` |

補間の例: `"ItemsSource must not contain a null element (index {i})."` (C#)、`"The time string '25:00' does not match the transport format 'HH:mm'; using 00:00 instead"` (Android の Bridge 診断。iOS は `KsSettingsViewBridge: ` の接頭辞に続けるため `the …` と小文字で始める)。

## 適用契機

本体コードに例外・ログ・assert・deprecated 警告などの開発者向け文字列を追加・変更するとき、およびコードレビューのとき。機械検査は無いため、レビューで判定する。

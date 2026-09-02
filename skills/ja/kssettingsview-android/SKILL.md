---
name: kssettingsview-android
description: KsSettingsView で Android の設定画面 (settings screen) を作る - Jetpack Compose の宣言的 DSL または XML の View ホスト (KsSettingsView という View) で、組み込みの Cell (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker) に加えて任意の Composable を行 (row) として表示する CustomCell、SettingsRootStore による表示中の更新、Theme / CellStyle のスタイル指定を扱う。jp.kamusoft:kssettingsview に依存する、または jp.kamusoft.kssettingsview.core / .ui / .compose を import する Kotlin アプリで設定画面を追加・変更・レビューするときに使う。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for Android

KsSettingsView は、iOS の設定アプリのようなリスト形式の設定画面を組み立てる UI ライブラリ。画面は行 (Cell) を Section にまとめたツリーとして宣言し、そのツリーがそのまま画面になる。この Skill が扱うのは Android 版で、Jetpack Compose の宣言的 DSL と XML に置く View ホスト (`KsSettingsView` という View) の 2 つの形で提供される。宣言ツリーとして書いても、Store から命令的に操作する形でも書ける。

## できること

| やりたいこと | 参照先 |
|---|---|
| 行を置く: ラベル、操作、ボタン、スイッチ、チェックボックス、ラジオ、テキスト入力、リスト選択、数値、時刻、日付 | [references/cells.md](references/cells.md) |
| 行を Section にまとめる、アイコン・説明・ヒントを付ける、行を無効化・非表示にする | [references/cells.md](references/cells.md) |
| 表示中の画面を変える: 行の挿入・削除・移動・差し替え、複数行のバッチ更新、`SettingsRootDiff` での直接駆動 | [references/updates.md](references/updates.md) |
| 再評価をまたいで行を追跡する、状態から表示・非表示を切り替える、XML から画面を組み込む | [references/updates.md](references/updates.md) |
| 色・フォント・行高さ、Classic / Modern の list 外観、Section の箱、`Theme` の既定値定数 | [references/styling.md](references/styling.md) |
| Section と画面全体の Header / Footer (任意の Composable も置ける) | [references/styling.md](references/styling.md) |
| 任意の Composable を行 (row) として表示する、独自の Cell 型と ViewHolder を定義する | [references/custom-cells.md](references/custom-cells.md) |

## 導入

ライブラリは Compose DSL まで含む単一 artifact として配布している。層は Kotlin のパッケージ名で表され — `jp.kamusoft.kssettingsview.core` (設定ツリー)、`.ui` (Cell・`Theme`・`CellStyle`・View ホスト)、`.compose` (Composable と宣言的 DSL) — `import` はこのパッケージ名で書く。

### ライブラリをビルドに取り込む

アプリモジュールの `build.gradle.kts` に依存を宣言する。

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:0.1.0")
}
```

Maven Central への初回公開はまだ行われていないため、執筆時点ではこの座標は公開リポジトリから解決できない。`0.1.0` は初回リリースの版を表す。公開までの間は、`android/` の Gradle ビルドで `./gradlew publishToMavenLocal` を実行して artifact を作り (版は `0.1.0-SNAPSHOT` になる)、`mavenLocal()` 経由で取り込める。artifact は Compose runtime / ui / foundation-layout・kotlinx-coroutines-core・androidx.annotation・RecyclerView を `api` 依存として宣言しているので、公開 API に対するコンパイルはこの 1 行で足りる。

### バージョン

以下はアプリモジュール自身が満たす必要のあるもの。

| アプリ側の要件 | 最低バージョン |
|---|---|
| minSdk | 29 |
| compileSdk | 35 |
| Java / Kotlin の JVM target | 17 |
| Kotlin | 2.4.10 |
| Compose BOM | 2025.11.01 |

ライブラリ自身は Gradle 9.5.0 と Android Gradle Plugin 8.13.2 でビルドされている。これらはライブラリ側のツールチェーンであり、利用者のビルドへの要件ではない。Compose BOM の行は artifact がコンパイルされた版で、推移的に引き込まれる Compose ライブラリの下限もこれに追従する。

ライブラリは利用アプリ側に前提を置かない。すべてを自前で同梱する Material3 派生テーマでラップした Context の中に描くため、XML テーマは何でもよく (最小構成のテーマ・AppCompat 系・MAUI テンプレート既定のいずれでも)、Activity も何でもよい (`ComponentActivity` を含む)。時刻・日付のピッカーもどの構成でも開く。この自己完結には知っておくべき帰結が 2 つある:

- アプリ側テーマの色 (カスタム色・dynamic color を含む) はライブラリ UI に届かない。見た目の調整はライブラリ自身の `Theme` / `CellStyle` で行う — [references/styling.md](references/styling.md) を参照。利用者所有のコンテンツ (`CustomCell` の中身、`KsAnyView` 経由で渡した View) だけは従来どおりホストのテーマで描画される。
- ライト / ダークは端末の夜間モードとアプリの uiMode 制御 (`AppCompatDelegate.setDefaultNightMode` / `UiModeManager.setApplicationNightMode`) で切り替わる。アプリが XML テーマで Dark 系を宣言するだけでは、ライブラリ UI は切り替わらない。

## 最小動作コード

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.SwitchCell

@Composable
fun SettingsScreen() {
    val notifications = remember { mutableStateOf(true) }

    KsSettingsView {
        Section(header = "General") {
            LabelCell(title = "Version", valueText = "1.0.0")
            SwitchCell(title = "Push notifications", isOn = notifications)
        }
    }
}
```

`Section` は DSL スコープのメンバ関数なので import は要らない。Cell の関数は Section スコープの拡張関数なので、使うものを個別に import する。`KsSettingsView` Composable は DSL / Store のどちらの overload も Compose の `modifier` 引数を受ける。

この `KsSettingsView { ... }` の再評価 DSL では、Cell の関数が `CellHandle` を返し、`Section` は `SectionHandle` を返す ([references/updates.md](references/updates.md) に出てくる `settingsRoot` builder の `section` / `cell` は Handle を返さない)。Handle は今置いた行や Section への不透明な参照で (利用者が構築することも中身を読むこともできない)、`LabelCell(title = "Name").titleColor(Color.Red)` のように [references/styling.md](references/styling.md) の modifier を呼び出しへ chain するために存在する。返り値を無視するのが通常の使い方。

## リファレンス

- [references/cells.md](references/cells.md) - 組み込み Cell ごとのレシピと、Section・アイコン・全 Cell 共通フィールド。
- [references/updates.md](references/updates.md) - 表示中の画面の更新、行の同一性、可視性、XML からの利用。
- [references/styling.md](references/styling.md) - `Theme`、`CellStyle`、style modifier、list 外観、Header / Footer。
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`、再利用のためのラップ関数、独自 Cell 型と ViewHolder。

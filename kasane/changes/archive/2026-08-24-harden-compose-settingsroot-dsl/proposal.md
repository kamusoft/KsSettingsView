# Proposal: harden-compose-settingsroot-dsl

## Why

`ks-settingsview-compose` の Store 初期値用 builder `settingsRoot { section(...) }` には、初回リリース前に確定しておきたい 2 つの課題がある。

1. `@SettingsRootDsl` が top-level 拡張関数 29 箇所に付与されており、Kotlin 2.4.10 が「関数への DSL marker 付与は効果がない」(KT-81567) と警告する。付与は以前から no-op で挙動には影響しないが、`annotation class SettingsRootDsl` に `@Target` が無いため今後も関数へ付けられてしまう
2. `section(...)` が `headerHeight` / `isHeaderVisible` / `isFooterVisible` を受け取れない。`SettingsRootScope` には `section(...)` 以外に Section を足す経路が無いため、これらを指定したい Section が 1 つでもあると builder 全体を諦めて `SettingsRoot(sections = listOf(...))` を手書きするしかない。core `Section` / iOS `ksSection` / Compose `DSLScope.Section` はすべて揃っており、この builder だけが非対称

利用者向け Skills (package-distribution phase-12) が DSL を文書化する前、かつ `android/` を広く触る phase-5 の着手前に API 面を確定させる。経緯と裏取りは [exploration.md](exploration.md)。

## What Changes

- `annotation class SettingsRootDsl` に `@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS)` を付与し、関数・プロパティへの付与をコンパイルエラーにする
- `DSLHandles.kt` / `BasicCellDsl.kt` / `InputCellDsl.kt` / `CustomCellDsl.kt` の top-level 拡張関数から `@SettingsRootDsl` を削除する (29 箇所)。receiver 型 6 クラスへの付与は維持する
- `SettingsRootScope.section(...)` の 2 オーバーロード (accessory 版 / 文字列ヘッダ版) に `headerHeight` / `isHeaderVisible` / `isFooterVisible` を追加する。文字列ヘッダ版には文字列 `footer` (省略可) も追加し、iOS の文字列版 `ksSection` と揃える。既定値と引数順は core `Section` data class と同一にする
- 上記を固定する単体テストを追加する

影響する capability: `settings-view-android-ui`

## Non-Goals

- スコープ制御 (入れ子ラムダから外側 receiver を暗黙に呼べないこと) をコンパイルテストで固定すること — kotlin-compile-testing 系の新規依存が Kotlin 本体の版追随を縛るため見送り。実装時の手動実証のみ行う
- `SettingsRootScope` に `section(section: Section)` のような pass-through 経路を足すこと
- `DSLScope.Section` / iOS `ksSection` / MAUI 側の変更 (すでに揃っている)
- サンプルアプリの変更 (`StoreDemoScreen.kt` は新引数を使わず、既定値で現行と同じ挙動)
- `docs/` / README の更新 (docs-refresh の責務、別途ユーザーが依頼)

## Impact

- 公開 API: `section(...)` への既定値付き引数追加。名前付き引数・末尾ラムダの既存呼び出しはソース互換。位置引数で `isVisible` を渡している呼び出しがあれば引数順の変更で影響するが、本体・サンプル・テストに該当なし。未配信のためバイナリ互換は考慮しない
- `@Target` 制限は本リポジトリ外の利用者が `@SettingsRootDsl` を関数に付けていた場合にコンパイルエラーになるが、未配信のため該当なし
- 挙動変化: 既存の呼び出しには変化なし (注釈削除はスコープ制御に影響しない。新引数の既定値は現行と同じ)。新引数を指定した呼び出しは、従来この builder からは生成できなかった `headerHeight` / トグル付きの `Section` を生成できる (観察可能な追加)
- リスク: 低。変更はすべて `ks-settingsview-compose` 1 module に閉じる

## 級: M

公開関数への引数追加 (公開 API の小変更) を含むため M。作業量は S 相当。

domain: android

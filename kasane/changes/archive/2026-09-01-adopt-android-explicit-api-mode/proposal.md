# Proposal: adopt-android-explicit-api-mode

## Why

Maven Central へ配る Android 本体 module は、Kotlin の暗黙 public を許したままで、公開 API と内部実装の境界をコンパイラで強制していない。公開 API に現れる外部型の依存漏れが発行物の全走査で初めて見つかった経緯を踏まえ、公開面を宣言上明確にし、意図しない API 追加をコンパイル時に止める。

初回リリース前に現行の公開面を棚卸しし、利用者向け契約ではない宣言を互換性負債になる前に内部へ戻す。

## What Changes

- `android/kssettingsview` で Kotlin Explicit API mode を Strict として有効にし、公開宣言の visibility と必要な型の明示をコンパイルエラーで強制する (android/ADR-0022)
- 公開契約として維持する宣言へ `public` と必要な型を明示する。DSL scope、公開シグネチャに現れる型、Cell の公開 extension point、一括登録 API は公開を維持する
- 内部 Adapter の照会である `KsCellRegistry.viewTypeOf`、テスト・診断用の `KsCellRegistry.isRegistered`、通常コンストラクタと重複する `SettingsRootStore.preview` を `internal` へ降格する
- Strict の陽性対照、debug / release compilation、変更前後の release AAR 公開 ABI 差分、外部 Kotlin code の可視性 probe、既存 Android テストで公開面と挙動を検証する
- 完了後の蒸留では、internal 化した API を `kasane/handbook/cross/user-skill-api-listing.md` の公開 API 掲載除外リストから取り除く。掲載基準そのものは変更しない

## Non-Goals

- `android/kssettingsview-bridge` への Explicit API mode 導入は行わない。Maven 非公開の interop module であり、MAUI binding が必要とする JVM public 面の整理は別の能力・判断を要するため
- iOS の可視性棚卸しは行わない。`ios-effectivestyle-visibility` が独立して扱うため
- iOS と Android の公開 API 可視性を対称化しない。各 platform の利用経路と既存契約を基準に個別判断し、iOS の既存 `SettingsRootStore.preview` と Registry 補助 API は本 change で変更しないため
- 3 platform の消費者プロジェクトや公開レジストリ経路は検証しない。確定した Android 公開面を入力に、package-distribution の消費者検証フェーズが後続で扱うため
- 新しい Cell、Store 操作、描画挙動、利用者向け API は追加しない。今回の目的は既存公開面の明示と縮小に限るため

## Impact

`KsCellRegistry.viewTypeOf`、`KsCellRegistry.isRegistered`、`SettingsRootStore.preview` を外部から直接呼ぶコードにはソース互換性のない変更となる。ただしリポジトリ内の Sample・Bridge・MAUI・利用者向けドキュメントに利用はなく、初回リリース前に実施する。

公開維持する宣言への `public` 明示は ABI と実行時挙動を変えない。一方、Strict compilation だけでは誤った internal 化を検出できないため、変更前後の release AAR 公開 ABI 差分を全走査し、意図した 3 API 以外に公開面の増減がないことを完了条件とする。UI とデータモデルの挙動には影響しない。

## 級: M

単一 Android 能力内の変更だが、公開 API 3 件の可視性引き下げと広範な公開修飾の明示を伴うため。

domain: android

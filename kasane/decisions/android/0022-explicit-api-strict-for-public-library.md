---
id: 0022
title: Android 公開ライブラリは Explicit API Strict で公開境界を強制する
status: accepted
date: 2026-09-01
---

## Context

Android 本体 module は Maven Central へ配る公開ライブラリだが、Kotlin の Explicit API mode が未設定で、暗黙の public 宣言がそのまま公開 API になり得る。公開 API に露出する外部型の依存スコープ漏れは、発行 aar の公開面を手作業で全走査して初めて検出された。今後も公開面の判定を人手だけに委ねると、意図しない公開と依存設計の適用漏れを繰り返す余地がある。

初回調査では、本体の暗黙 public は約 390 宣言ある一方、戻り値型・プロパティ型の明示不足は実質 0 件で、移行差分の大半は既存の公開意図を `public` として明示する機械的な変更になる見込みである。公開面を絞る判断は初回リリース前なら互換性を壊さずに行える。

## Decision

Maven 公開対象の Android 本体 module (`android/kssettingsview`) で Kotlin Explicit API mode を Strict として有効にする。Warning での移行期間は設けず、公開宣言の visibility と必要な型の明示不足をコンパイルエラーとして扱う。

Maven 非公開の interop Bridge module (`android/kssettingsview-bridge`) はこの決定の対象外とする。

## Alternatives Considered

- **Warning で開始する** — 同じ問題を警告として段階的に解消できるが、現行 CI は警告を失敗として扱わず、調査上も型明示不足がほぼ無いため、移行期間を設ける便益が薄い。公開境界の強制が成立しない期間だけが残るため採らない

## Consequences

- 正: 新しい公開宣言は、公開意図と型を明示しなければコンパイルを通らず、意図しない API 公開を追加時点で検出できる
- 正: 公開 API 面が宣言上明確になり、公開型に現れる外部依存を `api` スコープへ置く判断を機械的に追跡しやすくなる
- 負: 初回有効化では約 390 宣言への明示修飾と公開・内部境界の棚卸しが必要になり、広い機械的差分が生じる
- 負: 将来の公開 API 追加では visibility と必要な型の明示が必須となり、実装時の記述量が増える

出典: kasane/changes/adopt-android-explicit-api-mode/exploration.md (検討した選択肢・決定事項・未決の論点)

---
id: 0013
title: リソース参照は宣言元ライブラリの R クラス経由で行う
status: accepted
date: 2026-08-11
---

## Context

`EffectiveStyle.effectiveButtonTitleColorArgb` は ButtonCell タイトル色の 4 段目フォールバックとしてテーマの `colorPrimary` を動的解決するが、この属性を `com.google.android.material.R.attr.colorPrimary` として参照していた。`colorPrimary` の `<attr>` 宣言元は AppCompat であり (material 1.12.0 と appcompat 1.7.0 の aar `res/values/values.xml` を実測して確認)、material の R クラスに載っていたのは R.txt が推移的だったことによる副作用にすぎない。

material 1.13 以降は aar の R.txt が自モジュール宣言分に限られる。この前提が崩れると 2 つの経路で壊れる。Gradle 側で material を 1.13+ に上げるとコンパイルエラーになり、.NET Android (MAUI Binding) では R.txt から Java の R クラスを生成するためフィールドが欠落して実行時 `NoSuchFieldError` になる。後者は Binding 側で実際に発生し、`Xamarin.Google.Android.Material` 1.12.0.5 への版固定で回避していた。

リポジトリ内の `com.google.android.material.R.*` 参照を全数調査したところ (2026-08-11 実測)、他ライブラリ宣言に依存していたのは本 1 箇所だけだった。`SwitchCellViewHolder` の `colorOnPrimary` / `colorOutline` / `colorSurfaceContainerHighest`、`SheetChrome` の `colorOnSurfaceVariant`、Picker 系の `R.id` 25 件と `R.dimen` 2 件は、いずれも material 自身が宣言している。

## Decision

Android モジュールからリソース (attr / id / dimen / style 等) を参照するときは、**そのリソースを宣言しているライブラリの R クラス**を経由する。あるライブラリの R へ他ライブラリのリソースが推移的に載っていることに依存しない。

`colorPrimary` は AppCompat 宣言のため `androidx.appcompat.R.attr.colorPrimary` を参照する。material 自身が宣言するリソース (Material 3 の color role、Picker 内部 View の id 等) は従来どおり `com.google.android.material.R` を経由してよい。

属性名からは宣言元を判別できないため、判定は aar の `res/values/values.xml` (attr / dimen) と `res/layout/*.xml` の `@+id/` (id) を実測して行う。

## Alternatives Considered

- **material の R を参照したまま依存版を 1.12.x に固定し続ける**: 却下。実際に採っていた回避策だが、Gradle 側と Binding 側の双方に版固定が残り、material の更新 (Material 3 の新しい color role・不具合修正) を受け取れなくなる。1 属性の参照形式のために版全体を止める割に合わなさも理由。

## Consequences

- 正: material 1.13+ への引き上げが、この参照を理由にブロックされなくなる。
- 正: .NET Android の R クラス生成でフィールドが欠落する経路が塞がる。同じ症状は Robolectric の JVM テストでは再現せず実機・実行時にしか現れないため、原理の側で塞ぐ価値が大きい。
- 負: 参照を書くたびに宣言元ライブラリを調べる手間が増える。属性名からは判別できず aar の実測が要る。
- 保留: `Xamarin.Google.Android.Material` 1.12.0.5 の版固定は本決定では解除しない。1.13+ への引き上げには別途の互換検証が要る。Binding csproj のコメントは、版を上げる際に material の R 経由の参照が material 自身の宣言だけで済んでいるかを確認する手順として残してある。

出典: colorPrimary 宣言元参照への是正 (S 級・2026-08-11。ユーザー依頼文 / material 1.12.0・appcompat 1.7.0 aar の実測 / review-001〜003)
現行照合: 2026-08-11 確認。EffectiveStyle.kt:459 が `androidx.appcompat.R.attr.colorPrimary` を参照。リポジトリ内の `com.google.android.material.R.*` 参照はすべて material 自身の宣言 (attr 4 件 / id 25 件 / dimen 2 件)。判定: 維持

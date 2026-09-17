---
id: 0032
title: Header / Footer 背景色の既定は両外観とも透明にする
status: accepted
date: 2026-09-17
amends: 0030
---

## Context

Theme の `headerBackgroundColor` / `footerBackgroundColor` の既定は、core/ADR-0030 の Decision 1 により light は従来の固定値 (#F2F2F7)、dark はライブラリ所有の dark セットの値 (#000000) だった。list 下地の既定は light #FFFFFF / dark #000000 で、light では Header / Footer の領域が下地と異なる薄いグレーの帯として描かれる。

この背景色を実際に描画していたのは Android だけで、iOS は Header / Footer 領域の背景に Theme の背景色を適用していなかった (concepts に既知の platform 非対称として記録されていた)。iOS にも反映して両 OS を揃えると、既定値のままでは、何も指定していない iOS 画面 (light) に帯が新たに出る。

移植元の AiForms.SettingsView の `HeaderBackgroundColor` の既定は Transparent である (kasane/concepts/cross/reference/aiforms-spec-summary.md)。オーナーの判断は「既定が透明なら (iOS に反映しても) 問題はない」(2026-09-17)。ライブラリは beta 配信中 (0.1.0-beta.4) である。

## Decision

### 1. 既定値は両外観とも透明

`headerBackgroundColor` / `footerBackgroundColor` の既定は、light / dark のどちらの外観でも透明とし、3 platform で同じにする。何も指定しない Header / Footer の領域には list 下地がそのまま見える。

core/ADR-0030 Decision 1 のうち「Header / Footer の背景」の既定値 (light は現行の固定値・dark は dark セットの生値) だけをこの決定で置き換える。他の既定色と、未指定色を描画時に現在の外観の既定へ解決する仕組み・明示色の扱い・Theme の型は core/ADR-0030 のまま変えない。

### 2. iOS の公開定数は形を保ち値だけ透明にする

iOS の公開定数 `Theme.defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor` は、型・名前・可視性と「両外観の値を持つ `UIColor`」である形を保ち、両外観の値を透明にする。

他の既定色定数と同じ形に揃えたままにすることで、「既定値の定数を明示した Theme は既定と等価、同じ生値の固定色を明示した Theme は既定と等価でない」という core/ADR-0030 の等価性の契約を Header / Footer 背景にもそのまま適用する。

### 3. Android・MAUI・互換経路

- Android はライブラリ所有の light / dark 既定セット (`KsSettingsViewDefaults`) の値を透明にする。未指定の印 (`Color.Unspecified`) と透明は別の値であり、未指定色の解決の仕組みは変えない
- MAUI の facade は変えない。未指定を null で native に渡す現行契約のまま native の既定に追随する
- beta 配信中のため互換経路は置かず、既定の見た目の変化 (Android で、今出ている light の帯が消える) はリリースノートで告知する

## Alternatives Considered

- **既定値を変えずに iOS へ反映する**: 却下。両 OS は一致するが、何も指定していない iOS 画面 (light) に薄いグレーの帯が新たに出る。オーナーが望む「既定では帯が出ない」状態にならない
- **iOS への反映を見送り、既定値の検討ごと別の変更に分ける**: 却下 (オーナー判断)。iOS で背景色の指定が無視される状態と platform 非対称が残る。同じ変更の中で反映と既定値の変更を併せて行う

## Consequences

- 正: iOS の既定の見た目は変わらないまま、iOS でも Header / Footer 背景色の指定が効くようになり、platform 非対称が解消する
- 正: 既定の見た目が移植元 (AiForms) と同じになり、移行者の画面が明示指定なしで揃う
- 負: Android (MAUI の Android を含む) で、何も指定していない画面の light の Header / Footer の帯が消える。core/ADR-0030 が掲げた「既存利用者のライト時の見た目は変えない」の例外になる
- 負: 3 platform の既定値が同じであることの検証 (定数比較テスト) と、既定色の表を持つ concepts (core/styling/style-resolution.md) を新しい値へ追随させる必要がある

## Revisit When

- Header / Footer の領域を下地と区別して見せる既定 (帯) を求める要望が利用者から出たとき

出典: kasane/changes/archive/2026-09-17-scroll-indicator-visible-not-applied/exploration.md (スコープ変更の節) / kasane/changes/archive/2026-09-17-scroll-indicator-visible-not-applied/second-opinion-spec-001.md (core/ADR-0030 との衝突と等価性の指摘) / 探索・提案の会話中の議論 (2026-09-17)

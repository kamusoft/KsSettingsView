---
type: design-tokens
title: Cell共通行のレイアウト
description: Cell種別をまたいで共有する視覚文法とplatform別の行寸法トークン
tags: [styling, layout, cell, design-tokens]
timestamp: 2026-07-18
---

## 共通の視覚文法

設定行は、任意のicon、titleと任意のdescription、valueまたはtrailing control、任意のhintから構成する。

valueとtrailing controlはtitleと同じ段に置き、descriptionはその下に置く。hintはtrailing controlの有無に依存せず、行の右上を基準にする。任意要素がない場合は、そのための空領域を残さない。

共通行は配置と共通Styleの反映を担い、trailing controlの内容と操作はCell種別側が担う。

## 寸法トークン

| トークン | iOS | Android |
|---|---:|---:|
| 最低行高 | 48pt | 60dp |
| iconの既定一辺 | 24pt | 24dp |

最低行高は各プラットフォームの互換性と慣例に基づくため、機械的に同じ値へ揃えない。

## 高さモード

可変高さを既定とし、最低行高または利用者が指定した最低高さを守りながら、内容に応じて自然に伸びる。

固定高さは利用者が明示的に選んだ場合だけ使い、内容の自然高にかかわらず解決済みの高さへ揃える。Cell個別の正の高さ指定は、画面全体の正の高さ指定より優先する。


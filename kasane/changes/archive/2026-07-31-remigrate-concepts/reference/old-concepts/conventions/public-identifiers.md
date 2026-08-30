---
type: policy
title: 公開識別子と配布座標
description: 所有主体・製品・成果物の役割を各エコシステムの識別子へ写像する規約
tags: [conventions, identifiers, publishing]
timestamp: 2026-07-18
---

## 方針

公開識別子は、所有主体・製品・成果物の役割を区別し、各エコシステムの慣例へ写像する。文字列表現を全エコシステムで同一化することは目的としない。

## 識別子の写像

| 対象 | 値または規則 | 表すもの |
|---|---|---|
| Apple bundle ID / Android application ID・namespace | `jp.kamusoft.kssettingsview.*` | 所有主体・製品・用途 |
| Maven `groupId` | `jp.kamusoft` | 組織・project group |
| Maven `artifactId` | `ks-settingsview-*` | 製品に属する個別成果物 |
| .NET namespace | `KsSettingsView.*` | 製品・用途 |

Maven成果物は、`jp.kamusoft:ks-settingsview-core` のように `groupId:artifactId` の組で識別する。製品名は `groupId` に重ねず、個別成果物を表す `artifactId` に含める。

Apple、Android、.NETの個別モジュールやサンプルは、それぞれの接頭辞の下で用途を表す末尾要素を追加して導く。

## 不変条件

- Mavenの`groupId`と`artifactId`の責務を混同しない。
- 完全な公開識別子または配布座標から、所有主体とKsSettingsView製品を判別できる。
- 各エコシステムの大小文字や区切りの慣例を尊重する。
- 後続モジュールは独自の命名体系を作らず、この規則から識別子を導く。

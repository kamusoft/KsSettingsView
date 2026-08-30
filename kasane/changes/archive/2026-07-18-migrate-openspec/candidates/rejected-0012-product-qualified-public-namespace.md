---
id: 0012
title: 公開識別子に製品境界を含める
status: proposed
date: 2026-07-17
supersedes: 0002
review: rejected
review_date: 2026-07-18
---

> **却下（2026-07-18）**: Mavenの`groupId`と`artifactId`の役割を十分に調査せず、現行Gradle値を優先したため却下。ADR-0002を維持し、`groupId`は`jp.kamusoft`、製品・成果物は`artifactId`の`ks-settingsview-*`で表す。本ファイルは採用ADRではなく、誤った提案の監査記録として候補領域に退避する。

## Context

ADR-0002 は所有ドメインを基礎に公開識別子を定め、Maven Central の groupId を `jp.kamusoft` とした。一方、現行の Android ライブラリ群は `jp.kamusoft.kssettingsview` を group として使用している。

`jp.kamusoft` だけでは所有主体は示せても、同じ主体が公開する他製品から KsSettingsView を識別できない。公開座標にも製品境界を含め、後続モジュールが同じ規則から座標を導けるようにする必要がある。

## Decision

公開識別子は所有ドメインと製品名の両方を基礎とする。

- Apple と Android の識別子は `jp.kamusoft.kssettingsview.*` とする。
- Maven の groupId は現行実装を優先し、`jp.kamusoft.kssettingsview` とする。
- .NET 名前空間は `KsSettingsView.*` とする。

各エコシステムで文字列表現を完全に一致させるのではなく、それぞれの慣例に従いながら、所有主体と製品境界を一貫させる。

## Alternatives Considered

- Maven の groupId を `jp.kamusoft` とする案。所有主体は表せるが、公開座標の group 部分だけでは製品を識別できないため採用しない。
- `com.kamusoft.*` を使う案。対応するドメインを所有していないため採用しない。
- 新しい `dev` または `io` ドメインを取得する案。既に所有するドメインで所有主体と製品を表現でき、追加の取得・維持コストを正当化できないため採用しない。
- GitHub アカウント由来の名前空間を使う案。所有ドメインを利用できる状況で識別子が長くなり、kamusoft と製品の境界も弱くなるため採用しない。

## Consequences

- 公開座標の group 部分だけでも、所有主体とKsSettingsView製品の両方を識別できる。
- 現行Androidビルドのgroupと公開方針が一致する。
- 同じ所有主体が別製品を公開する場合も、製品ごとのgroup境界を設けられる。
- ADR-0002、OpenSpec spec、既存文書に記載された `jp.kamusoft` は旧判断となり、参照先を更新する必要がある。
- Maven座標は所有主体だけをgroupIdにする構成より長くなる。

出典: `android/ks-settingsview-core/build.gradle.kts`

出典: `android/ks-settingsview-ui/build.gradle.kts`

出典: `android/ks-settingsview-compose/build.gradle.kts`

出典: `kasane/decisions/0002-public-identifier-namespace.md`

出典: 2026-07-17 ユーザー判断「groupIdは現行優先。jp.kamusoft止まりだと製品識別ができない」

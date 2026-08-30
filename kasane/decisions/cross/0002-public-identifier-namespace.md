---
id: 0002
title: 公開識別子の名前空間
status: accepted
date: 2026-05-06
---

## Context

Apple、Android、Maven Central、.NET で公開する識別子には、後続のパッケージや Sample まで一貫して使える名前空間が必要である。Apple と Google では reverse-DNS 形式が標準的であり、Maven Central では名前空間の所有確認が必要になる。

プロジェクトオーナーは `kamusoft.jp` ドメインを保有しており、Maven Central の DNS TXT 検証を自身で行える。

## Decision

iOS のバンドル ID と Android のパッケージ名には `jp.kamusoft.kssettingsview.*` を使用する。Maven Central の groupId には `jp.kamusoft`、.NET 名前空間には `KsSettingsView.*` を使用する。

Android の命名要件に合わせ、`kssettingsview` は lowercase で連結する。.NET ではプラットフォームの慣例に合わせて PascalCase を使用する。

## Alternatives Considered

- `com.kamusoft.*`。`kamusoft.com` は他者が保有しており取得できないため採用しない。
- `dev.kamusoft.*` または `io.kamusoft.*`。新しいドメインの取得と維持に追加コストが発生するため採用しない。
- `io.github.muak.*`。Maven Central では GitHub アカウント検証を利用できるが、識別子が長くなって可読性が落ち、kamusoft のブランド要素も薄れる。所有ドメインを利用できるため採用しない。

## Consequences

- 所有済みドメインを根拠に、Apple、Android、Maven Central で標準的かつ検証可能な名前空間を使用できる。
- 新しいドメインの取得・維持コストを追加せず、公開識別子に kamusoft と KsSettingsView のブランドを反映できる。
- 各エコシステムの慣例に従うため、識別子は全プラットフォームで同一形式にはならず、lowercase の reverse-DNS、Maven groupId、PascalCase の .NET 名前空間を使い分ける必要がある。

出典: openspec/changes/archive/2026-05-06-add-monorepo-foundation/design.md

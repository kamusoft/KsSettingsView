---
id: 0015
title: kasane にドメイン軸を導入し core + platform で長命層を分割する
status: accepted
date: 2026-07-31
---

## Context

本プロジェクトは `android/` `ios/` `maui/` の platform ビルドルートを持つモノレポである (ADR-0001)。kasane 導入 (ksn-init) の時点ではハーネスに domain 機能が存在せず、`decisions/` と `concepts/` はフラット構造、config.yaml の `skills.impl` には Swift / Kotlin / Compose / C# / MAUI 系の 6 スキルが全系統ぶんフラットに並んでいた。

ksn-core の解決ルールは「用途キーに列挙されたスキルを全部ロード」であるため、iOS 作業のワーカーにも Kotlin / MAUI スキルが届き、指針の混線とコンテキスト浪費が起きる。これは domain-axis 規約 (ksn-core references/domain-axis.md) が明示的に禁じる「フラットな `skills.*` に全系統のスキルを並べる」アンチパターンに該当する。

一方、concepts 21 件の内訳は共通契約 (architecture / core-model / cells / styling で約 17 件) が大半で、platform 固有は `platforms/` の 4 件のみ。ADR も 14 件中ほぼ全てがプラットフォーム横断の契約である。

## Decision

- config.yaml に `domains` を定義し、ドメイン軸を導入する。構成は **`core` / `ios` / `android` / `maui`** (+ 予約ドメイン `cross`)。
  - `core` — 全 platform が共有する契約 (core-model / cells / styling / 共通 architecture)。ADR もほぼここに属する
  - `ios` / `android` — 現 `platforms/ios-*`・`platforms/android-*` および今後の platform 固有知識
  - `maui` — 現状 concepts なし (最初の書き込み時にディレクトリを掘る)
  - `cross` — conventions / docs 運用などリポジトリ横断のメタ事項
- `domain-skills` で platform 別スキルオーバーレイを定義し、共通 `skills.impl` は空に近づける (ios: swift-ui-impl-skill、android: kotlin-impl-skill + jetpack-compose-impl-skill、maui: csharp-impl-skill + maui-skill + maui-native-binding-skill + dotnet-test-skill 等)。
- 本プロジェクトの変更は複数 platform を跨ぐことが多く proposal の `domain:` 欄は `cross` が多くなる見込みだが、規約上「cross の変更は実際に触るドメインの domain-skills を結合する」ため、skill 解決は成立する。

## Alternatives Considered

- **platform のみのドメイン構成 (ios / android / maui、共通契約は cross へ)**: domain-skills の恩恵は得られるが、共通契約 17 件が `cross` に集中して実質フラットのままとなり、「cross = リポジトリ横断のメタ事項置き場」という規約の趣旨からも外れるため却下。
- **現状維持 (domain なし)**: 全系統スキルが全ワーカーに届くアンチパターンと、フラット index の肥大が解消されないため却下。

## Consequences

- 正: 用途キー解決が「共通 + 作業ドメインの結合」になり、platform 別に的確なスキルだけがワーカーに届く。
- 正: index が core / 各 platform / cross に素直に分かれ、無関係ドメインの混入 (コンテキスト汚染) を防げる。将来 platform を別リポジトリへ切り出す場合も持ち出しが容易になる。
- 負: `decisions/` と `concepts/` の再配置という移行コストが発生する (ほぼ core 行きのため ADR 番号は温存できる見込み)。
- 負: 以後の全変更で proposal.md に `domain:` 欄の判定が必須になる。新パッケージ→ドメインの導出規則を `concepts/rules.md` に書く必要がある。

---
出典: 2026-07-31 ksn-explore での議論 (フラット skills.impl の問題提起はオーナー、案2 採用はオーナー判断)

---
id: 0016
title: Android は core / ui / compose を単一 module に統合し `jp.kamusoft:kssettingsview` 1 artifact で配布する
status: accepted
date: 2026-08-21
---

## Context

Android の公開単位は `ks-settingsview-core` / `-ui` / `-compose` の 3 module (+ MAUI 専用の `-bridge`) で、Gradle `group` は `jp.kamusoft.kssettingsview`、module 間依存はすべて `implementation` だった。Maven Central へ配布する (cross/ADR-0018) にあたり、利用者が書く座標・artifact の粒度・groupId を決める必要がある。

accepted の cross/ADR-0002 は Maven groupId を `jp.kamusoft` と定めており、現行 Gradle `group` はそれに未追従の drift だった (kasane/handbook/cross/public-identifiers.md が「Maven 公開を導入する変更で先に解消する」と明記)。

module の依存を実測すると、`ui` は CustomCell の Compose ホスティングのため既に compose runtime / ui / foundation / material3 に依存しており、`compose` module (約 2,500 行) の外部依存は `ui` の部分集合、`core` は約 450 行で compose runtime にのみ依存する。姉妹ライブラリ KsDialogs が Compose API を別 module に分離した理由 (本体を Compose 非依存に保ち、MAUI binding 経由で compose-ui が推移しないようにする。KsDialogs android/ADR-0001) は、KsSettingsView では最初から成立していない。

`bridge` module は MAUI binding のための interop 輸送層であり、アプリ利用者向けの公開契約ではない (concepts/maui/api/native-bridge.md)。唯一の消費者である MAUI binding はリポジトリ内の gradlew で aar をソースビルドする。

## Decision

- Gradle `group` を cross/ADR-0002 のとおり **`jp.kamusoft`** に改める。
- `core` / `ui` / `compose` の 3 module を **単一 Gradle module `android/kssettingsview`** に物理統合し、**`jp.kamusoft:kssettingsview`** の 1 artifact として Maven Central に公開する。利用者が書く座標はこの 1 点で、Compose DSL も同じ artifact に含まれる。
- Kotlin のパッケージ名 (`jp.kamusoft.kssettingsview.core` / `.ui` / `.compose`) は統合後も維持し、層構造はパッケージ名で表す。
- artifactId はブランド名 `kssettingsview` を 1 トークンとして扱い、内部にハイフンを入れない (Maven の慣例: ハイフンはブランドとサブモジュールの境目にのみ使う)。Gradle の project 名・ディレクトリ名も artifactId に揃える。
- `bridge` は別 module (`android/kssettingsview-bridge`) のまま維持し、**Maven には公開しない**。group / version は本体と揃える。
- 発行先は Sonatype Central Portal (`jp.kamusoft` 名前空間は DNS で検証し、KsDialogs と共用する)。

## Alternatives Considered

- **3 module を 3 artifact (`kssettingsview-core` / `-ui` / `-compose`) として公開し、module 間を `api` にして利用者が書く座標を 1 点にする**: 却下。Compose 利用者は別座標 (`-compose`) を書く必要が残り、「1 つにまとめたい」という要件を満たさない。
- **KsDialogs 同型の 2 分割 (本体 + `-compose`)**: 却下。`ui` が既に Compose 一式に依存しているため、分離しても利用者に推移する依存は 1 つも減らず、分割の根拠がない。
- **3 module を維持したまま fat aar で 1 artifact にする**: 却下。AGP に fat aar の公式サポートがなく、サードパーティ plugin は壊れやすい。
- **groupId を現行 Gradle 値 `jp.kamusoft.kssettingsview` にして cross/ADR-0002 を supersede する**: 却下。座標 `jp.kamusoft.kssettingsview:kssettingsview` は製品名が二重になり、KsDialogs (`jp.kamusoft:ksdialogs`) とも揃わない。名前空間検証も `jp.kamusoft` 1 回で kamusoft の全ライブラリを賄える。
- **artifactId を現行どおり `ks-settingsview`**: 却下。ブランド名の内部にハイフンが入り慣例から外れる。Android namespace (`jp.kamusoft.kssettingsview.*`) との読みも揃わない。
- **`bridge` も Maven に公開する**: 却下。利用者向け契約ではなく、公開すると API 安定性の責任だけ増える。

## Consequences

- 正: 利用者の案内は `implementation("jp.kamusoft:kssettingsview:x.y.z")` の 1 行で済み、Compose 利用でも座標は変わらない。
- 正: 座標 (`jp.kamusoft` + `kssettingsview`) と Android namespace (`jp.kamusoft.kssettingsview.*`) の読みが一致し、KsDialogs と同型になる。
- 正: MAUI binding が束ねる aar が 3 本から 2 本 (`kssettingsview` + `kssettingsview-bridge`) に減る。利用者に推移する依存は変わらない。
- 正: module 間依存の公開スコープ (`api` / `implementation`) の問題が消える。
- 負: Android 側で module 境界による層の compile 時強制を失う (iOS の target 分割は残るので、core 契約をまたぐ誤った依存は iOS 側で検出される)。
- 負: core (JUnit 5) と ui / compose (JUnit 4 + Robolectric) のテスト基盤を 1 module に同居させる必要がある。
- 負: ディレクトリ改名・統合に伴い `android/settings.gradle.kts`、Sample の composite build 置換、MAUI binding csproj の aar パス、concepts / README の表記を追随させる。
- 負: kasane/handbook/cross/public-identifiers.md の artifactId 規則 (`ks-settingsview-*`) を改訂する。
- 負 (2026-09-01 実装結果): 「依存 1 行で完結」の成立には、公開 ABI に露出する外部型の依存を `api` スコープにする仕分けが必要 (実装では androidx.recyclerview / compose foundation-layout の列挙漏れが 2 度検出され、公開宣言の全走査で確定した)。module 統合により公開/内部の境界がコンパイラで強制されない問題は Explicit API mode の別変更 (changes/adopt-android-explicit-api-mode) で扱う。
- 中立 (2026-09-01 実装結果): テスト基盤の同居は JUnit Platform + junit-vintage-engine で既存テスト無改変のまま成立した (統合前後で 2700 件一致・全 green)。

出典: kasane/roadmaps/package-distribution/exploration.md (D) / kasane/decisions/cross/0002-public-identifier-namespace.md / kasane/handbook/cross/public-identifiers.md (Maven 座標の現在地)
出典 (2026-09-01 実装結果の追記と accepted 昇格): kasane/changes/archive/2026-09-01-add-android-maven-distribution/deviation.md / 同 review-001.md / 同 verify-001.md

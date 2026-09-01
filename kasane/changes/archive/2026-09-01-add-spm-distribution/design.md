# Design: add-spm-distribution

## Context

配布方針 (配信リポジトリへのスナップショット配布・umbrella product・tag 表記) は cross/ADR-0018 / 0019 / 0020 とフェーズ議論 (package-distribution/phase-4) で確定済み。本 design が扱うのは、その実装に伴う運用上の安全契約と検証方法 — セカンドオピニオン (second-opinion-spec-001.md) で指摘された穴の埋め方 — に限る。

## Goals / Non-Goals

proposal.md のとおり (重複記載しない)。

## Decisions

### Decision 1: 同期スクリプトの安全契約

**採用案:** 同期スクリプトは破壊的操作 (`.git/` 以外の除去) の**前に**次を全件検証し、1 つでも失敗したら同期先を一切変更せず異常終了する:

1. コピー元 5 点 (`ios/Package.swift` / `ios/Sources/` / `ios/Tests/` / ルート `LICENSE` / README テンプレート) がすべて存在する
2. 同期先は canonical path 化した上で git top-level ディレクトリである (`git rev-parse --show-toplevel` が同期先自身を指す)
3. 同期先の `origin` remote URL が配信リポジトリ (`KsSettingsView-SPM`) を指す
4. 同期先が monorepo 自身・その祖先ディレクトリでない

git metadata (HEAD / index / refs / remote 設定) には触れず、ネットワーク操作も行わない。

**理由:** 引数の誤指定 (monorepo 本体・無関係のディレクトリ) で任意の作業ツリーを消去し得る構造を、実行時検証で塞ぐ。remote URL 検証は「配信リポジトリ以外では絶対に発火しない」を機械的に保証する最も安い手段。

**代替案:**
- **A: 検証なしの単純同期 (rsync --delete 相当)** — 誤指定 1 回で回復困難なデータ消失。却下
- **B: 対話確認プロンプト** — phase-8 で CI から呼ぶ経路が成立しない。却下

### Decision 2: 検証用 tag のライフサイクル

**採用案:** https 解決確認は prerelease tag (`X.Y.Z-alpha.N` 形式) で行い、**検証完了後に tag を削除**する。検証の証跡 (解決ログ・実行記録) は change 側に保存する。失敗時も後始末として tag を削除してから原因対応する。

**理由:** public リポジトリに tag が残ると iOS だけが外部から解決可能な公開版になり、lockstep (ADR-0019) / tag-last (ADR-0020) と緊張関係になる。削除すれば production リポジトリ 1 つで完結し、公開状態が残らない。

**代替案:**
- **A: tag を残す** — prerelease は SwiftPM の範囲指定 (`from:`) では拾われず実害は小さいが、「publish 全成功後にのみ tag が生まれる」原則の例外が恒久化する。却下
- **B: 検証専用リポジトリ (`-staging`) で確認する** — リポジトリが 1 つ増える上、production と別経路の検証になり「本番経路を検証した」価値が下がる。却下
- **C: phase-8 の全 platform prerelease で検証する** — phase-4 のゴール (https 解決確認) が phase-8 まで遅延し、フェーズ分担の決定と矛盾する。却下

### Decision 3: 配信リポジトリ初期設定の検証方法

**採用案:** 初期設定 (visibility / default branch / Issues・Wiki・Projects・Discussions 無効 / description・Website) は `gh api` (`gh repo view` 含む) で機械的に照合し、その出力を受け入れの証跡とする。README の誘導先 URL も内容照合する。

**理由:** 外部状態はリポジトリ内のテストで担保できないため、受け入れ基準を「設定した」ではなく「API で観測した」に置く。

**代替案:**
- **A: 手動目視のみ** — 受け入れ基準にならず、設定漏れがすり抜ける。却下

### Decision 4: iOS テストと消費者検証の実行経路

**採用案:** 受け入れ条件のテスト実行は iOS Simulator destination の `xcodebuild test` (検証 CI `verify-ios.yml` と同じ経路) とし、実行テスト件数が 1 件以上であることを確認する。https 消費者検証も iOS Simulator 向けにビルドし、3 module それぞれの公開型を最低 1 つ参照するコードで配線を確認する。

**理由:** `swift test` は macOS ホスト実行で UIKit ガード内のテストが除外され空振りする (handbook cross/test-execution.md・ADR-0026 に反する)。import 文だけの消費者コードでは product の配線を検証できない。

**代替案:**
- **A: `swift build` / `swift test` (macOS ホスト)** — UI 系テストが 0 件で成功し得る。却下

## Risks / Trade-offs

- remote URL 検証 (Decision 1) はリポジトリ rename に弱いが、rename 時はスクリプト定数の更新で追随する (発生頻度は極小)
- tag 削除 (Decision 2) 後、配信リポジトリは phase-8 の初回リリースまで「commit はあるが tag がない」状態になる — 意図した状態であり README の誘導文言がそれを補う

## Migration Plan

新設のみで移行はない。product 削除の monorepo 内消費者への影響は同一 change 内で追随する (proposal 参照)。

## Open Questions

なし。

## ADR 候補

なし — Decision 1 / 3 / 4 はスクリプト・検証手順の局所契約、Decision 2 は一度きりの検証手順であり、いずれも選別 3 基準 (覆すコスト高 / 境界を越える / 将来を制約) に該当しない。配布方針レベルの決定は既存 ADR (cross/0018 / 0019 / 0020) が既に保持している。

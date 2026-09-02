---
id: 0026
title: iOS の icon 後片付けは解決時の所有権分類で守る (共有 UIImage を破棄しない)
status: accepted
date: 2026-08-25
---

## Context

maui/ADR-0015 の icon 実体化では、解決結果を `KsImageLease` として破棄管理する。この破棄 Action の意味論は OS で真逆であることが dotnet/maui 10.0.70 実ソースの調査で確定した:

- **Android**: 破棄 = Glide 参照カウントのデクリメント。リース1つ = 参照1つで、片方の破棄は他方に無害。破棄はむしろ必須 (スキップは即リーク — MAUI ソース内 TODO コメントが「C# 側が dispose しないと Glide キャッシュエントリが解放されない」と明記)
- **iOS**: file / font / uri / stream の4サービス全てが `() => image.Dispose()` の一発破壊。参照カウントは無い

iOS の file 経路のフォールバック `UIImage.FromBundle` (asset catalog 画像・拡張子なしファイル名で主経路 `CGImageSource.FromUrl` が失敗した場合) は、UIKit の名前付き画像キャッシュが所有する同一 UIImage を返すことを probe で実測した (Simulator / iOS 26.0.1、`ReferenceEquals` と native handle が全ケースで同時一致)。共有ケースでは複数の `KsImageLease` が文字どおり同一 managed オブジェクトを保持するため、片方の破棄が他方の icon 表示を壊す。

さらに、cache 所有画像への `Dispose()` は native メモリを解放せず (キャッシュが retain を保持)、共有 managed peer を無効化して他リースの表示を壊す効果しかない — **破棄という操作自体が所有権の誤認**である。

## Decision

- iOS の `KsImageResolver` は file 経路の解決結果ごとに所有権を分類する (`KsFileImageOwnership`、platform 非依存の純ロジック)。**キャッシュ所有** (UIKit の名前付き画像キャッシュが実体を所有) と分類した画像には後片付け口を付けない (破棄 no-op、寿命は UIKit が管理)。**facade 所有**の画像 (CGImageSource / URI / stream / font 経路) は従来どおりリース破棄時に即時後片付けする
- 分類は「照合キーを MAUI と同じ形 (`Path.GetFileNameWithoutExtension` — ディレクトリと拡張子を落とした名前) に揃えたキャッシュへの引き直し + `ReferenceEquals` による実体同一性の確認」**のみ**で行う。`File.Exists` 等で MAUI 内部の解決分岐を推し量る短絡は置かない — 分岐を推し量る実装は、推し量る先が変わると黙って壊れる
- 誤分類・判定材料の欠如 (null / 空名等)・分類中の例外は、すべて「破棄しない側」(一時リーク、GC / UIKit purge で回収可能) に倒し、表示破壊側には決して倒さない
- `StoreIcon` は、再解決が表示中と同一の画像インスタンスを返した場合、native への差し替え配信を行わず旧リースをその場で解放する (退役キューに積まない)
- Android の破棄経路は現行維持 (変更しない)

## Alternatives Considered

- **facade 全体の所有権モデル見直し (L 級)** — 却下。守るべき穴が iOS の FromBundle フォールバック1経路に絞られたため過剰装備。MAUI 側が既に per-request でほぼ分離している
- **対処なしで許容** — 却下。probe 実測で共有が再現した (非再現なら許容で閉じる条件付き出口だったが、使われなかった)
- **両 OS 一律の `ReferenceEquals` 破棄抑止** — 却下。Android の破棄は Glide 参照カウントのデクリメントであり、スキップは即リーク
- **プロセス全域 static の画像単位参照カウント表 (`KsSharedImageRegistry`)** — 当初この設計で実装したが廃止。FromBundle 画像は UIKit キャッシュ所有で `Dispose()` に native 解放効果がなく (共有 peer の無効化のみ)、参照カウントで破棄を遅延すること自体が所有権の誤認だった。リース放棄時に static 表が画像を恒久強参照する退行 (相方レビュー指摘) も構造ごと解消された
- **`File.Exists` による短絡 (MAUI `FileImageSourceService` の分岐ミラー)** — 却下。短絡は「破棄する側」を無検証で確定するため、MAUI の内部分岐と食い違った瞬間に「誤分類は破棄しない側にだけ倒す」という安全側不変条件が崩れる。実際に「実ファイルはあるが復号不能」ケースの誤評価 (復号失敗は例外にならずキャッシュ引きへフォールバックする) がレビューで反証された

## Consequences

- 正: 共有 UIImage の表示破壊経路が構造的に消える — キャッシュがその名前でその実体を持っていないと確認できたときにだけ後片付け口を付けるため、判定の失敗はすべて安全側に落ちる
- 正: 分類は解決ごとに完結し状態を持たないため、解決口の世代交代 (Host 再接続)・異なる SettingsView 間の共有も追加機構なしでカバーされる
- 正: `KsFileImageOwnership` は net10.0 純ロジック (maui/ADR-0009 のテスト戦略) で、ミューテーションで 6〜13 件の検出力を実測済み
- 負: ファイルから起こした facade 所有画像でも、同名の資産があると分類の引き直しが名前付き画像キャッシュへの常駐を1件増やし得る (UIKit 管理でメモリ逼迫時に purge される)。表示破壊の可能性と引き換えに受け入れた
- 負: iOS resolver の配線1行 (`cacheOwned ? null : result`) は net10.0 テストの射程外で、実行時証跡のみで守られている (将来 iOS platform テスト基盤を持つ場合の最初の対象候補)
- 負 (2026-09-02 判明): 照合キーを「ディレクトリと拡張子を落とした名前」に揃える分類は、MAUI の `ImageSourceExtensions.GetPlatformImage` が 10.0.60 以降でそう引くという内部挙動に依存する (10.0.30 以前は生のファイル名で引く。10.0.40 / 10.0.50 は未確認)。facade が要求する `Microsoft.Maui.Controls` 10.0.70 (`maui/Directory.Packages.props` の宣言元) がこの前提を保証しており、要求版を 10.0.60 未満へ下げる決定は本 ADR の安全側不変条件を崩す

出典: kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/exploration.md (調査結果・probe 実測・決定事項) / kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/deviation.md ([設計判断] 項) / kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/review-004.md
出典 (2026-09-02 MAUI 要求版への依存の追記): kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/history.md (2026-09-02: facade が要求する MAUI 本体の最低版) / kasane/changes/archive/2026-09-02-add-maui-nuget-distribution/design.md (Decision 7)

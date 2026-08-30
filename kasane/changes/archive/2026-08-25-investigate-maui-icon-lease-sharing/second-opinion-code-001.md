# セカンドオピニオン: investigate-maui-icon-lease-sharing (code-001)
**相方**: codex / **label**: so-code-investigate-maui-icon-lease-sharing / **日付**: 2026-08-25 / **対象**: 未コミットの working tree 変更 (KsSharedImageRegistry.cs 新規、iOS KsImageResolver.cs / KsSettingsController.cs / テスト Fake 群の変更、テスト 2 ファイル新規)
---
# レビュー結果: investigate-maui-icon-lease-sharing

**判定**: **CHANGES_REQUESTED**

Critical 0件 / Major 2件 / Minor 1件 / Suggestion 0件。

提示された検証結果（462件成功、両platformビルド警告0）を前提とした静的レビューです。指定どおりファイル作成・ビルド・テスト実行は行っていません。

## 指摘事項

### [🟠 Major] static registry が破棄されなかった画像をプロセス終了まで強参照する

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:28`, `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:30`, `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:126`, `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:284`

**問題点**: `Shared` の通常の `Dictionary` は画像キーと全 disposer を強参照します。一方、返却する `Handle` には finalizer がなく、`ReleaseHost()` は仕様どおり画像リースを維持し、`SettingsView` にも恒久離脱時の終端破棄経路がありません。

そのため、ページが恒久的に破棄されて controller と lease がGC対象になっても `Handle.Dispose()` は呼ばれず、registry のカウントは減りません。static dictionary だけが画像と `IImageSourceServiceResult` をプロセス終了まで保持し続けます。しかもiOS resolverは共有画像だけでなく、毎回別インスタンスになる file／URI／stream画像もすべてregistryへ登録します。

これは既知だった「明示的な後片付けが走らない可能性」を、確実なプロセス寿命の強参照リークへ悪化させます。

**推奨修正**: registry 自体が画像の生存理由にならない所有構造にしてください。例えば `ConditionalWeakTable` 等の弱いキーによる管理、または全リースを確実に終端解放できるlifecycle設計が必要です。明示破棄せず `SettingsView` を到達不能にした場合に、画像・resultがstatic registryから強参照され続けないことを `WeakReference` ベースのテストで固定してください。

### [🟠 Major] disposer例外で後続の退役リースが解放されず、static entryが残留する

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:107`, `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1639`

**問題点**: registryはdisposerの失敗を `AggregateException` として伝播します。一方、`DisposeRetired()` は `_retiredIcons` を先に空にしてから、例外処理なしで各leaseを破棄しています。

異なる画像A・Bが同時に退役し、Aのdisposerが失敗すると、ループがそこで終了してBのleaseは破棄されません。退役キューからは既に除去されているため再試行もなく、Bのregistryカウントとdisposerがstatic表に残留します。「後片付け口の例外時もカウント整合を壊さない」というtasks 2.1の不変条件を、controller統合後には満たしていません。

**推奨修正**: `DisposeRetired()` でもlease単位に例外を回収し、全leaseの解放を試みた後でまとめて伝播するか、後片付け例外を握りつぶす方針へ統一してください。異なる2画像を退役させ、先頭のdisposerが失敗しても後続が破棄され、両entryが除去されるcontrollerレベルのテストを追加してください。

### [🟡 Minor] explorationの「未決の論点」が完了済みの状態と矛盾している

**該当箇所**: `exploration.md:178`, `exploration.md:179`

**問題点**: 対処方針は同文書の「決定事項」で確定し、probe実測とFake拡張も完了していますが、「未決の論点」では方向性が未決、probeが未了と記載されたままです。tasksの全件完了および前段の実測記録と矛盾し、後続の蒸留時に未解決事項として誤認されます。

**推奨修正**: 3・4を解決済みとして取り消し線にし、採用方針・probe結果・実装済みテスト足場への短い参照へ更新してください。

## サマリー

参照同一性による集約、resolver世代・SettingsView境界をまたぐ共有、同一画像再解決時の即時解放、stale completion、複数disposerの実行については、仕様に対応した実装とテストが揃っています。iOSのbefore/after実測も実際の配線を補完しています。

一方、プロセス全域static化による強参照リークと、例外伝播時の退役キュー取りこぼしはリソース管理上の実害があるため、承認できません。

## アクションプラン

1. static registryが画像・resultの寿命を無期限に延長しない構造へ変更する。
2. `DisposeRetired()` を全lease解放保証のある例外処理へ修正する。
3. GC放棄経路と複数画像＋disposer例外の回帰テストを追加する。
4. `exploration.md` の完了済み論点を整理する。

## 突き合わせ結果 (2026-08-25, ksn-orchestrator)

ホスト側: review-001.md (CHANGES_REQUESTED / Major 1・Minor 1・Suggestion 3)

| # | 指摘 | 出典 | 採否 | 重要度 |
|---|---|---|---|---|
| 1 | `DisposeRetired` が退役キューを先に空にし、disposer 例外で後続リースが解放されず static entry が残留する | 双方一致 (ホスト Minor / 相方 Major) | **確定** | Major (高い方を採用) |
| 2 | `Release` が手放された後片付け口を `Disposers` から除かず、Host 再接続ごとに口が無限成長する (ホスト側 probe 実測: 再接続 50 回で 51 個保持) | ホストのみ | **確定** | Major |
| 3 | exploration.md「未決の論点」3・4 が完了済みの状態と矛盾し、蒸留時に未解決と誤認される | 相方のみ | **採用** | Minor (根拠明確・数行の記録整備) |
| 4 | static registry がリース放棄 (未破棄のまま到達不能) 時に画像・result をプロセス寿命まで強参照する | 相方のみ | **降格** | — |

指摘 4 の降格根拠: 実害シナリオは「リースを破棄しないまま放棄する」という facade のリース破棄契約 (UI スレッド契約・明示所有) の違反を前提とする。契約下では全 acquire に release が対応し、指摘 1・2 の修正後はカウント 0 でエントリが除去され残留しない。所有をプロセス全域 static とする構造は proposal の決定事項 (解決口の世代交代・SettingsView 間共有のカバーが目的) であり、所有権モデルの見直しは proposal Non-Goals で却下済み — 採用は承認済み提案の再審に相当するため修正サイクルは回さない。残余リスク (契約違反の呼び出し元では、従来のリース単体リークに比べ static 表が GC ルートになる分だけ保持が重くなる) は完了報告に出典付きで記載し、オーナー判断に委ねる。非共有画像も registry を経由する点は設計どおり (spec「非共有画像は従来どおり直ちに後片付けされる」を Scenario として固定済み・エントリは破棄時に即除去)。

### 再審 (2026-08-25, オーナー指摘による)

指摘 4 の降格を撤回する。降格根拠「実害はリース破棄契約の違反が前提」は誤り — コード確認の結果、`SettingsView` に恒久離脱時の終端破棄経路は存在せず (`DisconnectHandler` → `ReleaseHost()` は設計どおり icon リースを保持、全破棄の `Disconnect()` は private で接続失敗時のみ)、通常のページ使い捨てナビゲーションでリース放棄が発生する。利用者に守れる契約は無く、相方の指摘は正しかった。対応はオーナー決定により所有権分類方式へ設計変更 (deviation.md 参照) — cache 所有画像を破棄対象から外し registry 自体を廃止することで、指摘 4 の退行を構造ごと解消する。本見逃しは lessons/inbox/counterpart-finding-downgraded-on-unverified-premise.md に捕捉済み。

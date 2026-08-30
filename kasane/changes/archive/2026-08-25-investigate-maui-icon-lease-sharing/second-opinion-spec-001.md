# セカンドオピニオン: investigate-maui-icon-lease-sharing (spec-001)

**相方**: codex / **label**: so-spec-icon-lease-sharing / **日付**: 2026-08-24 / **対象**: 提案一式 (proposal.md / specs/maui-cells/spec.md / tasks.md、参考: exploration.md)

---

# レビュー結果: investigate-maui-icon-lease-sharing

**日付**: 2026-08-24
**判定**: **NEEDS_DISCUSSION**

## サマリー

方向性は妥当ですが、probe 不成立時の終了条件とデルタスペックが矛盾しています。また、共有表の所有範囲・画像同一性・複数 disposer の扱いが未確定で、主要シナリオを満たさない実装でもテストを通せる状態です。

指摘は Critical 0件、Major 5件、Minor 2件です。指定どおりビルド・テストおよびファイル作成は行っていません。

## 指摘事項

### [🟠 Major] probe 不成立時の完了経路がデルタスペックを満たさない

**該当箇所**: `proposal.md:14`, `proposal.md:31`, `tasks.md:6`, `specs/maui-cells/spec.md:5`

**問題点**: proposal/tasks は probe で再現しなければ実装・テストを行わず終了できる一方、デルタスペックは共有画像の安全性を無条件の `SHALL` として追加しています。この経路では仕様を満たす実装も Scenario 対応テストも存在せず、検証不能です。また、単一環境での非再現は、別の iOS バージョン・asset・実機で共有されないことの証明にはなりません。

**推奨修正**: probe を proposal 確定前の探索ゲートとして実施してください。再現時だけ現在の Requirement を持つ変更を確定し、非再現時は ADDED Requirement を残さない probe-only の調査として閉じるのが明確です。非再現を受容根拠にする場合は、対象 OS・実機/Simulator・asset 種別・反復数と残余リスクの受容を明記してください。

### [🟠 Major] 共有表の所有範囲と画像同一性が未決のままになっている

**該当箇所**: `proposal.md:15`, `exploration.md:86`, `maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:24`, `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:284`, `maui/KsSettingsView.Maui.Tests/Fakes/GatewayScope.cs:25`

**問題点**: 探索では「resolver 内 static か controller 所有か」「native handle か managed 参照か」が未決です。実環境では Host 生成ごとに新しい `KsImageResolver` が作られる一方、旧リースは `ReleaseHost` をまたいで保持されます。したがって resolver インスタンス単位の表では、スペックの再接続シナリオを保護できません。さらに既存 `GatewayScope.Reconnect()` は同じ Fake resolver を再利用するため、この欠陥をテストで隠します。

**推奨修正**: 少なくとも Handler/resolver 世代をまたぐ共有表の所有者を仕様または proposal で確定してください。異なる `SettingsView` 間も保護対象かを含めてスコープを決め、2つの resolver インスタンスを使うテストを追加してください。probe は `ReferenceEquals` と native handle の双方を記録し、実際に一方の破棄が他方を壊す同一性に基づいてキーを決定すべきです。

### [🟠 Major] 同一画像を包む複数の disposer をどう所有するか決まっていない

**該当箇所**: `proposal.md:15`, `specs/maui-cells/spec.md:7`, `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:40`

**問題点**: 複数回の解決では、同じ `UIImage` に対して解決ごとの `IImageSourceServiceResult` が存在し得ます。全 result を最後に破棄すれば同じ `UIImage.Dispose()` を複数回呼び、1つだけ破棄すれば他 result の `IDisposable` 契約を意図的に履行しないことになります。現在の「最後に `handle.Dispose()`」では、どの handle を保持・破棄・抑止するか実装者が安全に決められません。

**推奨修正**: FromBundle 経路の result が所有する資源を根拠付きで明記し、最初の disposer を代表として1回だけ実行するのか、別の所有移管を行うのかを確定してください。異なる2つの `DisposeProbe` が同一画像を包むテストで、「途中は0回、最後に合計1回」を固定してください。

### [🟠 Major] 同一 Cell シナリオの WHEN が現行 controller では発生しない

**該当箇所**: `specs/maui-cells/spec.md:9`, `tasks.md:17`, `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1609`

**問題点**: `StoreIcon` は新旧の `Image` が同一なら `MarkContentDirty` を呼びません。したがって再解決で同じ画像が返っても「差し替えの配信」は起きず、旧リースは `_retiredIcons` に残ったまま、別の更新が `DisposeRetired` を呼ぶまで破棄されません。Scenario の WHEN と controller レベルテストの駆動条件が実装と一致しておらず、tasks に controller の変更もありません。

**推奨修正**: 次のどちらを契約にするか決めてください。

- 新リースを登録した後、同一画像なら旧リースを直ちに解放する。その場合は controller 変更を tasks に追加する。
- 現行の遅延退役を維持し、どの後続イベントで退役キューを排出するかを Scenario に明記し、最終的な解放まで検証する。

### [🟠 Major] テスト計画が即時破棄経路と iOS 配線を検証できない

**該当箇所**: `tasks.md:15`, `tasks.md:17`, `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1562`, `maui/KsSettingsView.Maui.Tests/Fakes/FakeImageResolver.cs:58`, `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:38`

**問題点**: `CompleteIcon` は追い抜かれた結果、旧 Host 世代の結果、除去済み Cell の結果をその場で破棄します。これらが表示中の画像と共有されるケースは、保存済みリースの差し替え・Cell 除去より直接的ですが Scenario にありません。また Fake は既に同じ `object` を複数の `CompleteTracked` に渡せるため、task 3.1 の前提は古い一方、Fake 経由では iOS resolver の参照カウント配線自体を通りません。platform ファイルは net10.0 テスト対象外です。

**推奨修正**: 少なくとも以下を追加してください。

- 表示中の画像と同じ画像を返した stale completion の即時破棄
- Host 世代をまたいだ completion の即時破棄
- 異なる resolver インスタンス間の共有
- 参照カウント機構を iOS resolver が実際に使用することを確認できる platform テストまたは注入 seam

task 3.1 は「同一画像を返す機能追加」ではなく、共有 registry と複数 disposer を明示的に注入・観測する足場へ再定義すべきです。

### [🟡 Minor] 「リーク方向に倒れる」が検証可能な不変条件になっていない

**該当箇所**: `proposal.md:30`, `tasks.md:10`, `specs/maui-cells/spec.md:25`

**問題点**: 「バグ時は解放漏れ方向」という記述だけでは、実装やテストの合否を判定できません。二重 `Dispose`、カウントの underflow、0件になった共有表エントリの残留、後片付け例外時の状態が未定義です。

**推奨修正**: 「各リースの破棄は冪等」「カウント0でエントリを除去」「実際の disposer は最大1回」などを明示し、単体テストへ対応付けてください。disposer が例外を投げた場合に伝播するか握りつぶすかも決めてください。

### [🟡 Minor] Android 付随確認に合否基準がない

**該当箇所**: `proposal.md:17`, `tasks.md:21`, `exploration.md:36`

**問題点**: 「分析し、結論と根拠を記録する」だけでは、順序保証が成立しないと判明した場合の扱いも、何を確認すれば成立と判断できるかも決まっていません。

**推奨修正**: 「native 更新呼び出し完了後にのみ `Dispose` が要求され、main-looper post はその後片付けをさらに後ろへ送るため先行しない」など、確認対象のイベント順序を明記してください。不成立なら実装停止・別 change 起票・本 change への修正同梱のどれにするかも定めてください。

## アクションプラン

1. probe を変更仕様の前提ゲートにするか、非再現時にも Requirement を保証するかを決定する。
2. 共有表のキー、所有範囲、複数 disposer の所有規則を確定する。
3. 同一画像時の `StoreIcon` の退役タイミングを Scenario/tasks と一致させる。
4. stale completion・resolver 世代跨ぎ・iOS 配線のテストを追加する。
5. 検証可能な参照カウント不変条件を明文化してから実装へ進む。

---

## 突き合わせ結果

ホスト側自己レビュー (2周、指摘ゼロ) との突き合わせ。全指摘が「相方のみ」であり、根拠 (該当箇所特定 + 実害シナリオ) で採否を判定した。

| 指摘 | 採否 | 判定根拠 |
|---|---|---|
| Major-1: probe 不成立経路と spec の矛盾 | **採用** | アーティファクト間の構造矛盾として実在。非再現時はデルタスペックを撤回して probe-only で閉じる旨と、非再現の記録要件 (環境・反復・残余リスク受容) を明文化する |
| Major-2: 共有表の所有範囲・キー未決 | **採用** | ReleaseHost 後もリースが生存し Host 再接続で resolver が作り直される事実と整合。exploration 論点6 を proposal 段階で確定すべきという指摘は正当。設計判断としてオーナーに提示 |
| Major-3: 複数 disposer の所有規則未決 | **採用** | 「最後に handle.Dispose()」の曖昧さは実装者が安全に決められない。破棄の意味論 (全 handle をまとめて実行 / 代表1回) を spec の不変条件として明文化する |
| Major-4: 同一 Cell シナリオの WHEN が発生しない | **採用** | `StoreIcon` の dirty 抑止により配信が起きず premise 不成立 (lessons spec-review L-001 の型)。契約の選択 (即時解放 vs 遅延退役の明記) を設計判断としてオーナーに提示 |
| Major-5: stale completion・世代跨ぎ・iOS 配線の検証欠落 | **採用** | `CompleteIcon` の即時破棄経路は共有時に最も直接的な破壊経路で Scenario 欠落は事実。task 3.1 の前提 (Fake が同一 object を渡せない) が古い点も事実。iOS 配線の検証は probe ハーネスでの確認を最低線とし、platform 自動テストの導入可否は実装フェーズ判断 |
| Minor-1: リーク方向の不変条件化 | **採用** | 冪等・underflow・エントリ除去・例外方針を spec/tasks に明文化 |
| Minor-2: 4.1 の合否基準 | **採用** | 成立条件 (イベント順序) と不成立時の扱いを tasks に明記 |

降格・未解決: なし。判定 NEEDS_DISCUSSION の討議点 (Major-2 の所有範囲、Major-4 の契約選択) はオーナー判断へ。

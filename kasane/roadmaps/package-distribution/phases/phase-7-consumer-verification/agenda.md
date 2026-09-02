# phase-7-consumer-verification

配布物を参照する消費者プロジェクトを `verification/` に platform ごとに持ち、publish 前の dry-run (ローカルフィード) と publish 後の smoke (実レジストリ) で配信経路を検証できるようにする。Sample はソース参照のまま維持する。

## 論点

- `verification/` の構成 (iOS / Android / MAUI それぞれ最小 app、LabelCell 1 個程度) と、README のインストール手順の雛形を兼ねる書き方
- ローカルフィード方式: SwiftPM は配信リポジトリの prerelease tag または生成したスナップショットの `path:` 参照 (monorepo の file:// ではない)、Android は mavenLocal (または Central Portal の保留状態)、MAUI はローカルフォルダフィード
- dry-run と smoke の切り替え方 (参照先をパラメータ化するか、2 構成を持つか)
- CI からの起動方法 (phase-8 の release workflow の publish 前ステップ / publish 後ジョブ) と検証範囲 (解決・ビルドまでか、起動まで含めるか)
- MAUI 消費者でのトランジティブ依存の確認 (binding 2 件、AndroidX 版)
- cross/ADR-0016 (Sample パリティ) との関係: verification/ はパリティ対象外とする整理

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
- [ ] **phase-9 からの申し送り** (2026-08-30): `verification/` の消費者プロジェクトに、ルート README / `skills/` と同じ最小コード例を入れて CI でビルドさせる。README の例は現状 `skills/` の Skill 本文との文字列一致でしか担保されておらず、実際にビルドが通るかは未検証
- [x] **phase-5 からの申し送り・人の作業 (オーナー)** (2026-09-01): Central Portal アカウントで `jp.kamusoft` 名前空間を登録し DNS TXT 検証を通す → 2026-09-01 完了 (Portal で namespace 追加 → kamusoft.jp の apex に検証用 TXT を追加 → Verified → TXT は削除済み)。KsDialogs 含む `jp.kamusoft` 配下で共用できる
- [ ] **phase-5 からの申し送り** (2026-09-01): Explicit API mode の導入 ([changes/archive/2026-09-01-adopt-android-explicit-api-mode](../../../../changes/archive/2026-09-01-adopt-android-explicit-api-mode/exploration.md) に簡易起票済み → 2026-09-01 に M 級として実装・蒸留済み。リンクは 2026-09-02 の蒸留時に archive パスへ修正) を本フェーズの消費者検証と併せて実施するか判断する — API 面の棚卸しと消費者視点の検証は同じ問いの別角度で、逆順は検証やり直しを招く
- [ ] **phase-5 からの申し送り** (2026-09-01): docs-refresh の明示依頼 (skills/ とルート README の module 統合追随、互換情報 Kotlin 2.3+ / minSdk 29 / compileSdk 35 の明記) をユーザーへ依頼する。**phase-6 からの申し送りを併合 (2026-09-02)** — MAUI 分を同じ依頼に含める: 名前空間 `KsSettingsView` / xmlns `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` への追随 (skills maui + aiforms-migration。README の例は change 内で追随済み)、互換情報 (`Microsoft.Maui.Controls` 10.0.70 以上と NU1605 の注意、最低 OS 版 Android 29 / iOS 16.0 とビルド時ガード `KSSV0001`)、`SwitchCell` / `EntryCell` が MAUI 本体の同名型と衝突するため C# では完全修飾または using alias が要る注意書き、API 版付き TFM の SDK 要件 (下の確認結果を待たず「SDK 10.0.300 で検証」の形で先に載せてよい)。phase-6 の TODO は change 完了直後の依頼を求めており、本フェーズの着手を待たずに依頼してよい
- [ ] **phase-6 からの申し送り** (2026-09-02): 消費者検証で API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) の解決要件 (利用者側の SDK 版 / `TargetPlatformVersion`) を確認し、README / skills の互換情報に SDK 要件として載せる (docs-refresh 依頼に含める)
- [ ] **phase-6 からの申し送り** (2026-09-02): MAUI 消費者の `nuget.config` は `<clear/>` + ローカルフィードだけでは `dotnet new maui` テンプレートの依存 14 件 (Xamarin.AndroidX.* / Microsoft.Extensions.*) が workload の library-packs に無く NU1101 で restore できない。ローカルフィード + nuget.org を列挙し、隔離した `RestorePackagesPath` と `.nupkg.metadata` の source で本リポジトリ由来の 3 パッケージだけを検証対象の取得元として確認する形にする (証跡: [consumer-verification.txt](../../../../changes/archive/2026-09-02-add-maui-nuget-distribution/evidence/consumer-verification.txt) 0 節・6-1)
- [ ] **phase-6 からの申し送り** (2026-09-02): `dotnet publish` (フル trimming) と実機起動を smoke の範囲に含めるかを論点「検証範囲」で決める。phase-6 の消費者検証は Release ビルド (Android / iOS simulator) までで、trimming 後も facade / binding のアセンブリが残ることは確認済み。同梱 README の最小例 (XAML + `MauiProgram`) を無編集で写した消費者ビルドも証跡があり、`verification/` の MAUI 消費者はこの例をそのまま使える

# phase-1-android-build-toolchain

Gradle 9 / AGP / Kotlin を JDK 25 で動くツールチェーンへ更新し、`android/gradle/libs.versions.toml` (バージョンカタログ) を導入する。既存 change `upgrade-android-build-toolchain` (proposal / tasks / spec 改訂済み) をロードマップへ組み込んだフェーズ。

## 論点

- 目標バージョンの確定: Gradle 9.x のマイナー、AGP (8.13 系に留めるか 9.x か)、Kotlin / Compose Compiler の追随 (change の tasks 1.1 で実装着手時に確定)

## 決定事項

- 起案前 (ksn-explore 2026-08-21) に確定済み。詳細は change 側の exploration.md を参照: catalog 導入を本 change に含める、project version の値は据え置きで宣言箇所のみ集約、maven-publish / group 変更は phase-5 の責務

## TODO

- [x] ksn-orchestrator で `upgrade-android-build-toolchain` を実装する (2026-08-21)
- [x] ksn-distill で蒸留・アーカイブする (2026-08-21)

## 実装結果 (2026-08-21 反映)

- 確定版: Gradle 9.5.0 / AGP 8.13.2 / Kotlin 2.4.10 (Compose BOM 2024.10.01 と project version 0.1.0-SNAPSHOT は据え置き)。論点「目標バージョンの確定」はこれで解消 (AGP 9.x は採らず 8.13 系。Gradle 9.7.x は KGP 2.4 のテスト済み上限 9.5.0 を超えるため見送り)
- `android/gradle/libs.versions.toml` を新設し 4 module + samples/app が参照。samples は `versionCatalogs` で共有。契約は concepts `android/architecture/build-toolchain.md` に蒸留
- deviation 4 件 (archive の deviation.md): MAUI binding csproj の aar 再生成入力に catalog / wrapper / gradle.properties を追加 (Non-Goal だった csproj 変更をオーナー指示で実施) / spec の「4 module の release aar」は 3 module の誤記 / `android/gradle.properties` にも tooling.parallel / wrapper の `distributionSha256Sum` 固定
- 検証: JDK 25 (Studio JBR) / 21 で 1261 tests × 2 variant 失敗 0、samples 実機 (Pixel 6a)、MAUI `dotnet build`、Android Studio (JBR 25) sync 成功。Studio の Gradle JDK を 21 へ手動固定する暫定運用は不要になった

### 申し送り (受け皿)

- README / `android/README.md` / `docs/overview.md` の Gradle・AGP 版の記述が旧値のまま → [phase-9-docs](../phase-9-docs/agenda.md) の TODO に追記 (docs-refresh 依頼時に含める)
- 検証 CI のランナー要件: Gradle JVM は JDK 17〜25 のいずれでもよいが、`jvmToolchain(17)` のため JDK 17 のローカル存在 (または toolchain resolver plugin の追加) が要る。AGP 8.13 系は Gradle 10 非対応 API を内部使用 → [phase-3-verification-ci](../phase-3-verification-ci/agenda.md) の論点に追記
- Kotlin 2.4.10 化で生成物の `kotlin-stdlib` 下限が上がる (利用側の Kotlin 要件) → [phase-5-android-packaging](../phase-5-android-packaging/agenda.md) の論点に追記 (POM / README の互換情報)
- Kotlin 2.4 が `@SettingsRootDsl` の top-level 関数付与を無効と警告 (KT-81567、compose モジュール 29 箇所) → 配信とは無関係の独立変更として簡易起票: [changes/harden-compose-settingsroot-dsl](../../../../changes/harden-compose-settingsroot-dsl/exploration.md) (2026-08-21 に add-settingsroot-dsl-visibility-args と統合)


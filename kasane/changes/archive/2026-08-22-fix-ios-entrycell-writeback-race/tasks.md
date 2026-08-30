# Tasks: fix-ios-entrycell-writeback-race

実装は ios/ADR-0004 (proposed) の Decision に従う。実行時挙動の検証規約により、unit test の緑だけでは完了と
しない — グループ 3・4 の Simulator 証跡まで含めて完了。

## 1. 修正前の再現確立 (Simulator `<ios-simulator-udid>` / iPhone 17 / iOS 26.5) — 実装ゲート

**ゲート**: 校正済みの注入手段 (1.2) で修正前ビルドの FAIL を少なくとも 1 件確立できるまで、グループ 2 以降へ
進まない (実行時挙動の検証規約「修正前に実環境で症状を再現する」)。Simulator → WDA `frequency` → pixie4 の
順に試しても再現しない場合は、実装に進まず探索へ戻す (原因仮説の再検討)。A/B 証跡が成立しない限り
ios/ADR-0004 を accepted へ昇格しない。

- [x] 1.0 `samples/ios/KsSettingsViewSample/StoreDemoView.swift` に Store 直接経路の EntryCell を 1 つ追加する:
      固定 id (`static let` の UUID)・非空で一意な初期値 (例: `store.entry@example.com`)・`onTextChanged` で
      `store.replaceCell(cellID:new:)` に同 id・新 text の EntryCell を渡す。「項目削除」が末尾を消すため、
      EntryCell は先頭に置く (→ Scenario: 高速連続入力の完全性 の Store 経路対象)
- [x] 1.1 iOS サンプル (`samples/ios`、bundle `jp.kamusoft.kssettingsview.samples.ios`) を `-derivedDataPath`
      固定でビルドし、`simctl install` / `simctl launch` で Simulator `<ios-simulator-udid>` に導入する
- [x] 1.2 mobilecli `io text` が 1 文字ずつ `editingChanged` を発火させる経路であることをログ (一時的な
      print 等、コミットしない) で 1 回確認する。一括代入の挙動なら WDA `/wda/keys` (`frequency` 指定) へ
      切り替える
- [x] 1.3 Android の `kasane/changes/archive/2026-08-11-fix-entrycell-writeback-caret-race/repro-burst-loop.sh` を
      mobilecli + `dump ui` 向けに移植し、change 配下に `repro-burst-loop-ios.sh` として置く
      (フォーカス確立 = 入力欄右端をタップ → タップなしで `io text abcde` → `dump ui` の `value` を
      PREFIX で引いて `AFTER == BEFORE + 注入文字列` かつ `rect` 不変で判定 / PASS・FAIL・SKIP /
      MIN_VALID=15 / FAIL>0 または有効試行不足で非ゼロ終了 / 結果ログ保存)。PREFIX 一致件数が 1 件で
      ないとき (0 件・複数件) は SKIP ではなく前提失敗として非ゼロ終了する。対象は入力デモ画面のメール欄
      (`tanaka.taro@example.com`、TwoWay = SwiftUI DSL 経路) と Store デモ画面の EntryCell (1.0、Store 直接経路)。
      画面遷移 (`io tap`) も含めて画面ごとに手順を分ける
- [ ] 1.4 修正前ビルドでメール欄で有効 15 試行以上を実行し、欠落・並び替えの再現率を記録する (A、**ゲート
      対象**)。Simulator で再現しない場合は、注入速度の問題か (→ WDA `frequency` で再試行)、それでも再現
      しなければオーナー許可済みのフォールバック pixie4 (WDA 直結) へ移る。いずれでも FAIL が得られなければ
      ゲート不成立として探索へ戻す
- [ ] 1.5 修正前ビルドで Store デモの EntryCell でも有効 15 試行以上を実行し、再現率を記録する (A)。これは
      「Store 直接経路の窓の有無」の実測であり、FAIL 0 でもゲート不成立とはしない (同期経路では同値ガードで
      止まる可能性があるため)。結果は evidence.md に経路別に記録する

## 2. ガードの実装 (ios/Sources/KsSettingsViewUI/EntryCellView.swift)

- [ ] 2.1 `render`: 同一 Cell (同じ `cell.id`) の再 render で `textField.isFirstResponder` の間は `text`
      を代入しない。同一性判定は保持した `cell.id` で行い、equals / 参照比較を使わない。最後に render した
      `cell.text` を保持する (→ Requirement: フォーカス中の EntryCell 入力欄は値の SSoT)
- [ ] 2.2 `textFieldDidEndEditing`: 最後に render した `cell.text` と入力欄が食い違っていれば再同期する。
      再同期は `isProgrammaticUpdate` で `onTextChanged` を発火させない
      (→ Requirement: フォーカス喪失時の text 再同期と収束)
- [ ] 2.3 別 Cell (異なる `cell.id`) の render・非フォーカス時の内容更新では従来どおり text を反映する。
      `prepareForReuse` で同一性判定・再同期の保持状態を破棄する
      (→ Requirement: 非フォーカス時と別 Cell 再 render の text 反映は維持)
- [ ] 2.4 プロパティ反映の優先順位を維持する: 表示系は即時 / 入力系は従来どおり / `isEnabled = false` は
      編集終了としてフォーカス喪失規則に従う。first responder 中の `isEnabled = false` で UIKit が
      自動で first responder を手放さない場合は明示的に `resignFirstResponder()` する
      (→ Requirement: プロパティ反映の優先順位)
- [ ] 2.5 `isSecureTextEntry` の退避・復元 (`secureSavedText`) とガードの干渉がないことを確認する
      (フォーカス中は text 代入が走らないため復元経路は従来どおり)

## 3. unit test (ios/Tests/KsSettingsViewUITests/InputCellsTests.swift)

`UIWindow.makeKeyAndVisible()` で first responder を成立させる既存パターン (`test_EntryCellView_tapHandler…`)
を使う。

- [ ] 3.1 フォーカス中の同一 id・異なる text の再 render で text / キャレットが変わらないテスト (stale
      render を明示的に挟む決定論的テスト。→ Scenario: フォーカス中のプログラム的更新は入力欄を上書きしない
      / 高速連続入力の完全性)
- [ ] 3.2 `resignFirstResponder()` で最新 `cell.text` へ再同期し、`onTextChanged` が発火しないテスト
      (→ Scenario: 保留されたプログラム的更新の反映)
- [ ] 3.3 喪失直前入力の保全テスト: 古い render 値のまま喪失 → 静穏化後に直前入力が表示・通知経路の双方で
      保たれる (→ Scenario: フォーカス喪失直前の入力の保全)
- [ ] 3.4 同一性判定の判別テスト 3 種: 同一 id・異 text は上書きしない / 異なる id・同 text の Cell B を render
      した後、入力が B の callback へ届き、続く「B と同 id・異 text」の再 render でガードが働く (保持 id と
      handler の両方を観測) / `prepareForReuse` 後の再利用で前 Cell の状態を持ち越さない
      (→ Scenario: 別 Cell の render / 別 Cell の render 後は新しい Cell が同一性の基準になる /
      prepareForReuse 後の再利用)
- [ ] 3.5 非フォーカス時の内容更新反映テスト (→ Scenario: 非フォーカスの入力欄への内容更新)
- [ ] 3.6 フォーカス中の placeholder 変更が反映され text が保たれるテスト・`isEnabled = false` で first
      responder でなくなるテスト (→ Scenario: フォーカス中のプロパティ変更 / フォーカス中の無効化)
- [ ] 3.7 既存テストの回帰確認 (InputCellsTests / ContentUpdateBatchTests / ReplaceCellTypeChangeTests ほか
      `ios/Tests` 全緑、実行件数を確認)

## 4. Simulator 検証 (修正後、同一手順の A/B)

合格条件: 各対象で**有効試行 15 回以上・FAIL 0**。結果ログとスクリーンショットは change 配下に証跡として
保存し、evidence.md から参照する。

- [ ] 4.1 `repro-burst-loop-ios.sh` によるメール欄 (TwoWay 経路) のバースト入力試験: 有効 15 試行で欠落・
      並び替え 0 (→ Scenario: 高速連続入力の完全性)
- [ ] 4.2 同スクリプトによる Store デモの EntryCell (Store 直接経路) のバースト入力試験: 有効 15 試行で
      欠落・並び替え 0 (→ Scenario: 高速連続入力の完全性)
- [ ] 4.3 連続バースト後の入力継続確認 (同一手順でバースト後も追加注入が受け付けられること。
      → Scenario: バースト入力後の入力継続)
- [ ] 4.4 日本語 IME の変換中に書き戻しエコーが届いても未確定文字列が維持されることの確認 (Simulator の
      日本語キーボードを `mobilecli io tap` で操作し `simctl io screenshot` で記録)。markedText 中に同一 Cell の
      `render` が実際に到着したことを一時ログ / カウンタ (コミットしない) で記録し、同じ試行の証跡として
      スクリーンショットと併せて evidence.md に残す — 再 render が起きていない空振りを成功と誤認しない
      (→ Scenario: 日本語 IME 変換中の内容更新エコー)
- [ ] 4.5 通常操作の回帰: 低速入力・日本語 IME の変換確定・BackSpace 連続削除・フォーカス移動・Done
      ツールバー・パスワード欄の入力 (既存の secure 退避復元が壊れていないこと)
- [ ] 4.6 evidence.md を作成する: 環境 (Simulator UDID / iOS 版 / Xcode 版 / キーボード)、1.4 の A と 4.1〜4.2
      の B の対比、4.3〜4.5 の記録、フォールバック (WDA / pixie4) を使った場合はその経緯

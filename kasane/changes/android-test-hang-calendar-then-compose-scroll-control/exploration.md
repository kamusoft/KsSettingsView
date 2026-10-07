# Exploration: android-test-hang-calendar-then-compose-scroll-control

## 課題 / 動機

Android のユニットテストで、`DateCalendarRecreationTest` と `ComposeScrollControlTest` の 2 クラスだけを絞って実行すると、テストが固まって終わらない。

change `android-compose-scroll-restore-with-multiple-hosts` の実装中 (2026-10-07) に、全件実行の間欠的な失敗を追って見つけた。その変更の前のコード (HEAD の写し) でも 2 クラスの絞り込みで同じく固まるため、その change には含めず別に積む。

- 再現: `android/` で `./gradlew :kssettingsview:testDebugUnitTest --tests '*DateCalendarRecreationTest' --tests '*ComposeScrollControlTest'`
- 全件実行 (`./gradlew test --rerun-tasks --continue`) でも、5 回中 2 回、`ComposeScrollControlTest` の「状態の変更と同じ処理から出した命令は新しいツリーで解決される」が 5 秒の待機切れで落ちた (debug)。エミュレータを動かしていない回でも落ちたため、負荷が原因という見立ては外れている
- 変更の前のコードの写しでは、全件実行は 2 回とも通った。上の change で頻度が上がったかどうかは、この回数では判断できない
- 手がかり (原因は未特定): 固まっている間、メインスレッドがメッセージの投入を回し続けていた。`kasane/handbook/cross/test-execution.md` の「カレンダーの選択面を提示した後に `idle()` を呼ばない」(Compose の `PopupLayout.pollForLocationOnScreenChange` が main looper を占有する) と同じ系統が、クラスをまたいで残っている見立て
- 対象: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt`、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollControlTest.kt`

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

未探索 (簡易起票)

- 固まる原因の特定。カレンダーの選択面のテストが残した main looper の仕事 (位置のポーリング等) が、後から走るクラスへ持ち越されているのか
- 絞り込みでの固まりと、全件実行での間欠的な待機切れが同じ原因か
- 上の change (`DateCalendarRecreationTest` にカレンダーを提示するテストを 1 本足した) で、全件実行の失敗の頻度が上がったか
- 直す場所。テストの後片付け (選択面を閉じる・looper を空にする) か、待機の書き方か、テストクラスの分離の設定か

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / M / L (理由)

未判定

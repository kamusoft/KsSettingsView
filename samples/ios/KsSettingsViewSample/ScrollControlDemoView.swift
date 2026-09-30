// ScrollControlDemoView.swift
// KsSettingsViewSample
//
// スクロール命令ハンドル (`KsScrollController`) の使い方を示すデモ画面。
// 一覧の中の ButtonCell から Cell・Section・先頭・末尾への命令を出し、
// 項目を足した直後の末尾への命令が追加した項目まで届くことを確かめる。
//
// 対応する Android 側定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/ScrollControlDemoScreen.kt
// （文言・Section 構成・デモデータは Android / MAUI と一字一句揃える。cross/ADR-0016）

import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// スクロール制御のデモ画面。
///
/// 命令ハンドルは画面の View が `@State` で持ち、`.scrollController(_:)` で一覧へ接続する。
/// 命令で指す Section / Cell には `.sectionID(_:)` / `.cellID(_:)` で明示 ID を付ける。
/// 開いたときに自動でスクロールはしない（操作の Section が画面外に出ないようにするため）。
///
/// Cell の通知 (`onTap`) は `@Sendable` の閉包で受け取るが、呼ばれるのは常にメインスレッドなので、
/// `MainActor.assumeIsolated` の中でメインアクターに隔離されたハンドルへ命令を出す。
struct ScrollControlDemoView: View {

    /// 番号付き Section の数。
    private static let numberedSectionCount = 5
    /// 番号付き Section 1 つあたりの Cell の数。
    private static let cellsPerSection = 5
    /// 「Section へ」の対象の Section の明示 ID。
    private static let targetSectionID = "section-4"
    /// 「Cell を中央へ」の対象の Cell の明示 ID。
    private static let targetCellID = "cell-3-3"

    /// 一覧へ接続するスクロール命令ハンドル。
    @State private var scroll = KsScrollController()
    /// 「追加して末尾へ」で足した項目の番号。最後の番号付き Section の末尾に並ぶ。
    @State private var addedNumbers: [Int] = []

    var body: some View {
        KsSettingsView {
            Section(
                "操作",
                footer: "「Section へ」は Section 4 の見出しを上端へ、「Cell を中央へ」は項目 3-3 を表示範囲の中央へ移します。「追加して末尾へ」は Section 5 に項目を足して末尾へ移ります。"
            ) {
                ButtonCell(title: "末尾へ", onTap: { MainActor.assumeIsolated { scroll.scrollToEnd() } })
                ButtonCell(
                    title: "Section へ",
                    onTap: {
                        MainActor.assumeIsolated {
                            scroll.scrollToSection(id: Self.targetSectionID, position: .start)
                        }
                    }
                )
                ButtonCell(
                    title: "Cell を中央へ",
                    onTap: {
                        MainActor.assumeIsolated {
                            scroll.scrollTo(id: Self.targetCellID, position: .center)
                        }
                    }
                )
                ButtonCell(title: "追加して末尾へ", onTap: { MainActor.assumeIsolated { addAndScrollToEnd() } })
            }

            for number in 1...Self.numberedSectionCount {
                Section("Section \(number)", footer: "Section \(number) の Footer") {
                    for index in 1...Self.cellsPerSection {
                        LabelCell(title: "項目 \(number)-\(index)")
                            .cellID("cell-\(number)-\(index)")
                    }
                    if number == Self.numberedSectionCount {
                        // 足した項目は番号を ForEach の key にして、表示の同一性を保つ。
                        ForEach(addedNumbers, id: \.self) { added in
                            LabelCell(title: "追加した項目 \(added)")
                        }
                    }
                }
                .sectionID("section-\(number)")
            }

            Section("先頭へ戻る", footer: "「先頭へ」はアニメーションなしで移ります。") {
                ButtonCell(
                    title: "先頭へ",
                    onTap: { MainActor.assumeIsolated { scroll.scrollToStart(animated: false) } }
                )
            }
        }
        .rootFooter("ここが内容の末尾です（Root Footer）")
        .scrollController(scroll)
        .navigationTitle(SampleScreen.scrollControl.title)
    }

    /// 最後の番号付き Section に項目を 1 つ足し、同じ処理の中で末尾への命令を出す。
    ///
    /// 命令は足した項目が表示に反映された後に実行されるため、足した項目を含む末尾へ届く。
    private func addAndScrollToEnd() {
        addedNumbers.append(addedNumbers.count + 1)
        scroll.scrollToEnd()
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        ScrollControlDemoView()
    }
}
#endif

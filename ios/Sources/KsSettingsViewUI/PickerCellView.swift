// PickerCellView.swift
// KsSettingsViewUI
//
// `PickerCell` の Renderer 実装。共通行レイアウト関数経由で描画し、選択値テキストを行内 trailing
// （`valueLabelText`）に、chevron（`makeChevronView()`）を Cell 級アクセサリ（`accessoryView`）に
// 配置する。タップで `PickerListViewController` を `UINavigationController` 経由でモーダル提示する。
// `UIListContentConfiguration` / `UICellAccessory` 経路は使わない。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `PickerCell` 描画用 Cell View。
@MainActor
internal final class PickerCellView: KsListCellBase, @MainActor KsCellRenderer {
    /// タップ時に呼ばれるハンドラ（`KsSettingsViewController` の didSelectItemAt 経由で呼ばれる）。
    internal var tapHandler: (@Sendable () -> Void)?
    /// 直近 bind 時の Cell 値。タップ時のモーダル生成に必要。
    private var lastCell: PickerCell?
    /// 直近 bind 時の Theme。
    private var lastTheme: Theme = Theme()

    func render(cell: any KsCell, theme: Theme) {
        guard let picker = cell as? PickerCell else {
            assertionFailure("PickerCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        self.lastCell = picker
        self.lastTheme = theme

        let effective = EffectiveStyle(theme: theme, cellStyle: picker.style)
        let valueText = picker.effectiveValueText()

        applyCellBaseLayout(
            self,
            title: picker.title,
            description: picker.description,
            icon: picker.icon,
            hintText: picker.hintText,
            effective: effective,
            theme: theme,
            isEnabled: picker.isEnabled,
            valueLabelText: valueText,
            accessoryView: makeChevronView()
        )

        // タップ起動は `KsSettingsViewController` の didSelectItemAt 経由で行う
        // （`TapNotifyingRenderer.tapHandler` プロトコル準拠）。
        if picker.isEnabled {
            let handler: @Sendable () -> Void = { [weak self] in
                MainActor.assumeIsolated {
                    self?.presentPickerModal()
                }
            }
            self.tapHandler = handler
        } else {
            self.tapHandler = nil
        }
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.tapHandler = nil
        self.lastCell = nil
    }

    // MARK: - モーダル提示

    /// 選択面 VC を直近 bind した Cell / Theme から組み立てる。
    ///
    /// スタイル解決値は VC 側で `Theme` と `CellStyle` から合成するため、ここでは解決前の
    /// 素材（Theme / CellStyle / Cell 固有 accent）をそのまま渡す。
    /// 提示経路とテストの検証 seam が同一の組み立てを共有するための単一の入口。
    private func makeListViewController() -> PickerListViewController? {
        guard let picker = lastCell else { return nil }
        return PickerListViewController(
            items: picker.items,
            selectionMode: picker.selectionMode,
            selectedIndex: picker.selectedIndex,
            selectedIndices: picker.selectedIndices,
            maxSelectedNumber: picker.maxSelectedNumber,
            // 選択面のタイトルは `pageTitle` 未指定時に Cell の `title` へフォールバックする。
            navigationTitle: picker.pageTitle ?? picker.title,
            theme: lastTheme,
            cellStyle: picker.style,
            cellAccentColor: picker.accentColor,
            onSingleDone: { newIndex in
                picker.onSelectionChanged?(newIndex)
            },
            onMultiDone: { newSet in
                picker.onMultiSelectionChanged?(newSet)
            }
        )
    }

    /// 実際に提示する VC（選択面を載せた navigation controller）を組み立てる。
    /// 提示元の外観の引き継ぎもここで済ませ、提示経路とテストの検証 seam が同じ結果を共有する。
    private func makePresentedViewController() -> UINavigationController? {
        guard let listVC = makeListViewController() else { return nil }
        let nav = UINavigationController(rootViewController: listVC)
        PresentationAppearance.inherit(from: self, to: nav)
        return nav
    }

    private func presentPickerModal() {
        guard let nav = makePresentedViewController() else { return }
        guard let presenter = KeyWindowResolver.topPresentedViewController() else { return }

        presenter.present(nav, animated: true, completion: nil)
    }

    // MARK: - テスト用

    /// テストから tapHandler クロージャを発火させる（モーダル提示の擬似呼び出し）。
    internal func _simulateTap() {
        tapHandler?()
    }

    /// テストから直接モーダル提示処理を実行するアクセサ（keyWindow から提示する）。
    internal func _presentPickerModalForTesting() {
        presentPickerModal()
    }

    /// テスト用: 提示経路と同一の組み立てで選択面 VC を生成する（配線の検証 seam）。
    /// VC を直接生成するテストでは `render` からの配線漏れを検出できないため、この経路を用いる。
    internal func _makeListViewControllerForTesting() -> PickerListViewController? {
        return makeListViewController()
    }

    /// テスト用: 提示経路と同一の組み立てで、実際に提示する VC を生成する（配線の検証 seam）。
    internal func _makePresentedViewControllerForTesting() -> UINavigationController? {
        return makePresentedViewController()
    }

    /// テスト用: bind 中の cell を取得する。
    internal var _lastCell: PickerCell? { lastCell }
}

#endif

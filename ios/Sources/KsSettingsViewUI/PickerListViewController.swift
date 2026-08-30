// PickerListViewController.swift
// KsSettingsViewUI
//
// `PickerCell` のモーダル選択画面（内部用）。`UITableViewController` ベースで、
// `selectionMode` に応じて単一選択 / 複数選択を切替える。
//
// 選択面は呼び出し元 Cell / Theme の実効スタイル（文字色・フォント・背景・区切り線・
// タップハイライト・強調色）を継承し、選択中の項目を中央付近に表示した状態で開く。
// タイトルは呼び出し元で `pageTitle ?? title` を解決した値を受け取る。

#if canImport(UIKit)
import UIKit

/// `PickerCell` モーダル選択画面（内部用）。`UINavigationController` 経由で modal 提示される。
///
/// 単一選択モード（`.single`）: 行タップで選択即時 dismiss。
/// 複数選択モード（`.multiple`）: navigation bar の「完了」ボタンで dismiss。
@MainActor
internal final class PickerListViewController: UITableViewController {

    private let items: [PickerItem]
    private let selectionMode: PickerSelectionMode
    private let maxSelectedNumber: Int

    /// 呼び出し元 Cell の `Theme` / `CellStyle` から解決した実効スタイル。
    /// 候補行の文字色・フォント、行と面の背景、区切り線、タップハイライトの供給源。
    private let effective: EffectiveStyle
    /// 選択印・ナビゲーションバーのボタンへ適用する解決済み強調色。
    /// 3 段解決（`PickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`）の結果値で、
    /// ナビゲーションバーは `Theme` を別途参照せずこの値を共有する。
    private let resolvedAccentColor: UIColor

    /// 単一選択モードの初期 index。
    private var currentSingle: Int?
    /// 複数選択モードの選択集合（編集中状態を保持）。
    private var currentMulti: Set<Int>

    /// 初期スクロールを実施済みか（レイアウト確定後に 1 度だけ行うためのフラグ）。
    private var hasPerformedInitialScroll = false

    /// 単一選択モード時の確定 callback（モーダル dismiss 直前に発火）。
    private let onSingleDone: ((Int) -> Void)?
    /// 複数選択モード時の確定 callback（「完了」押下時に発火）。
    private let onMultiDone: ((Set<Int>) -> Void)?

    private static let cellReuseIdentifier = "PickerListCell"

    /// - Parameters:
    ///   - navigationTitle: ナビゲーションバーへ表示する解決済みタイトル
    ///     （呼び出し元で `pageTitle ?? title` を解決して渡す）。
    ///   - theme: 呼び出し元 Cell が描画に使っている Theme。
    ///   - cellStyle: 呼び出し元 Cell の CellStyle（Theme より優先される）。
    ///   - cellAccentColor: `PickerCell.accentColor`（Cell 固有指定。3 段解決の最優先段）。
    init(
        items: [PickerItem],
        selectionMode: PickerSelectionMode,
        selectedIndex: Int?,
        selectedIndices: Set<Int>,
        maxSelectedNumber: Int,
        navigationTitle: String?,
        theme: Theme,
        cellStyle: CellStyle,
        cellAccentColor: UIColor?,
        onSingleDone: ((Int) -> Void)?,
        onMultiDone: ((Set<Int>) -> Void)?
    ) {
        self.items = items
        self.selectionMode = selectionMode
        self.currentSingle = selectedIndex
        self.currentMulti = selectedIndices
        self.maxSelectedNumber = maxSelectedNumber
        let effective = EffectiveStyle(theme: theme, cellStyle: cellStyle)
        self.effective = effective
        self.resolvedAccentColor = cellAccentColor ?? effective.accentColor
        self.onSingleDone = onSingleDone
        self.onMultiDone = onMultiDone
        super.init(style: .plain)
        self.title = navigationTitle
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.register(PickerListItemCell.self, forCellReuseIdentifier: Self.cellReuseIdentifier)
        // 選択印（checkmark アクセサリ）は tintColor で着色される。
        tableView.tintColor = resolvedAccentColor
        // 面の背景・区切り線は呼び出し元 Cell の実効値を継承する。
        tableView.backgroundColor = effective.cellBackgroundColor
        tableView.separatorColor = effective.separatorColor

        switch selectionMode {
        case .single:
            tableView.allowsMultipleSelection = false
            // 単一選択は Cancel のみ navigation bar に置く（選択時即時 dismiss）
            navigationItem.leftBarButtonItem = makeBarButtonItem(
                systemItem: .cancel,
                action: #selector(handleCancel)
            )
        case .multiple:
            tableView.allowsMultipleSelection = true
            navigationItem.leftBarButtonItem = makeBarButtonItem(
                systemItem: .cancel,
                action: #selector(handleCancel)
            )
            // `.done` システムアイテムを利用することで、OS の自動ローカライズ
            //（Done / 完了 / 完了 / 완료 / 完成 など）に追従する。
            navigationItem.rightBarButtonItem = makeBarButtonItem(
                systemItem: .done,
                action: #selector(handleDone)
            )
        }
        applyNavigationBarTitleAppearance()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        // レイアウト確定後（行の実寸が決まった後）に 1 度だけ初期スクロールする。
        guard !hasPerformedInitialScroll else { return }
        hasPerformedInitialScroll = true
        scrollToInitialSelection()
    }

    // MARK: - スタイル適用

    /// ナビゲーションバー用のボタンを生成する。
    /// ボタン色は選択印と同一の解決済み強調色を使う（フォントサイズはシステム既定のまま）。
    private func makeBarButtonItem(
        systemItem: UIBarButtonItem.SystemItem,
        action: Selector
    ) -> UIBarButtonItem {
        let item = UIBarButtonItem(barButtonSystemItem: systemItem, target: self, action: action)
        item.tintColor = resolvedAccentColor
        return item
    }

    /// ナビゲーションバーのタイトル文字色へ実効タイトル色を適用する。
    ///
    /// 呼び出し時点で navigationItem / バーに設定済みの appearance を複製してタイトル文字色だけを
    /// 差し替えるため、背景・フォントサイズの構成は基点の appearance から引き継がれる。
    private func applyNavigationBarTitleAppearance() {
        let bar = navigationController?.navigationBar

        // 標準状態: 表示中のバーの構成を基点にする（どこにも設定が無ければシステム既定の構成）。
        let standardBase = navigationItem.standardAppearance ?? bar?.standardAppearance ?? {
            let base = UINavigationBarAppearance()
            base.configureWithDefaultBackground()
            return base
        }()
        let standard = applyingTitleColor(to: standardBase)
        navigationItem.standardAppearance = standard

        // compact 状態: 未設定なら標準状態の構成が使われるため、同じ結果を明示的に置く。
        if let compactBase = navigationItem.compactAppearance ?? bar?.compactAppearance {
            navigationItem.compactAppearance = applyingTitleColor(to: compactBase)
        } else {
            navigationItem.compactAppearance = standard
        }

        // スクロール上端: 未設定時のバーは背景が透過し、下地（実効セル背景色を適用した
        // 候補リスト）が透ける。この見え方を保つため透過構成を基点にする。
        if let scrollEdgeBase = navigationItem.scrollEdgeAppearance ?? bar?.scrollEdgeAppearance {
            navigationItem.scrollEdgeAppearance = applyingTitleColor(to: scrollEdgeBase)
        } else {
            let base = UINavigationBarAppearance(barAppearance: standardBase)
            base.configureWithTransparentBackground()
            navigationItem.scrollEdgeAppearance = applyingTitleColor(to: base)
        }
    }

    /// 渡された appearance を複製し、タイトル文字色だけを実効タイトル色へ差し替えて返す。
    private func applyingTitleColor(to base: UINavigationBarAppearance) -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance(barAppearance: base)
        appearance.titleTextAttributes[.foregroundColor] = effective.titleColor
        appearance.largeTitleTextAttributes[.foregroundColor] = effective.titleColor
        return appearance
    }

    // MARK: - 初期スクロール

    /// 初期スクロールの対象行。
    ///
    /// 単一選択は `selectedIndex`、複数選択は選択中の**最小 index**。
    /// 有効（範囲内）index の抽出はこの計算にのみ用い、選択集合そのものは正規化しない
    /// （範囲外 index は作業状態・確定 callback に保持される）。
    /// 選択なし・範囲外のみ・`items` が空の場合は `nil`（＝先頭表示のまま）。
    private var initialScrollTargetRow: Int? {
        switch selectionMode {
        case .single:
            guard let idx = currentSingle, items.indices.contains(idx) else { return nil }
            return idx
        case .multiple:
            return currentMulti.filter { items.indices.contains($0) }.min()
        }
    }

    /// 選択中の項目が可視領域の中央付近に来るようスクロールする。
    /// リスト端部付近ではスクロール可能範囲によるクランプで端寄せになる（UIKit の既定挙動）。
    private func scrollToInitialSelection() {
        guard let row = initialScrollTargetRow else { return }
        tableView.scrollToRow(at: IndexPath(row: row, section: 0), at: .middle, animated: false)
    }

    // MARK: - UITableViewDataSource

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: Self.cellReuseIdentifier, for: indexPath)
        let item = items[indexPath.row]
        let displayText = item.text
        cell.textLabel?.text = displayText
        // 副表示を持つ候補だけが 2 行構成になる（`subText` が nil の行は主表示のみ）。
        cell.detailTextLabel?.text = item.subText

        // 候補行のスタイル継承（文字色・フォント・背景・タップハイライト）。
        // 選択印の色は tableView へ設定した tintColor が行へ継承される。
        cell.textLabel?.textColor = effective.titleColor
        cell.textLabel?.font = effective.titleFont
        // 副表示は description 系統の実効値を継承する。
        cell.detailTextLabel?.textColor = effective.descriptionColor
        cell.detailTextLabel?.font = effective.descriptionFont
        cell.backgroundColor = effective.cellBackgroundColor
        // `selectedBackgroundView` は未設定でも UIKit 既定のハイライトビューを返すため、
        // 実効ハイライト色を確実に効かせるには専用ビューを差し替える必要がある。
        let highlightView = UIView()
        highlightView.backgroundColor = effective.selectedBackgroundColor
        cell.selectedBackgroundView = highlightView

        // 表示名はアクセシビリティラベルとして明示的に公開する（選択状態と 1 ノードに揃える）。
        // 副表示を持つ行は主表示に続けて読み上げられるよう連結する。
        if let subText = item.subText {
            cell.accessibilityLabel = "\(displayText), \(subText)"
        } else {
            cell.accessibilityLabel = displayText
        }

        let isChecked: Bool
        switch selectionMode {
        case .single:
            isChecked = (indexPath.row == currentSingle)
        case .multiple:
            isChecked = currentMulti.contains(indexPath.row)
        }
        applyCheckState(to: cell, isChecked: isChecked)
        return cell
    }

    /// 候補行のチェック表示とアクセシビリティ選択状態をまとめて反映する。
    private func applyCheckState(to cell: UITableViewCell, isChecked: Bool) {
        cell.accessoryType = isChecked ? .checkmark : .none
        if isChecked {
            cell.accessibilityTraits.insert(.selected)
        } else {
            cell.accessibilityTraits.remove(.selected)
        }
    }

    // MARK: - UITableViewDelegate

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        switch selectionMode {
        case .single:
            currentSingle = indexPath.row
            tableView.reloadData()
            onSingleDone?(indexPath.row)
            dismissModal()

        case .multiple:
            if currentMulti.contains(indexPath.row) {
                // 既にチェック済み → 外す（既選択の解除は常に可能）
                currentMulti.remove(indexPath.row)
                tableView.deselectRow(at: indexPath, animated: false)
                if let cell = tableView.cellForRow(at: indexPath) {
                    applyCheckState(to: cell, isChecked: false)
                }
            } else {
                // 上限到達チェック
                if maxSelectedNumber > 0 && currentMulti.count >= maxSelectedNumber {
                    // 新規チェックを無視 + 触覚フィードバック
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    tableView.deselectRow(at: indexPath, animated: false)
                    return
                }
                currentMulti.insert(indexPath.row)
                if let cell = tableView.cellForRow(at: indexPath) {
                    applyCheckState(to: cell, isChecked: true)
                }
                tableView.deselectRow(at: indexPath, animated: false)
            }
        }
    }

    // MARK: - Navigation actions

    @objc private func handleCancel() {
        dismissModal()
    }

    @objc private func handleDone() {
        if selectionMode == .multiple {
            onMultiDone?(currentMulti)
        }
        dismissModal()
    }

    private func dismissModal() {
        if let nav = navigationController, nav.presentingViewController != nil {
            nav.dismiss(animated: true)
        } else {
            dismiss(animated: true)
        }
    }

    // MARK: - テスト用フック

    internal var _currentSingle: Int? { currentSingle }
    internal var _currentMulti: Set<Int> { currentMulti }
    internal func _simulateSelect(_ row: Int) {
        tableView(tableView, didSelectRowAt: IndexPath(row: row, section: 0))
    }
    internal func _simulateDone() { handleDone() }
    internal func _simulateCancel() { handleCancel() }
    /// 配線検証用: 呼び出し元から受け取った Theme / CellStyle の合成結果。
    internal var _effectiveStyle: EffectiveStyle { effective }
    /// 配線検証用: 3 段解決後の強調色（選択印とナビゲーションバーのボタンが共有する値）。
    internal var _resolvedAccentColor: UIColor { resolvedAccentColor }
    /// 配線検証用: 初期スクロールの対象行（端部クランプに影響されない計算結果そのもの）。
    internal var _initialScrollTargetRow: Int? { initialScrollTargetRow }
}
#endif

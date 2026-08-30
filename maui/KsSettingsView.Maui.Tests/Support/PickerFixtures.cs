using System;
using System.Collections;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace KsSettingsView.Maui.Tests.Support;

/// <summary>PickerCell の object 候補を検証するためのモデル群。</summary>
internal static class PickerFixtures
{
    /// <summary>主表示・副表示の双方を持つ、値等価なモデル。</summary>
    /// <param name="Name">主表示に使う名前</param>
    /// <param name="Note">副表示に使う補足</param>
    public sealed record Plan(string Name, string? Note = null);

    /// <summary>公開プロパティを持たず、ToString() だけで表示されるモデル。</summary>
    /// <param name="label">ToString() が返す文字列</param>
    public sealed class Opaque(string label)
    {
        /// <inheritdoc/>
        public override string ToString() => label;
    }

    /// <summary>射影対象が string 以外・null・非 public instance になっているモデル。</summary>
    public sealed class Awkward
    {
        /// <summary>string 以外の値を返すプロパティ。</summary>
        public int Count { get; init; }

        /// <summary>値が null になり得るプロパティ。</summary>
        public string? Missing { get; init; }

        /// <summary>射影対象にならない非 public プロパティ。</summary>
        private string Hidden => "hidden";

        /// <summary>射影対象にならない静的プロパティ。</summary>
        public static string Shared => "shared";

        /// <inheritdoc/>
        public override string ToString() => $"awkward:{Count}/{Hidden}";
    }

    /// <summary>引数の型だけが違う indexer を 2 つ持つ (= 同名プロパティが複数ある) モデル。</summary>
    public sealed class Indexed
    {
        /// <summary>位置で引く indexer。</summary>
        /// <param name="index">引く位置</param>
        public string this[int index] => $"int:{index}";

        /// <summary>名前で引く indexer。</summary>
        /// <param name="key">引く名前</param>
        public string this[string key] => $"string:{key}";

        /// <inheritdoc/>
        public override string ToString() => "indexed";
    }

    /// <summary>候補と選択を持ち、TwoWay バインドの相手になる ViewModel。</summary>
    public sealed class SelectionViewModel : INotifyPropertyChanged
    {
        /// <summary><see cref="Items"/> のバッキングフィールド。</summary>
        private IList? _items;

        /// <summary><see cref="Selected"/> のバッキングフィールド。</summary>
        private object? _selected;

        /// <summary><see cref="SelectedMany"/> のバッキングフィールド。</summary>
        private IList? _selectedMany;

        /// <inheritdoc/>
        public event PropertyChangedEventHandler? PropertyChanged;

        /// <summary>選択候補。</summary>
        public IList? Items
        {
            get => _items;
            set => Set(ref _items, value);
        }

        /// <summary>単一選択の選択項目。</summary>
        public object? Selected
        {
            get => _selected;
            set => Set(ref _selected, value);
        }

        /// <summary>複数選択の選択項目。</summary>
        public IList? SelectedMany
        {
            get => _selectedMany;
            set => Set(ref _selectedMany, value);
        }

        /// <summary>実体が変わったときだけ格納して変更を通知する。</summary>
        /// <remarks>
        /// 値等価では省かない。値等価な別実体が届いたかどうかを観測できるようにするため。
        /// </remarks>
        /// <typeparam name="T">格納する値の型</typeparam>
        /// <param name="field">格納先</param>
        /// <param name="value">格納する値</param>
        /// <param name="name">通知するプロパティ名</param>
        private void Set<T>(ref T field, T value, [CallerMemberName] string? name = null)
        {
            if (ReferenceEquals(field, value))
            {
                return;
            }

            field = value;
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
        }
    }

    /// <summary>getter が必ず例外を送出するモデル。</summary>
    public sealed class Exploding
    {
        /// <summary>参照すると例外になるプロパティ。</summary>
        public string Boom => throw new InvalidOperationException("getter failed");
    }
}

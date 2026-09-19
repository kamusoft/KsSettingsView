using System;
using System.Collections.Generic;
using System.Collections.Specialized;

namespace KsSettingsView.Tests.Fakes;

/// <summary>
/// 2 つ目の購読を受け付けない Cell コレクション。
/// </summary>
/// <remarks>
/// 対応表への登録が途中で失敗する接続を再現するために使う。この Section より前の Section は
/// 登録を終えており、後ろの Section は未登録のまま残るため、後片付け待ちの実体と置き場所に
/// 残る実体が同時に存在する状態を作れる。
/// 1 つ目の購読は Section が論理上の子を追従するために張るため、受け付ける。
/// </remarks>
internal sealed class UnsubscribableCellCollection : List<CellBase>, INotifyCollectionChanged
{
    private int _subscriptions;

    /// <inheritdoc/>
    public event NotifyCollectionChangedEventHandler? CollectionChanged
    {
        add
        {
            if (_subscriptions++ > 0)
            {
                throw new InvalidOperationException("subscribing to the cells failed");
            }
        }

        remove
        {
        }
    }
}

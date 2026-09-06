# develop の branch protection (2026-09-04、change slim-ci-triggers)

## 変更前

```json
{
  "required_status_checks": {
    "strict": false,
    "checks": [
      "ios / verify",
      "android / verify",
      "maui / verify",
      "lint",
      "consumer-ios / verify",
      "consumer-android / verify",
      "consumer-maui / verify"
    ]
  },
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false,
    "require_last_push_approval": false,
    "required_approving_review_count": 0
  },
  "enforce_admins": false,
  "allow_force_pushes": false,
  "allow_deletions": false
}
```

## 変更後 (gh api -X PUT で完全 payload を送信、読み直した結果)

```json
{
  "required_status_checks": null,
  "required_pull_request_reviews": null,
  "enforce_admins": false,
  "allow_force_pushes": false,
  "allow_deletions": false
}
```

main は変更していない (必須 status check 7 件のまま)。

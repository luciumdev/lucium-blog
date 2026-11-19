# ── GitHub Actions OIDC → 배포 Role (장기 액세스 키 없음) ──────────
data "aws_caller_identity" "current" {}

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "ci_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # 지정 저장소의 지정 브랜치 push 만 허용 (PR·포크·다른 브랜치 차단)
    # 주의: job 에 environment: 를 붙이면 sub 가 repo:o/r:environment:<name> 으로 바뀜
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:ref:refs/heads/${var.github_deploy_branch}"]
    }
  }
}

resource "aws_iam_role" "ci_deploy" {
  name                 = "${var.project}-ci-deploy"
  assume_role_policy   = data.aws_iam_policy_document.ci_assume.json
  max_session_duration = 3600
}

# workflow.yml 이 쓰는 호출만: send-command / wait / get-command-invocation
data "aws_iam_policy_document" "ci_deploy" {
  statement {
    sid     = "SendDeployCommand"
    actions = ["ssm:SendCommand"]
    resources = [
      aws_instance.app.arn,
      "arn:aws:ssm:${var.region}::document/AWS-RunShellScript",
    ]
  }

  # GetCommandInvocation 은 리소스 단위 제한 미지원
  statement {
    sid       = "ReadCommandResult"
    actions   = ["ssm:GetCommandInvocation"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "ci_deploy" {
  name   = "ssm-deploy"
  role   = aws_iam_role.ci_deploy.id
  policy = data.aws_iam_policy_document.ci_deploy.json
}

# ── 비용 알림 ─────────────────────────────────────────────────────
resource "aws_budgets_budget" "monthly" {
  name         = "${var.project}-monthly"
  budget_type  = "COST"
  limit_amount = tostring(var.budget_limit_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.budget_alert_email]
  }
}

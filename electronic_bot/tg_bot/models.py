from django.db import models
import uuid


# Create your models here.

class TgUser(models.Model):
    tg_id = models.BigIntegerField(primary_key=True)
    money = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    is_notify = models.BooleanField(default=True)  # 是否奖励通知
    invite_tg_id = models.BigIntegerField(blank=True, null=True)  # 邀请人
    deposit_reward = models.BigIntegerField(default=0)
    pg_player_id = models.CharField(max_length=50, blank=True, null=True)
    jdb_player_id = models.CharField(max_length=50, blank=True, null=True)
    create_time = models.DateTimeField(auto_now_add=True)
    is_admin = models.BooleanField(default=False)

    class Meta:
        db_table = 'tg_user'


class AmountChange(models.Model):
    user = models.ForeignKey('TgUser', on_delete=models.CASCADE, related_name='amount_changes')
    change_type = models.CharField(max_length=20)
    name = models.CharField(max_length=50)
    change_amount = models.DecimalField(max_digits=10, decimal_places=2)  # 变化金额
    before_amount = models.DecimalField(max_digits=10, decimal_places=2)  # 变化前金额
    after_amount = models.DecimalField(max_digits=10, decimal_places=2)  # 变化后金额
    amount_type = models.CharField(max_length=20, default='CNY')
    create_time = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'amount_change'
        ordering = ['-create_time']


class TgRecharge(models.Model):
    recharge_id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tg_id = models.BigIntegerField()
    money = models.DecimalField(max_digits=10, decimal_places=2)
    amount_type = models.CharField(max_length=20)
    pay_type = models.CharField(max_length=20, default='USDT')
    status = models.IntegerField(default=0)
    create_time = models.DateTimeField(auto_now_add=True)
    update_time = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'tg_recharge'


class WithdrawalRecord(models.Model):
    # 用户关联
    user = models.ForeignKey('TgUser', on_delete=models.CASCADE, verbose_name="用户")

    # 提现金额 (USDT)
    withdraw_amount = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        verbose_name="提现金额 (USDT)"
    )

    # 提现金额对应的 CNY
    withdraw_cny = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        verbose_name="提现金额 (CNY)"
    )

    # 提现前余额 (CNY)
    before_balance = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        verbose_name="提现前余额 (CNY)"
    )

    # 提现后余额 (CNY)
    after_balance = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        verbose_name="提现后余额 (CNY)"
    )

    withdraw_address = models.CharField(
        max_length=255,
        verbose_name="提现地址"
    )

    # 审核状态
    PENDING = 'pending'
    APPROVED = 'approved'
    REJECTED = 'rejected'
    STATUS_CHOICES = [
        (PENDING, "待审核"),
        (APPROVED, "已通过"),
        (REJECTED, "未通过"),
    ]
    status = models.CharField(
        max_length=10,
        choices=STATUS_CHOICES,
        default=PENDING,
        verbose_name="审核状态"
    )

    # 审核员 Telegram ID
    admin_tg_id = models.BigIntegerField(
        null=True,
        blank=True,
        verbose_name="审核员tg_id"
    )

    # 提现申请时间
    created_at = models.DateTimeField(
        auto_now_add=True,
        verbose_name="创建时间"
    )

    # 审核时间
    reviewed_at = models.DateTimeField(
        null=True,
        blank=True,
        verbose_name="审核时间"
    )

    def __str__(self):
        return f"提现记录 {self.id} - 用户 {self.user.tg_id} - 状态 {self.get_status_display()}"

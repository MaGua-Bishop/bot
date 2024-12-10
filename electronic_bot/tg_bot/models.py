from django.db import models
import uuid


# Create your models here.

class TgUser(models.Model):
    tg_id = models.BigIntegerField(primary_key=True)
    money = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    pg_money = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    is_notify = models.BooleanField(default=True)  # 是否奖励通知
    invite_tg_id = models.BigIntegerField(blank=True, null=True)  # 邀请人
    deposit_reward = models.BigIntegerField(default=0)
    create_time = models.DateTimeField(auto_now_add=True)

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

from django.db import models
from django.utils.timezone import now


class TgUser(models.Model):
    tg_id = models.BigIntegerField(primary_key=True)  # 用户唯一标识
    tg_name = models.CharField(max_length=30)  # 用户昵称
    tg_username = models.CharField(max_length=30, null=True, blank=True)  # 用户用户名
    money = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)  # 用户余额
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    is_admin = models.BooleanField(default=False)  # 是否为管理员

    class Meta:
        db_table = 'tg_user'

    def __str__(self):
        return self.tg_name


class TgLottery(models.Model):
    lottery_id = models.CharField(max_length=100, primary_key=True)  # 抽奖唯一标识
    tg_id = models.BigIntegerField()  # 创建抽奖的用户ID
    tg_name = models.CharField(max_length=30)  # 创建抽奖的用户昵称
    chat_id = models.BigIntegerField()  # 创建抽奖的群聊ID
    message_id = models.BigIntegerField(null=True, blank=True)  # 抽奖关联的消息ID
    money = models.DecimalField(max_digits=10, decimal_places=2)  # 抽奖金额
    virtua_money = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)  # 虚拟金额
    number = models.BigIntegerField()  # 抽奖份数
    status = models.IntegerField(default=0)  # 抽奖状态：0进行中，1已结束
    link = models.CharField(max_length=100, null=True, blank=True)  # 订阅链接
    link_type = models.IntegerField(null=True, blank=True)  # 链接类型：1群聊，2频道
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    update_time = models.DateTimeField(null=True, blank=True)  # 更新时间

    class Meta:
        db_table = 'tg_lottery'

    def __str__(self):
        return self.lottery_id


class TgPrizePool(models.Model):
    prize_pool_id = models.CharField(max_length=100, primary_key=True)  # 奖池唯一标识，使用UUID
    lottery_id = models.CharField(max_length=100)  # 关联的抽奖ID
    money = models.DecimalField(max_digits=10, decimal_places=2)  # 奖池金额
    status = models.IntegerField(default=0)  # 奖池状态：0进行中，1已完成
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    update_time = models.DateTimeField(null=True, blank=True)  # 更新时间

    class Meta:
        db_table = 'tg_prize_pool'

    def __str__(self):
        return self.prize_pool_id


class TgLotteryInfo(models.Model):
    lottery_info_id = models.CharField(max_length=100, primary_key=True)  # 中奖信息唯一标识，使用UUID
    lottery_id = models.CharField(max_length=100)  # 关联的抽奖ID
    lottery_create_tg_id = models.BigIntegerField()  # 创建抽奖的用户ID
    prize_pool_id = models.CharField(max_length=100, null=True, blank=True)  # 关联的奖池ID
    tg_id = models.BigIntegerField()  # 中奖用户ID
    money = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)  # 中奖金额
    status = models.IntegerField(default=0)  # 中奖状态：0未兑换，1已兑换
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    update_time = models.DateTimeField(null=True, blank=True)  # 更新时间
    tg_name = models.CharField(max_length=30, default='')  # 中奖用户昵称

    class Meta:
        db_table = 'tg_lottery_info'


def __str__(self):
    return str(self.lottery_info_id)


class TgRecharge(models.Model):
    recharge_id = models.BigAutoField(primary_key=True)  # 充值记录唯一标识
    tg_id = models.BigIntegerField()  # 用户ID
    money = models.DecimalField(max_digits=10, decimal_places=2)  # 充值金额
    status = models.IntegerField(default=0)  # 充值状态：0待处理，1已完成
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    update_time = models.DateTimeField(null=True, blank=True)  # 更新时间

    class Meta:
        db_table = 'tg_recharge'

    def __str__(self):
        return str(self.recharge_id)


class TgTakeout(models.Model):
    takeout_id = models.BigAutoField(primary_key=True)  # 提现记录唯一标识
    tg_id = models.BigIntegerField()  # 用户ID
    review_tg_id = models.BigIntegerField(null=True, blank=True)  # 审核人员ID
    money = models.DecimalField(max_digits=10, decimal_places=2)  # 提现金额
    status = models.IntegerField(default=0)  # 提现状态：0待处理，1成功，-1拒绝
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    update_time = models.DateTimeField(null=True, blank=True)  # 更新时间

    class Meta:
        db_table = 'tg_takeout'

    def __str__(self):
        return str(self.takeout_id)


class TgLuckydraw(models.Model):
    luckydraw_id = models.BigAutoField(primary_key=True)  # 抽奖记录唯一标识
    tg_id = models.BigIntegerField()  # 用户ID
    tg_full_name = models.CharField(max_length=200, null=True, blank=True)  # 用户全名
    tg_username = models.CharField(max_length=200, null=True, blank=True)  # 用户用户名
    money = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)  # 中奖金额
    status = models.IntegerField(default=0)  # 抽奖状态：0未开奖，1已开奖
    luckydraw_time = models.DateTimeField(null=True, blank=True)  # 开奖时间
    create_time = models.DateTimeField(auto_now_add=True)  # 创建时间
    update_time = models.DateTimeField(null=True, blank=True)  # 更新时间

    class Meta:
        db_table = 'tg_luckydraw'

    def __str__(self):
        return str(self.luckydraw_id)


class TGTransactionLog(models.Model):
    sender = models.ForeignKey('TgUser', related_name='sent_transactions', on_delete=models.CASCADE)
    receiver = models.ForeignKey('TgUser', related_name='received_transactions', on_delete=models.CASCADE, null=True)
    amount = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    transaction_date = models.DateTimeField(auto_now_add=True)
    status = models.BooleanField(default=True)  # True 表示成功，False 表示失败

    class Meta:
        db_table = 'tg_transaction_log'
        verbose_name = 'Transaction Log'
        verbose_name_plural = 'Transaction Logs'
        ordering = ['-transaction_date']

    def __str__(self):
        return f"Transaction from {self.sender.tg_name} to {self.receiver.tg_name} of {self.amount}, Status: {'Success' if self.status else 'Failure'}"

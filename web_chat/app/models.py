import random
import string

from django.db import models
from django.db.models.signals import pre_save
from django.dispatch import receiver
from django.contrib.auth.models import AbstractUser
from django.contrib import admin
from decimal import Decimal


def generate_random_string(length=18):
    return ''.join(random.choices(string.ascii_letters + string.digits, k=length))


class User(models.Model):
    user = models.CharField(max_length=255, verbose_name="用户名")
    money = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0.00'), verbose_name="余额")
    uid = models.CharField(max_length=255, verbose_name="uid", default=generate_random_string)
    admin = models.ForeignKey('Admin', on_delete=models.SET_NULL, null=True, verbose_name="创建人")
    is_black = models.BooleanField(default=False, verbose_name="是否拉黑")
    is_tou = models.BooleanField(default=False, verbose_name="是否托")
    is_auto_tou = models.BooleanField(default=False, verbose_name="是否自动托")

    def __str__(self):
        return str(self.user)


@receiver(pre_save, sender=User)
def user_before_save(sender, instance, **kwargs):
    # 获取当前的请求对象
    request = getattr(instance, '_request', None)

    # 如果是新创建的用户且有请求对象
    if not instance.pk and request and request.user.is_authenticated:
        instance.admin = request.user

    # 处理金额变更的逻辑
    old_instance = sender.objects.filter(id=instance.pk).first()
    if old_instance:
        old_money = old_instance.money
        new_money = instance.money
        if old_money != new_money:
            ChangeMoney.objects.create(
                user=instance,
                last_money=old_money,
                money=new_money - old_money,
                now_money=new_money,
            )


class ChangeMoney(models.Model):
    CHANGE_TYPES = [
        ('管理员修改', '管理员修改'),
        ('管理员上分', '管理员上分'),
        ('管理员下分', '管理员下分'),
        ('下注扣除', '下注扣除'),
        ('中奖增加', '中奖增加')
    ]

    user = models.ForeignKey("User", on_delete=models.CASCADE, verbose_name="用户")
    last_money = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0.00'), verbose_name="原金额")
    money = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0.00'), verbose_name="变更金额")
    now_money = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0.00'), verbose_name="现金额")
    change_type = models.CharField(max_length=255, verbose_name="变更原因", default="管理员修改",
                                   choices=CHANGE_TYPES)
    create_time = models.DateTimeField(auto_now_add=True, verbose_name="创建时间")

    def __str__(self):
        return str(self.user.user)


class Message(models.Model):
    user = models.ForeignKey("User", on_delete=models.CASCADE, null=True, blank=True)
    admin_username = models.CharField(max_length=255, verbose_name="所属代理", null=True, blank=True)
    message = models.TextField(null=True, blank=True)
    file_url = models.URLField(null=True, blank=True)
    file_type = models.CharField(max_length=50, null=True, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True)
    is_bot = models.BooleanField(default=False, verbose_name="是否机器人消息")

    def __str__(self):
        return f"{self.user} - {self.timestamp}"


class Admin(AbstractUser):

    available_time = models.DateTimeField(null=True, blank=True, verbose_name="可用时间")
    is_open = models.BooleanField(default=False)  # 开封盘
    odds = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('100'))  # 聊天控制器的赔率
    total_score = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'))  # 聊天控制器的积分
    close_seconds = models.IntegerField(default=60)  # 聊天控制器的预留封盘
    rebate = models.DecimalField(max_digits=5, decimal_places=2, default=Decimal('0'))  # 聊天控制器的返水
    max_bet_amount = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'))  # 最大下注金额

    single_bet_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                           verbose_name="特指令单场限额")
    normal_single_player_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                                     verbose_name="普通指令单场单玩家限额")
    single_order_max_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                                 verbose_name="单场注单最大限额")
    positive_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                               verbose_name="正单注限额")
    corner_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                             verbose_name="角单注限额")
    single_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                             verbose_name="念单注限额")
    common_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                             verbose_name="通单注限额")
    vehicle_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                              verbose_name="车单注限额")
    special_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                              verbose_name="特单注限额")
    single_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                             verbose_name="单单注限额")
    double_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                             verbose_name="双单注限额")
    large_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                            verbose_name="大单注限额")
    small_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                            verbose_name="小单注限额")
    fan_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                          verbose_name="番单注限额")
    add_order_limit = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0'),
                                          verbose_name="加单注限额")

    # 设置必需字段
    REQUIRED_FIELDS = []  # username 和 password 已经是必需的了

    class Meta:
        verbose_name = '管理员'
        verbose_name_plural = '管理员'

    def __str__(self):
        return self.username


class ChatController(models.Model):
    class Meta:
        verbose_name = '聊天控制器'
        verbose_name_plural = '聊天控制器'
        managed = False
        default_permissions = ()


class LotteryRecord(models.Model):
    """开奖记录"""
    issue = models.CharField(max_length=20, unique=True, verbose_name='期号')
    code = models.CharField(max_length=100, blank=True, verbose_name='开奖号码')
    time = models.CharField(max_length=20, blank=True, verbose_name='开奖时间')
    sum_num = models.IntegerField(default=0, verbose_name='总和')
    sum_single_double = models.IntegerField(default=0, choices=(
        (0, '单'),
        (1, '双'),
    ), verbose_name='总和单双')
    sum_big_small = models.IntegerField(default=0, choices=(
        (0, '大'),
        (1, '小'),
    ), verbose_name='总和大小')
    last_big_small = models.IntegerField(default=0, choices=(
        (0, '尾大'),
        (1, '尾小'),
    ), verbose_name='尾数大小')
    first_dragon_tiger = models.IntegerField(default=0, choices=(
        (0, '龙'),
        (1, '虎'),
    ), verbose_name='第一龙虎')
    second_dragon_tiger = models.IntegerField(default=0, choices=(
        (0, '龙'),
        (1, '虎'),
    ), verbose_name='第二龙虎')
    third_dragon_tiger = models.IntegerField(default=0, choices=(
        (0, '龙'),
        (1, '虎'),
    ), verbose_name='第三龙虎')
    fourth_dragon_tiger = models.IntegerField(default=0, choices=(
        (0, '龙'),
        (1, '虎'),
    ), verbose_name='第四龙虎')

    status = models.IntegerField(default=0, choices=(
        (0, '可下注'),
        (1, '停止下注'),
        (2, '已开奖'),
    ), verbose_name='开奖状态')
    created_at = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='更新时间')

    class Meta:
        verbose_name = '开奖记录'
        verbose_name_plural = verbose_name
        ordering = ['-issue']

    def __str__(self):
        return f"{self.issue} - {self.get_status_display()}"


class BetRecord(models.Model):
    """下注记录"""
    user_id = models.CharField(max_length=100)
    user_name = models.CharField(max_length=255, verbose_name="用户名", default='Unknown')
    admin_username = models.CharField(max_length=100)  # 管理员(所属聊天室)
    issue = models.CharField(max_length=20)  # 期号
    bet_type = models.CharField(max_length=20)  # 下注类型
    amount = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0.00'))  # 下注金额
    status = models.IntegerField(default=0)  # 0-未开奖, 1-已开奖
    win_amount = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal('0.00'))  # 赢金额
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    message = models.TextField(null=True, blank=True)  # 下注指令

    class Meta:
        db_table = 'bet_records'

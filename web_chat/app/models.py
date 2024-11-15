import random
import string

from django.db import models
from django.db.models.signals import pre_save
from django.dispatch import receiver
from django.contrib.auth.models import AbstractUser


def generate_random_string(length=18):
    return ''.join(random.choices(string.ascii_letters + string.digits, k=length))


class User(models.Model):
    user = models.CharField(max_length=255, verbose_name="用户名")
    money = models.FloatField(verbose_name="余额", default=0)
    uid = models.CharField(max_length=255, verbose_name="uid", default=generate_random_string)

    def __str__(self):
        return str(self.user)


@receiver(pre_save, sender=User)
def user_before_save(sender, instance, **kwargs):
    # 在 User 保存之前调用
    # if sender:
    #     ChangeMoney.objects.create(user=instance, last_money=instance.money)
    # if instance.pk:
    old_instance = sender.objects.filter(id=instance.pk).first()  # 获取数据库中当前的数据
    if old_instance:
        old_money = old_instance.money
        new_money = instance.money
        print(f"Old money: {old_money}, New money: {new_money}")
        # 可以在这里执行其他逻辑，例如检查修改并触发某些操作
        if old_money != new_money:
            ChangeMoney.objects.create(user=instance, last_money=old_money, money=new_money - old_money,
                                       now_money=new_money)


class ChangeMoney(models.Model):
    user = models.ForeignKey("User", on_delete=models.CASCADE)
    last_money = models.FloatField(verbose_name="原金额")
    money = models.FloatField(verbose_name="变更金额")
    now_money = models.FloatField(verbose_name="现金额")
    change_type = models.CharField(max_length=255, verbose_name="变更原因", default="管理员修改",
                                   choices=[('管理员修改', '管理员修改'), ("下注扣除", '下注扣除'),
                                            ("中奖增加", '中奖增加')])
    create_time = models.DateTimeField(auto_now_add=True, verbose_name="创建时间")

    def __str__(self):
        return str(self.user.user)


class Message(models.Model):
    user = models.ForeignKey("User", on_delete=models.CASCADE)
    message = models.TextField(null=True, blank=True)
    file_url = models.URLField(null=True, blank=True)
    file_type = models.CharField(max_length=50, null=True, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.user} - {self.timestamp}"


class Admin(AbstractUser):
    available_time = models.DateTimeField(null=True, blank=True, verbose_name="可用时间")

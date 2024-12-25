from django.core.files.base import ContentFile
from django.db import models
from django.db.models.signals import pre_save
from django.dispatch import receiver
from django.core.exceptions import ValidationError
from simplepro.components.fields import ForeignKey

import utils


class TelegramUserName(models.Model):
    """telegram用户名"""
    username = models.CharField(max_length=100, verbose_name='用户名', unique=True)
    name = models.CharField(max_length=100, verbose_name='名称', null=True, blank=True)
    first_name = models.CharField(max_length=50, verbose_name='名', null=True, blank=True)
    last_name = models.CharField(max_length=50, verbose_name='姓', null=True, blank=True)
    about = models.CharField(max_length=100, verbose_name='简介', null=True, blank=True)
    image = models.ImageField(max_length=100, verbose_name='头像', null=True, blank=True, upload_to='images/')
    original_image = models.ImageField(max_length=100, verbose_name='原头像', null=True, blank=True,
                                       upload_to='images/')
    status = models.BooleanField(default=True, verbose_name='监控状态')
    create_time = models.DateTimeField(auto_now_add=True, verbose_name='创建时间')

    def __str__(self):
        return self.username


# Create your models here.
@receiver(pre_save, sender=TelegramUserName)
def username_before_save(sender, instance, **kwargs):
    old_instance = sender.objects.filter(id=instance.pk).first()  # 获取数据库中当前的数据
    instance.username = instance.username.replace("@", "").replace("https://t.me/", "").replace(" ", "")

    if old_instance:  # 修改
        # 如果头像发生变化，更新原头像字段
        if old_instance.image.name != instance.image.name:
            instance.original_image = old_instance.image.name  # 保存原头像路径
    else:  # 新增
        # TODO: 触发检查函数
        status, name, about, image, image_name, first_name, last_name = utils.get_telegram_user_data(instance.username)
        with open("media/images/默认头像.png", 'rb') as f:
            default_image = f.read()
        instance.image = ContentFile(default_image, name="默认头像.png")
        if status:
            instance.name = name
            instance.about = about
            instance.first_name = first_name
            instance.last_name = last_name
            if image:
                instance.image = ContentFile(image, name=image_name)


class CopyTelegramUser(models.Model):
    username = models.CharField(max_length=100, verbose_name='用户名', null=True)
    phone = models.CharField(max_length=100, verbose_name='手机号', null=True)
    user_id = models.CharField(max_length=100, verbose_name='用户ID', null=True)
    session = models.FileField(verbose_name="session文件名", upload_to='session/')
    fileJson = models.FileField(verbose_name="json文件名", upload_to='fileJson/')
    copyObj = ForeignKey(TelegramUserName, verbose_name="复制用户", on_delete=models.SET_NULL, null=True, blank=True)
    create_time = models.DateTimeField(auto_now_add=True, verbose_name='添加时间')

    def __str__(self):
        return self.username + f"({self.phone})"


class AutoUser(models.Model):
    copy_user = ForeignKey(TelegramUserName, verbose_name="用户", on_delete=models.SET_NULL, null=True, blank=True)
    user = ForeignKey(CopyTelegramUser, verbose_name="复制用户", on_delete=models.SET_NULL, null=True, blank=True)
    status = models.BooleanField(default=False, verbose_name='监控状态')
    create_time = models.DateTimeField(null=True, blank=True, verbose_name='添加时间')

    def __str__(self):
        return f"{self.user.username} -> {self.copy_user.username}"

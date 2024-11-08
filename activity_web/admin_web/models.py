from django.db import models
from django.utils import timezone


class Audit(models.Model):
    account = models.CharField(max_length=100, verbose_name="会员账户")
    phone = models.CharField(max_length=100, verbose_name="手机号")
    create_time = models.DateTimeField(default=timezone.now, verbose_name="申请时间")  # 默认为当前时间
    update_time = models.DateTimeField(null=True, blank=True, verbose_name="更新时间")  # 可以为空
    # 其他字段
    type = models.IntegerField(default=0, verbose_name="审核类型", choices=[(0, '办理存款彩金'), (1, '办理提现彩金')])
    status = models.IntegerField(default=0, verbose_name="审核状态",
                                 choices=[(0, '待审核'), (1, '已通过'), (2, '已拒绝')])

    class Meta:
        db_table = 'audit'  # 设置数据库表名
        app_label = 'admin_web'  # 请替换为你的应用名称

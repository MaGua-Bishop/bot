from django.db import models


class TgUser(models.Model):
    tg_id = models.BigIntegerField(primary_key=True)
    tg_full_name = models.CharField(max_length=50)
    tg_username = models.CharField(max_length=50, blank=True, null=True)
    money = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    create_time = models.DateTimeField(auto_now_add=True)
    is_admin = models.BooleanField(default=False)

    class Meta:
        db_table = 'tg_user'


class TgRecharge(models.Model):
    recharge_id = models.BigAutoField(primary_key=True)
    tg_id = models.BigIntegerField()
    money = models.DecimalField(max_digits=10, decimal_places=2)
    status = models.IntegerField(default=0)
    create_time = models.DateTimeField(auto_now_add=True)
    update_time = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'tg_recharge'


class TgJoinGroup(models.Model):
    join_group_id = models.BigAutoField(primary_key=True)
    tg_id = models.BigIntegerField()
    money = models.DecimalField(max_digits=10, decimal_places=2, default=0.00)
    create_time = models.DateTimeField(auto_now_add=True)
    update_time = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'tg_join_group'

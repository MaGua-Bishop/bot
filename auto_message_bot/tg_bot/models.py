from django.db import models


# Create your models here.
class TgButton(models.Model):
    timing_message = models.ForeignKey('tg_bot.TgTimingMessage', on_delete=models.CASCADE)
    name = models.CharField(max_length=50)
    url = models.CharField(max_length=200)

    class Meta:
        db_table = 'tg_button'

    def __str__(self):
        return f"{self.name} ({self.url})"


class TgTimingMessage(models.Model):
    tg_id = models.BigIntegerField()
    message_id = models.IntegerField()
    expiration_date = models.DateField(null=True, blank=True)
    reminder_sent = models.BooleanField(default=False)

    class Meta:
        db_table = 'tg_timing_message'

    def __str__(self):
        return f"{self.message_id} ({self.expiration_date})"


class TGInvite(models.Model):
    inviter_id = models.BigIntegerField()
    chat_id = models.BigIntegerField()
    chat_title = models.CharField(max_length=255)
    chat_type = models.CharField(max_length=50)  # 群聊或频道的类型（例如：group, channel）
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'tg_invite'

    def __str__(self):
        return f"{self.chat_title} - {self.chat_type} (Invited by {self.inviter_id})"


class TGInviteTimingMessage(models.Model):
    invite_id = models.IntegerField()  # 群聊 ID
    timing_message_id = models.IntegerField()  # 定时消息 ID
    time = models.TimeField()  # 定时消息的发送时间
    message_id = models.IntegerField(null=True, blank=True)  # 上次发送的消息ID
    is_pinned = models.BooleanField(default=False)  # 是否置顶
    delete_last_message = models.BooleanField(default=False)  # 是否删除上次发送的消息
    created_at = models.DateTimeField(auto_now_add=True)  # 创建时间

    class Meta:
        db_table = 'tg_invite_timing_message'

    def __str__(self):
        return f"Group ID: {self.invite_id} - Timing Message ID: {self.timing_message_id} - Time: {self.time}"

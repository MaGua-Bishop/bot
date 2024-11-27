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
    time = models.TimeField()

    class Meta:
        db_table = 'tg_timing_message'

    def __str__(self):
        return f"{self.message_id} ({self.time})"


class TGInvite(models.Model):
    inviter_id = models.BigIntegerField() 
    chat_id = models.BigIntegerField() 
    chat_title = models.CharField(max_length=255) 
    chat_type = models.CharField(max_length=50)# 群聊或频道的类型（例如：group, channel）
    created_at = models.DateTimeField(auto_now_add=True) 

    class Meta:
        db_table = 'tg_invite'

    def __str__(self):
        return f"{self.chat_title} - {self.chat_type} (Invited by {self.inviter_id})"

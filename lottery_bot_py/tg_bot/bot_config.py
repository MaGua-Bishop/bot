import telebot
import os
from django.conf import settings
TOKEN = settings.BOT_TOKEN
bot = telebot.TeleBot(TOKEN)

WEBHOOK_URL = os.environ.get('SERVER_HOST') + '/tgbot/lottery_bot_cn'
# WEBHOOK_URL = 'https://f82b-119-39-51-21.ngrok-free.app/tgbot/lottery_bot'
bot.remove_webhook()
bot.set_webhook(url=WEBHOOK_URL)

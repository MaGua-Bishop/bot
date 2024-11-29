import telebot
from django.conf import settings
import os

TOKEN = settings.TG_BOT_TOKEN
bot = telebot.TeleBot(TOKEN)

# WEBHOOK_URL = os.environ.get('SERVER_HOST') + '/tgbot/auto_message_bot'
WEBHOOK_URL = 'https://bd69-119-39-51-90.ngrok-free.app/tgbot/auto_message_bot'
bot.remove_webhook()
bot.set_webhook(url=WEBHOOK_URL)

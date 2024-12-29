import telebot
from django.conf import settings
import os

TOKEN = settings.TG_BOT_TOKEN
bot = telebot.TeleBot(TOKEN)

WEBHOOK_URL = os.environ.get('SERVER_HOST') + '/tgbot/electronic_bot'
# WEBHOOK_URL = 'https://a088-119-39-51-21.ngrok-free.app/tgbot/electronic_bot'
bot.remove_webhook()
bot.set_webhook(url=WEBHOOK_URL)

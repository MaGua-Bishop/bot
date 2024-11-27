import telebot
from django.conf import settings

TOKEN = settings.TG_BOT_TOKEN
bot = telebot.TeleBot(TOKEN)

# WEBHOOK_URL = os.environ.get('SERVER_HOST') + '/tgbot/joingroup_bot/'
WEBHOOK_URL = 'https://612a-119-39-51-90.ngrok-free.app/tgbot/auto_message_bot'
bot.remove_webhook()
bot.set_webhook(url=WEBHOOK_URL)

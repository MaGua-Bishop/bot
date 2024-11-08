import telebot
import os

TOKEN = '7736269405:AAGimT1d2LhJOjRnsc_riAFyBwHPRrcJaKg'
# TOKEN = '8030696965:AAFY6eZti5yIg7J0p-mXIHSH9fLsmrvHBFE'
bot = telebot.TeleBot(TOKEN)

WEBHOOK_URL = os.environ.get('SERVER_HOST') + '/tgbot/joingroup_bot/'
# WEBHOOK_URL = 'https://3a95-111-55-97-11.ngrok-free.app/tgbot/joingroup_bot/'
bot.remove_webhook()
bot.set_webhook(url=WEBHOOK_URL)

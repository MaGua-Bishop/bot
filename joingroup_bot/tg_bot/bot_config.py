import telebot
import os

TOKEN = '8030696965:AAFY6eZti5yIg7J0p-mXIHSH9fLsmrvHBFE'
bot = telebot.TeleBot(TOKEN)

WEBHOOK_URL = os.environ.get('SERVER_HOST')
bot.remove_webhook()
bot.set_webhook(url=WEBHOOK_URL)

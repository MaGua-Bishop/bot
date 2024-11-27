import telebot
from django.http import JsonResponse
from django.views import View
from .bot_config import bot
import tg_bot.bot_message_handler
import tg_bot.bot_callback_handler

class BotView(View):
    def post(self, request, *args, **kwargs):
        json_str = request.body.decode('UTF-8')
        update = telebot.types.Update.de_json(json_str)
        bot.process_new_updates([update])
        return JsonResponse({'status': 'ok'})

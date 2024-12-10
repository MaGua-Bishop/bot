from telebot import types

from .bot_config import bot
from .utlis import get_start_reply_markup
from .models import TgUser

commands = [
    types.BotCommand("start", "Start Bot"),
    types.BotCommand("help", "Help"),
    types.BotCommand("support", "Support")
]
bot.set_my_commands(commands, scope=types.BotCommandScopeAllPrivateChats())


@bot.message_handler(commands=['start'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    user_id = message.from_user.id
    full_name = message.from_user.full_name
    user, created = TgUser.objects.get_or_create(tg_id=user_id)
    if created:
        user.money = 0
        user.pg_money = 0
        try:
            if message.text.startswith('/start') and len(message.text.split()) > 1:
                invite_tg_id = int(message.text.split()[1])
                user.invite_tg_id = invite_tg_id
                print(invite_tg_id)
        except (ValueError, AttributeError):
            user.invite_tg_id = None
        user.save()
    text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {user.pg_money}"
    bot.send_message(message.chat.id, text, parse_mode="HTML", reply_markup=get_start_reply_markup())


@bot.message_handler(commands=['help'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    text = f"👩‍💻 Support<a href='https://t.me/{2142298091}'>@linanming</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.send_message(chat_id=message.chat.id, text=text,
                     reply_markup=markup, parse_mode="HTML")

@bot.message_handler(commands=['support'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    text = f"👩‍💻 Support<a href='https://t.me/{2142298091}'>@linanming</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.send_message(chat_id=message.chat.id, text=text,
                     reply_markup=markup, parse_mode="HTML")

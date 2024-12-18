#!/usr/bin/env python
"""Django's command-line utility for administrative tasks."""
import logging
import os
import sys
# import telebot
# import config
# from config import bot


def main():
    """Run administrative tasks."""
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'bot_data.settings')
    try:
        from django.core.management import execute_from_command_line
    except ImportError as exc:
        raise ImportError(
            "Couldn't import Django. Are you sure it's installed and "
            "available on your PYTHONPATH environment variable? Did you "
            "forget to activate a virtual environment?"
        ) from exc
    execute_from_command_line(sys.argv)


if __name__ == '__main__':
    # if sys.argv[1] == "runserver":
    #     telebot.logger.setLevel(logging.DEBUG)
    #     bot.set_webhook(f"{config.WEBHOOK_HOST}/{config.WEBHOOK_PATH}/{bot.token}")
    main()
